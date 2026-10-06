package com.carlos.job.reconcile;

import cn.hutool.core.util.StrUtil;
import com.carlos.job.config.JobProperties;
import com.carlos.job.dispatch.AbstractDispatchJob;
import com.carlos.job.manager.JobInfoManager;
import com.carlos.job.manager.JobScheduleManager;
import com.carlos.job.pojo.dto.JobInfoDTO;
import com.carlos.job.pojo.enums.JobConcurrentModeEnum;
import com.carlos.job.pojo.enums.JobMisfireStrategyEnum;
import com.carlos.job.pojo.enums.JobScheduleTypeEnum;
import com.carlos.job.pojo.param.JobCreateParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 启动对账器：以 job_info（控制面）为准校验 Quartz（执行面），修复漂移
 * </p>
 * <p>
 * 三类漂移处理：
 * DB 有 Quartz 无 → 重建调度；Quartz 有 DB 无 → 回填元数据（兼容历史任务）；
 * 两边都有但调度配置不一致 → 以 DB 为准重新调度。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobReconcileRunner implements ApplicationRunner {

    private final Scheduler scheduler;

    private final JobInfoManager jobInfoManager;

    private final JobScheduleManager scheduleManager;

    private final JobProperties jobProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!Boolean.TRUE.equals(jobProperties.getReconcileOnStartup())) {
            log.debug("Job reconcile on startup disabled");
            return;
        }
        try {
            reconcile();
        } catch (Exception e) {
            // 对账失败不阻断启动，漂移由下次对账或人工处理
            log.error("Job reconcile on startup fail", e);
        }
    }

    private void reconcile() throws SchedulerException {
        List<JobInfoDTO> infoList = jobInfoManager.listAll();
        Set<String> dbKeys = new HashSet<>();
        int rebuilt = 0;
        int rescheduled = 0;
        for (JobInfoDTO info : infoList) {
            String key = jobKeyString(info.getJobGroup(), info.getJobName());
            dbKeys.add(key);
            try {
                JobKey jobKey = JobKey.jobKey(info.getJobName(), info.getJobGroup());
                if (!scheduler.checkExists(jobKey)) {
                    rebuildSchedule(info);
                    rebuilt++;
                    continue;
                }
                if (scheduleDrifted(jobKey, info)) {
                    reschedule(info);
                    rescheduled++;
                }
            } catch (Exception e) {
                // 单行漂移处理失败不影响其余行
                log.error("Job reconcile row fail: {}", key, e);
            }
        }
        int backfilled = backfillLegacy(dbKeys);
        log.info("Job reconcile done: db={}, rebuilt={}, rescheduled={}, backfilled={}",
            infoList.size(), rebuilt, rescheduled, backfilled);
    }

    /**
     * 调度配置是否漂移。仅比较 CRON 表达式与 NORMAL/PAUSED 状态；
     * 非 CRON 类型与 BLOCKED/ERROR 等运行态不参与漂移判定（避免误判重复重建）。
     */
    private boolean scheduleDrifted(JobKey jobKey, JobInfoDTO info) throws SchedulerException {
        // 非 CRON 或配置缺失时不做漂移判定
        if (!JobScheduleTypeEnum.CRON.getCode().equals(info.getScheduleType())
            || StrUtil.isBlank(info.getScheduleConf())) {
            return false;
        }
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
        if (triggers.isEmpty()) {
            return true;
        }
        Trigger trigger = triggers.get(0);
        // 触发器类型与 DB 调度类型不一致视为漂移（如 DB 是 CRON 但 Quartz 是 SimpleTrigger）
        if (!(trigger instanceof CronTrigger cronTrigger)) {
            return true;
        }
        if (!cronTrigger.getCronExpression().equals(info.getScheduleConf())) {
            return true;
        }
        // 状态漂移仅比较 NORMAL/PAUSED，BLOCKED/ERROR 等运行态归约跳过
        String dbStatus = info.getStatus();
        if (!isManagedStatus(dbStatus)) {
            return false;
        }
        Trigger.TriggerState state = scheduler.getTriggerState(trigger.getKey());
        return !state.name().equals(dbStatus);
    }

    /**
     * 是否参与状态对账的管理态（仅正常/暂停，运行态不算漂移）
     */
    private boolean isManagedStatus(String status) {
        return Trigger.TriggerState.NORMAL.name().equals(status)
            || Trigger.TriggerState.PAUSED.name().equals(status);
    }

    /**
     * 重建调度：DB 有记录但 Quartz 缺失（如 QRTZ 表被清理）
     */
    private void rebuildSchedule(JobInfoDTO info) {
        log.warn("Job reconcile rebuild: {}", jobKeyString(info.getJobGroup(), info.getJobName()));
        scheduleManager.createJob(toCreateParam(info));
        pauseIfNeeded(info);
    }

    /**
     * 重新调度：两边都有但配置漂移，以 DB 为准
     */
    private void reschedule(JobInfoDTO info) throws SchedulerException {
        log.warn("Job reconcile reschedule: {}", jobKeyString(info.getJobGroup(), info.getJobName()));
        JobKey jobKey = JobKey.jobKey(info.getJobName(), info.getJobGroup());
        scheduler.deleteJob(jobKey);
        scheduleManager.createJob(toCreateParam(info));
        pauseIfNeeded(info);
    }

    private void pauseIfNeeded(JobInfoDTO info) {
        if (Trigger.TriggerState.PAUSED.name().equals(info.getStatus())) {
            scheduleManager.pauseJob(info.getJobName(), info.getJobGroup());
        }
    }

    /**
     * 回填历史任务：Quartz 有但 job_info 无（J0 之前创建的任务）
     */
    private int backfillLegacy(Set<String> dbKeys) throws SchedulerException {
        int count = 0;
        Set<JobKey> quartzKeys = scheduler.getJobKeys(GroupMatcher.anyGroup());
        for (JobKey jobKey : quartzKeys) {
            String key = jobKeyString(jobKey.getGroup(), jobKey.getName());
            if (dbKeys.contains(key)) {
                continue;
            }
            JobDetail detail = scheduler.getJobDetail(jobKey);
            if (detail == null) {
                continue;
            }
            if (jobInfoManager.add(buildBackfillDTO(detail))) {
                count++;
                log.info("Job reconcile backfill legacy job: {}", key);
            } else {
                log.warn("Job reconcile backfill fail: {}", key);
            }
        }
        return count;
    }

    private JobCreateParam toCreateParam(JobInfoDTO info) {
        JobCreateParam param = new JobCreateParam()
            .setJobName(info.getJobName())
            .setJobGroup(info.getJobGroup())
            .setServiceName(info.getServiceName())
            .setHandlerName(info.getHandlerName())
            .setScheduleType(info.getScheduleType())
            .setScheduleConf(info.getScheduleConf())
            .setParams(info.getParams())
            .setDescription(info.getDescription())
            .setMisfireStrategy(info.getMisfireStrategy())
            .setConcurrentMode(info.getConcurrentMode())
            .setTimeoutSeconds(info.getTimeoutSeconds())
            .setFailRetryCount(info.getFailRetryCount())
            .setAuthor(info.getAuthor())
            .setAlarmReceivers(info.getAlarmReceivers());
        // 兼容旧调用方只读 cronExpression 的字段
        if (JobScheduleTypeEnum.CRON.getCode().equals(info.getScheduleType())) {
            param.setCronExpression(info.getScheduleConf());
        }
        return param;
    }

    private JobInfoDTO buildBackfillDTO(JobDetail detail) throws SchedulerException {
        String cron = null;
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(detail.getKey());
        if (!triggers.isEmpty() && triggers.get(0) instanceof CronTrigger cronTrigger) {
            cron = cronTrigger.getCronExpression();
        }
        return new JobInfoDTO()
            .setJobName(detail.getKey().getName())
            .setJobGroup(detail.getKey().getGroup())
            .setServiceName(detail.getJobDataMap().getString(AbstractDispatchJob.DATA_SERVICE_NAME))
            .setHandlerName(detail.getJobDataMap().getString(AbstractDispatchJob.DATA_HANDLER_NAME))
            .setScheduleType(JobScheduleTypeEnum.CRON.getCode())
            .setScheduleConf(cron)
            .setParams(detail.getJobDataMap().getString(AbstractDispatchJob.DATA_PARAMS))
            .setDescription(detail.getDescription())
            .setMisfireStrategy(JobMisfireStrategyEnum.SMART_POLICY.getCode())
            .setConcurrentMode(JobConcurrentModeEnum.SERIAL.getCode())
            .setTimeoutSeconds(0)
            .setFailRetryCount(0)
            .setStatus(resolveTriggerState(detail.getKey()));
    }

    /**
     * 回填时状态归约为管理态（NORMAL/PAUSED)，运行态统一记为 NORMAL
     */
    private String resolveTriggerState(JobKey jobKey) throws SchedulerException {
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
        if (triggers.isEmpty()) {
            return Trigger.TriggerState.NORMAL.name();
        }
        String state = scheduler.getTriggerState(triggers.get(0).getKey()).name();
        return isManagedStatus(state) ? state : Trigger.TriggerState.NORMAL.name();
    }

    private String jobKeyString(String group, String name) {
        return StrUtil.blankToDefault(group, JobScheduleManager.DEFAULT_GROUP) + "." + name;
    }

}
