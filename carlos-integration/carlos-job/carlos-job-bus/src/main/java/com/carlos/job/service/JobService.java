package com.carlos.job.service;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.pagination.Paging;
import com.carlos.job.convert.JobRegisterConvert;
import com.carlos.job.enums.JobErrorCode;
import com.carlos.job.manager.JobInfoManager;
import com.carlos.job.manager.JobScheduleManager;
import com.carlos.job.pojo.dto.JobInfoDTO;
import com.carlos.job.pojo.dto.JobRegisterDTO;
import com.carlos.job.pojo.enums.JobConcurrentModeEnum;
import com.carlos.job.pojo.enums.JobMisfireStrategyEnum;
import com.carlos.job.pojo.enums.JobScheduleTypeEnum;
import com.carlos.job.pojo.param.ApiJobRegisterParam;
import com.carlos.job.pojo.param.JobCreateParam;
import com.carlos.job.pojo.param.JobPageParam;
import com.carlos.job.pojo.param.JobUpdateParam;
import com.carlos.job.pojo.vo.JobVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Trigger;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 定时任务 业务
 * </p>
 * <p>
 * 双写编排：job_info（控制面，事实源）+ Quartz（执行面）。
 * 顺序为先 DB 后 Quartz，Quartz 失败时回滚 DB，保证不出现半建任务。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobScheduleManager scheduleManager;

    private final JobInfoManager jobInfoManager;

    /**
     * 新增任务
     *
     * @param param 任务参数
     */
    public void addJob(JobCreateParam param) {
        if (StrUtil.isBlank(param.getJobName())) {
            throw JobErrorCode.JOB_PARAM_NAME_EMPTY.exception();
        }
        if (StrUtil.isBlank(param.getServiceName())) {
            throw JobErrorCode.JOB_PARAM_SERVICE_EMPTY.exception();
        }
        if (StrUtil.isBlank(param.getHandlerName())) {
            throw JobErrorCode.JOB_PARAM_HANDLER_EMPTY.exception();
        }
        // 预检：重复创建直接语义化 409，避免并发下撞 DB 唯一键裸抛 500
        if (jobInfoManager.getByJobKey(param.getJobName(), param.getJobGroup()) != null
            || scheduleManager.exists(param.getJobName(), param.getJobGroup())) {
            throw JobErrorCode.JOB_ALREADY_EXISTS.exception("任务已存在: " + param.getJobName());
        }
        // 1. 先写控制面
        JobInfoDTO infoDTO = buildInfoDTO(param);
        if (!jobInfoManager.add(infoDTO)) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("任务元数据落库失败: " + param.getJobName());
        }
        // 2. 再操作执行面，失败回滚控制面
        try {
            scheduleManager.createJob(param);
        } catch (RuntimeException e) {
            jobInfoManager.removeByJobKey(param.getJobName(), param.getJobGroup());
            throw e;
        }
        log.info("Create job: {}.{}", param.getJobGroup(), param.getJobName());
    }

    /**
     * 幂等注册任务（注解式接入）：以 (jobName, jobGroup) 为业务键，<b>已存在则跳过、不覆盖</b>
     *
     * <p>{@code job_info} 是唯一真相源，运维在后台调整过的 cron/参数不会被下次发布改回去；
     * 注解声明与库中值不一致时只打 WARN。多实例并发注册由唯一索引仲裁，
     * 撞键方收敛为"已存在"而不是 500。</p>
     *
     * @param param 自动注册参数
     * @return 注册结果（未创建不是错误）
     */
    public JobRegisterDTO registerIfAbsent(ApiJobRegisterParam param) {
        if (StrUtil.isBlank(param.getJobName())) {
            throw JobErrorCode.JOB_PARAM_NAME_EMPTY.exception();
        }
        if (StrUtil.isBlank(param.getServiceName())) {
            throw JobErrorCode.JOB_PARAM_SERVICE_EMPTY.exception();
        }
        if (StrUtil.isBlank(param.getHandlerName())) {
            throw JobErrorCode.JOB_PARAM_HANDLER_EMPTY.exception();
        }
        // 分组先解析：后续所有 manager 调用都用解析后的值，否则空分组匹配不到以 DEFAULT 落库的行
        String jobGroup = StrUtil.blankToDefault(param.getJobGroup(), JobScheduleManager.DEFAULT_GROUP);
        JobCreateParam createParam = JobRegisterConvert.INSTANCE.toCreateParam(param).setJobGroup(jobGroup);
        JobInfoDTO existing = jobInfoManager.getByJobKey(param.getJobName(), jobGroup);
        if (existing != null) {
            // 漂移必须回给调用方：客户端据此在启动汇总里提示"声明与库中不一致"，只打服务端日志等于没人看见
            boolean drifted = warnIfDrifted(existing, createParam);
            return new JobRegisterDTO().setCreated(false).setExisted(true).setDrifted(drifted)
                .setStatus(existing.getStatus())
                .setMessage(drifted ? "任务已存在，声明与库中配置不一致（保持库中配置）" : "任务已存在，保持库中配置");
        }
        // 漂移态：Quartz 有、控制面无。重建会覆盖执行面配置，故跳过并交给对账或人工处理
        if (scheduleManager.exists(param.getJobName(), jobGroup)) {
            log.warn("自动注册发现漂移态任务（调度器存在但 job_info 缺失），已跳过: {}.{}，请在后台删除该任务后重试",
                jobGroup, param.getJobName());
            return new JobRegisterDTO().setCreated(false).setExisted(false).setDrifted(true)
                .setMessage("调度器存在同名任务但控制面元数据缺失（漂移态），已跳过");
        }
        String status = StrUtil.blankToDefault(param.getStatus(), Trigger.TriggerState.NORMAL.name());
        try {
            if (!jobInfoManager.add(buildInfoDTO(createParam, status))) {
                throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("任务元数据落库失败: " + param.getJobName());
            }
        } catch (DuplicateKeyException e) {
            // 唯一索引 uk_job_info_name_group 即仲裁者，无需分布式锁
            log.info("自动注册撞唯一键，视为已存在: {}.{}", jobGroup, param.getJobName());
            JobInfoDTO winner = jobInfoManager.getByJobKey(param.getJobName(), jobGroup);
            boolean drifted = winner != null && warnIfDrifted(winner, createParam);
            return new JobRegisterDTO().setCreated(false).setExisted(true).setDrifted(drifted)
                // 状态以胜出方库中的真实值为准，别把本进程解析出的期望值报给调用方
                .setStatus(winner == null ? status : winner.getStatus())
                .setMessage("任务已由其他实例并发注册");
        }
        try {
            scheduleManager.createJob(createParam);
            if (!Trigger.TriggerState.NORMAL.name().equals(status)) {
                scheduleManager.pauseJob(param.getJobName(), jobGroup);
            }
        } catch (BusinessException e) {
            if (!JobErrorCode.JOB_ALREADY_EXISTS.getCode().equals(e.getErrorCode())) {
                rollbackRegister(param.getJobName(), jobGroup);
                throw e;
            }
            // 执行面已被别的参与者按刚写入的 job_info 建好（典型是调度中心启动时的 JobReconcileRunner），
            // 两面都有即一致状态。此处若照常回滚，会删掉不属于本次的触发器、并让客户端收到 409 后不再重试，
            // 任务就凭空消失了。
            log.warn("自动注册时执行面已存在（可能由启动对账并发建立），收敛为已创建: {}.{}", jobGroup, param.getJobName());
        } catch (RuntimeException e) {
            rollbackRegister(param.getJobName(), jobGroup);
            throw e;
        }
        log.info("自动注册任务: {}.{}, handler={}, status={}", jobGroup, param.getJobName(),
            param.getHandlerName(), status);
        return new JobRegisterDTO().setCreated(true).setExisted(false).setDrifted(false)
            .setStatus(status)
            .setMessage("任务已创建");
    }

    /**
     * 注册失败补偿：先清执行面，只有执行面清干净了才清控制面
     *
     * <p>执行面清理失败时刻意<b>保留</b>控制面行：库里留着顶多是"两面都有"（一致态，下次启动对账即可收敛），
     * 而删掉行才会造出正是本方法想避免的"Quartz 有 / DB 无"漂移态。</p>
     */
    private void rollbackRegister(String jobName, String jobGroup) {
        try {
            if (scheduleManager.exists(jobName, jobGroup)) {
                scheduleManager.deleteJob(jobName, jobGroup);
            }
        } catch (RuntimeException e) {
            log.error("自动注册回滚执行面失败，保留控制面元数据待对账修复: {}.{}", jobGroup, jobName, e);
            return;
        }
        try {
            jobInfoManager.removeByJobKey(jobName, jobGroup);
        } catch (RuntimeException e) {
            // 不能让它盖掉调用方真正关心的失败原因
            log.error("自动注册回滚控制面失败，需人工清理: {}.{}", jobGroup, jobName, e);
        }
    }

    /**
     * 注解声明与库中值不一致时提示漂移（不覆盖，job_info 为唯一真相源）
     */
    private boolean warnIfDrifted(JobInfoDTO existing, JobCreateParam param) {
        List<String> drifts = new ArrayList<>();
        collectDrift(drifts, "scheduleConf", existing.getScheduleConf(), param.resolveScheduleConf());
        collectDrift(drifts, "handlerName", existing.getHandlerName(), param.getHandlerName());
        collectDrift(drifts, "serviceName", existing.getServiceName(), param.getServiceName());
        collectDrift(drifts, "params", existing.getParams(), param.getParams());
        if (drifts.isEmpty()) {
            return false;
        }
        log.warn("自动注册漂移（保持库中配置，job_info 为唯一真相源）: {}.{} -> {}",
            existing.getJobGroup(), existing.getJobName(), String.join(", ", drifts));
        return true;
    }

    /**
     * 只收集显式声明过的字段：未声明（空）的字段不参与比对，避免误报
     */
    private void collectDrift(List<String> drifts, String field, String existingValue, String declaredValue) {
        if (StrUtil.isNotBlank(declaredValue) && !StrUtil.equals(existingValue, declaredValue)) {
            drifts.add(field + " 库中=[" + existingValue + "] 声明=[" + declaredValue + "]");
        }
    }

    /**
     * 修改任务（调度配置/参数/描述/策略）
     *
     * @param param 任务参数
     */
    public void updateJob(JobUpdateParam param) {
        JobInfoDTO oldInfo = jobInfoManager.getByJobKey(param.getJobName(), param.getJobGroup());
        if (oldInfo == null) {
            throw JobErrorCode.JOB_NOT_FOUND.exception("任务不存在: " + param.getJobName());
        }
        // 1. 先更新控制面
        JobInfoDTO newInfo = buildUpdateDTO(param);
        if (!jobInfoManager.updateByJobKey(newInfo)) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("任务元数据更新失败: " + param.getJobName());
        }
        // 2. 再操作执行面，失败回滚控制面为旧值
        try {
            scheduleManager.updateJob(param);
        } catch (RuntimeException e) {
            jobInfoManager.updateByJobKey(oldInfo);
            throw e;
        }
        log.info("Update job: {}.{}", param.getJobGroup(), param.getJobName());
    }

    /**
     * 暂停任务
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     */
    public void pauseJob(String jobName, String jobGroup) {
        requireExists(jobName, jobGroup);
        scheduleManager.pauseJob(jobName, jobGroup);
        jobInfoManager.updateStatus(jobName, jobGroup, Trigger.TriggerState.PAUSED.name());
    }

    /**
     * 恢复任务
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     */
    public void resumeJob(String jobName, String jobGroup) {
        requireExists(jobName, jobGroup);
        scheduleManager.resumeJob(jobName, jobGroup);
        jobInfoManager.updateStatus(jobName, jobGroup, Trigger.TriggerState.NORMAL.name());
    }

    /**
     * 删除任务（Quartz 与 DB 任一存在即可删，保证漂移态下也能清理 DB 残留）
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     */
    public void deleteJob(String jobName, String jobGroup) {
        boolean inQuartz = scheduleManager.exists(jobName, jobGroup);
        boolean inDb = jobInfoManager.getByJobKey(jobName, jobGroup) != null;
        if (!inQuartz && !inDb) {
            throw JobErrorCode.JOB_NOT_FOUND.exception("任务不存在: " + jobName);
        }
        if (inQuartz) {
            scheduleManager.deleteJob(jobName, jobGroup);
        }
        if (inDb) {
            jobInfoManager.removeByJobKey(jobName, jobGroup);
        }
    }

    /**
     * 手动触发一次
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     */
    public void runOnce(String jobName, String jobGroup) {
        scheduleManager.triggerJob(jobName, jobGroup);
    }

    /**
     * 任务详情（Quartz 视图 + job_info 策略元数据合并）
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     * @return 任务视图对象
     */
    public JobVO getJobInfo(String jobName, String jobGroup) {
        JobVO vo = scheduleManager.getJobInfo(jobName, jobGroup);
        if (vo == null) {
            throw JobErrorCode.JOB_NOT_FOUND.exception("任务不存在: " + jobName);
        }
        return mergeMetadata(vo);
    }

    /**
     * 分页查询任务（Quartz 视图 + job_info 策略元数据合并）
     *
     * @param param 分页参数
     * @return 分页结果
     */
    public Paging<JobVO> getPage(JobPageParam param) {
        Paging<JobVO> page = scheduleManager.getPage(param);
        if (page.getRecords() != null) {
            page.getRecords().forEach(this::mergeMetadata);
        }
        return page;
    }

    /**
     * 预览未来触发时间
     *
     * @param scheduleType 调度类型
     * @param scheduleConf 调度配置
     * @param count        预览条数
     * @return 未来触发时间列表
     */
    public List<LocalDateTime> previewFireTimes(Integer scheduleType, String scheduleConf, int count) {
        return scheduleManager.previewFireTimes(scheduleType, scheduleConf, count);
    }

    private void requireExists(String jobName, String jobGroup) {
        if (!scheduleManager.exists(jobName, jobGroup)) {
            throw JobErrorCode.JOB_NOT_FOUND.exception("任务不存在: " + jobName);
        }
    }

    /**
     * 合并控制面策略元数据到 VO（Quartz 侧无这些字段）；DB 缺失时保持 Quartz 视图原样
     */
    private JobVO mergeMetadata(JobVO vo) {
        JobInfoDTO info = jobInfoManager.getByJobKey(vo.getJobName(), vo.getJobGroup());
        if (info == null) {
            return vo;
        }
        vo.setScheduleType(info.getScheduleType());
        vo.setScheduleConf(info.getScheduleConf());
        vo.setMisfireStrategy(info.getMisfireStrategy());
        vo.setConcurrentMode(info.getConcurrentMode());
        vo.setTimeoutSeconds(info.getTimeoutSeconds());
        vo.setFailRetryCount(info.getFailRetryCount());
        vo.setAuthor(info.getAuthor());
        vo.setAlarmReceivers(info.getAlarmReceivers());
        return vo;
    }

    /**
     * 由新增参数构造控制面元数据（策略字段缺省取默认值，保持存量行为）
     */
    private JobInfoDTO buildInfoDTO(JobCreateParam param) {
        return buildInfoDTO(param, Trigger.TriggerState.NORMAL.name());
    }

    /**
     * 由新增参数构造控制面元数据（指定初始状态，供自动注册的 PAUSED 场景使用）
     */
    private JobInfoDTO buildInfoDTO(JobCreateParam param, String status) {
        JobScheduleTypeEnum scheduleType = JobScheduleTypeEnum.of(param.getScheduleType());
        return new JobInfoDTO()
            .setJobName(param.getJobName())
            .setJobGroup(StrUtil.blankToDefault(param.getJobGroup(), JobScheduleManager.DEFAULT_GROUP))
            .setServiceName(param.getServiceName())
            .setHandlerName(param.getHandlerName())
            .setScheduleType(scheduleType == null ? JobScheduleTypeEnum.CRON.getCode() : scheduleType.getCode())
            .setScheduleConf(param.resolveScheduleConf())
            .setParams(param.getParams())
            .setDescription(param.getDescription())
            .setMisfireStrategy(JobMisfireStrategyEnum.ofDefault(param.getMisfireStrategy()).getCode())
            .setConcurrentMode(JobConcurrentModeEnum.ofDefault(param.getConcurrentMode()).getCode())
            .setTimeoutSeconds(param.getTimeoutSeconds() == null ? 0 : param.getTimeoutSeconds())
            .setFailRetryCount(param.getFailRetryCount() == null ? 0 : param.getFailRetryCount())
            .setAuthor(param.getAuthor())
            .setAlarmReceivers(param.getAlarmReceivers())
            .setStatus(status);
    }

    /**
     * 由修改参数构造控制面元数据（空策略字段不覆盖，走 MP 非空更新）
     */
    private JobInfoDTO buildUpdateDTO(JobUpdateParam param) {
        JobInfoDTO dto = new JobInfoDTO()
            .setJobName(param.getJobName())
            .setJobGroup(StrUtil.blankToDefault(param.getJobGroup(), JobScheduleManager.DEFAULT_GROUP))
            .setParams(param.getParams())
            .setDescription(param.getDescription())
            .setAuthor(param.getAuthor())
            .setAlarmReceivers(param.getAlarmReceivers())
            .setTimeoutSeconds(param.getTimeoutSeconds())
            .setFailRetryCount(param.getFailRetryCount())
            .setMisfireStrategy(param.getMisfireStrategy())
            .setConcurrentMode(param.getConcurrentMode());
        if (param.getScheduleType() != null) {
            dto.setScheduleType(param.getScheduleType());
        }
        if (StrUtil.isNotBlank(param.resolveScheduleConf())) {
            dto.setScheduleConf(param.resolveScheduleConf());
        }
        return dto;
    }

}
