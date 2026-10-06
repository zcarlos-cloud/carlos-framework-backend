package com.carlos.job.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.job.dispatch.AbstractDispatchJob;
import com.carlos.job.dispatch.ParallelDispatchJob;
import com.carlos.job.dispatch.SerialDispatchJob;
import com.carlos.job.enums.JobErrorCode;
import com.carlos.job.manager.JobScheduleManager;
import com.carlos.job.pojo.enums.JobConcurrentModeEnum;
import com.carlos.job.pojo.enums.JobMisfireStrategyEnum;
import com.carlos.job.pojo.enums.JobScheduleTypeEnum;
import com.carlos.job.pojo.enums.JobTriggerTypeEnum;
import com.carlos.job.pojo.param.JobCreateParam;
import com.carlos.job.pojo.param.JobPageParam;
import com.carlos.job.pojo.param.JobUpdateParam;
import com.carlos.job.pojo.vo.JobVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.OperableTrigger;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 任务调度封装实现类
 * </p>
 * <p>
 * 支持 Quartz 原生多触发类型（CRON/固定间隔/一次性）、misfire 过期策略、串行/并行并发模式。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class JobScheduleManagerImpl implements JobScheduleManager {

    /**
     * 最小调度间隔（毫秒），防止高频任务拖垮调度线程池与下游服务
     */
    private static final long MIN_INTERVAL_MS = 10_000L;

    /**
     * 单页最大条数上限，防大查询
     */
    private static final int MAX_PAGE_SIZE = 500;

    /**
     * 触发时间预览条数
     */
    private static final int PREVIEW_COUNT = 5;

    private final Scheduler scheduler;

    @Override
    public void createJob(JobCreateParam param) {
        JobScheduleTypeEnum scheduleType = resolveScheduleType(param.getScheduleType());
        String scheduleConf = resolveScheduleConf(scheduleType, param.resolveScheduleConf());
        validateSchedule(scheduleType, scheduleConf);
        JobConcurrentModeEnum concurrentMode = JobConcurrentModeEnum.ofDefault(param.getConcurrentMode());
        JobMisfireStrategyEnum misfire = JobMisfireStrategyEnum.ofDefault(param.getMisfireStrategy());

        String group = StrUtil.blankToDefault(param.getJobGroup(), DEFAULT_GROUP);
        JobKey jobKey = JobKey.jobKey(param.getJobName(), group);
        try {
            if (scheduler.checkExists(jobKey)) {
                throw JobErrorCode.JOB_ALREADY_EXISTS.exception("任务已存在: " + group + "." + param.getJobName());
            }
            JobDetail jobDetail = buildJobDetail(jobKey, concurrentMode, param.getServiceName(),
                param.getHandlerName(), param.getParams(), param.getDescription());
            Trigger trigger = buildTrigger(jobKey, scheduleType, scheduleConf, misfire);
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Create job success: {}.{}, type={}, concurrent={}, handler={}@{}",
                group, param.getJobName(), scheduleType, concurrentMode,
                param.getHandlerName(), param.getServiceName());
        } catch (SchedulerException e) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("创建任务失败: " + e.getMessage());
        }
    }

    @Override
    public void updateJob(JobUpdateParam param) {
        String group = StrUtil.blankToDefault(param.getJobGroup(), DEFAULT_GROUP);
        JobKey jobKey = JobKey.jobKey(param.getJobName(), group);
        try {
            JobDetail oldDetail = scheduler.getJobDetail(jobKey);
            if (oldDetail == null) {
                throw JobErrorCode.JOB_NOT_FOUND.exception("任务不存在: " + group + "." + param.getJobName());
            }
            // 更新调度配置：类型/配置缺省时沿用原触发器
            Trigger oldTrigger = scheduler.getTrigger(TriggerKey.triggerKey(param.getJobName(), group));
            JobScheduleTypeEnum scheduleType = param.getScheduleType() != null
                ? resolveScheduleType(param.getScheduleType()) : inferScheduleType(oldTrigger);
            String scheduleConf = StrUtil.isNotBlank(param.resolveScheduleConf())
                ? param.resolveScheduleConf() : inferScheduleConf(oldTrigger);
            validateSchedule(scheduleType, scheduleConf);
            JobConcurrentModeEnum concurrentMode = JobConcurrentModeEnum.ofDefault(param.getConcurrentMode());
            JobMisfireStrategyEnum misfire = JobMisfireStrategyEnum.ofDefault(param.getMisfireStrategy());

            // 保留原有的目标服务与处理器，仅更新并发模式/参数/描述
            JobDetail newDetail = buildJobDetail(jobKey, concurrentMode,
                oldDetail.getJobDataMap().getString(AbstractDispatchJob.DATA_SERVICE_NAME),
                oldDetail.getJobDataMap().getString(AbstractDispatchJob.DATA_HANDLER_NAME),
                param.getParams(), param.getDescription());
            scheduler.addJob(newDetail, true);
            Trigger newTrigger = buildTrigger(jobKey, scheduleType, scheduleConf, misfire);
            scheduler.rescheduleJob(TriggerKey.triggerKey(param.getJobName(), group), newTrigger);
            log.info("Update job success: {}.{}, type={}", group, param.getJobName(), scheduleType);
        } catch (SchedulerException e) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("更新任务失败: " + e.getMessage());
        }
    }

    @Override
    public void pauseJob(String jobName, String jobGroup) {
        try {
            scheduler.pauseJob(jobKey(jobName, jobGroup));
            log.info("Pause job: {}", jobKey(jobName, jobGroup));
        } catch (SchedulerException e) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("暂停任务失败: " + e.getMessage());
        }
    }

    @Override
    public void resumeJob(String jobName, String jobGroup) {
        try {
            scheduler.resumeJob(jobKey(jobName, jobGroup));
            log.info("Resume job: {}", jobKey(jobName, jobGroup));
        } catch (SchedulerException e) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("恢复任务失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteJob(String jobName, String jobGroup) {
        try {
            scheduler.deleteJob(jobKey(jobName, jobGroup));
            log.info("Delete job: {}", jobKey(jobName, jobGroup));
        } catch (SchedulerException e) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("删除任务失败: " + e.getMessage());
        }
    }

    @Override
    public void triggerJob(String jobName, String jobGroup) {
        try {
            JobKey jobKey = jobKey(jobName, jobGroup);
            if (!scheduler.checkExists(jobKey)) {
                throw JobErrorCode.JOB_NOT_FOUND.exception("任务不存在: " + jobKey);
            }
            // 传入触发类型标记，执行日志可区分手动触发与 cron 定时触发
            JobDataMap dataMap = new JobDataMap();
            dataMap.put(AbstractDispatchJob.DATA_TRIGGER_TYPE, JobTriggerTypeEnum.MANUAL.name());
            scheduler.triggerJob(jobKey, dataMap);
            log.info("Trigger job manually: {}", jobKey);
        } catch (SchedulerException e) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("触发任务失败: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String jobName, String jobGroup) {
        try {
            return scheduler.checkExists(jobKey(jobName, jobGroup));
        } catch (SchedulerException e) {
            log.warn("Check job exists fail: {}.{}, {}", jobGroup, jobName, e.getMessage());
            return false;
        }
    }

    @Override
    public JobVO getJobInfo(String jobName, String jobGroup) {
        try {
            JobDetail detail = scheduler.getJobDetail(jobKey(jobName, jobGroup));
            if (detail == null) {
                return null;
            }
            return toVO(detail);
        } catch (SchedulerException e) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("查询任务失败: " + e.getMessage());
        }
    }

    @Override
    public Paging<JobVO> getPage(JobPageParam param) {
        try {
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyGroup());
            List<JobVO> all = new ArrayList<>();
            for (JobKey jobKey : jobKeys) {
                JobDetail detail = scheduler.getJobDetail(jobKey);
                if (detail != null) {
                    all.add(toVO(detail));
                }
            }
            List<JobVO> filtered = all.stream()
                .filter(job -> StrUtil.isBlank(param.getJobName()) || job.getJobName().contains(param.getJobName()))
                .filter(job -> StrUtil.isBlank(param.getJobGroup()) || param.getJobGroup().equals(job.getJobGroup()))
                .filter(job -> StrUtil.isBlank(param.getServiceName())
                    || param.getServiceName().equals(job.getServiceName()))
                .filter(job -> StrUtil.isBlank(param.getHandlerName()) || job.getHandlerName() != null
                    && job.getHandlerName().contains(param.getHandlerName()))
                .filter(job -> StrUtil.isBlank(param.getStatus()) || param.getStatus().equals(job.getStatus()))
                .sorted(Comparator.comparing(JobVO::getJobGroup).thenComparing(JobVO::getJobName))
                .toList();
            // 钳制单页上限，防大查询
            int size = Math.min(Math.max(param.getSize(), 1), MAX_PAGE_SIZE);
            return inMemoryPage(filtered, param.getCurrent(), size);
        } catch (SchedulerException e) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("查询任务列表失败: " + e.getMessage());
        }
    }

    @Override
    public List<LocalDateTime> previewFireTimes(Integer scheduleType, String scheduleConf, int count) {
        JobScheduleTypeEnum type = resolveScheduleType(scheduleType);
        String conf = resolveScheduleConf(type, scheduleConf);
        validateSchedule(type, conf);
        int limit = count <= 0 ? PREVIEW_COUNT : Math.min(count, 20);
        List<LocalDateTime> result = new ArrayList<>();
        try {
            if (type == JobScheduleTypeEnum.CRON) {
                CronExpression expression = new CronExpression(conf);
                Date cursor = new Date();
                for (int i = 0; i < limit; i++) {
                    Date next = expression.getNextValidTimeAfter(cursor);
                    if (next == null) {
                        break;
                    }
                    result.add(toLocalDateTime(next));
                    cursor = next;
                }
            } else if (type == JobScheduleTypeEnum.FIXED_INTERVAL) {
                long interval = Long.parseLong(conf);
                LocalDateTime now = LocalDateTime.now();
                for (int i = 1; i <= limit; i++) {
                    result.add(now.plusNanos(interval * i * 1_000_000L));
                }
            } else {
                // 一次性仅一个触发点
                result.add(parseOnceTime(conf));
            }
        } catch (ParseException e) {
            throw JobErrorCode.JOB_PARAM_CRON_INVALID.exception("调度配置无效: " + conf);
        }
        return result;
    }

    // ==================== 调度类型与配置解析 ====================

    private JobScheduleTypeEnum resolveScheduleType(Integer scheduleType) {
        JobScheduleTypeEnum type = JobScheduleTypeEnum.of(scheduleType);
        return type == null ? JobScheduleTypeEnum.CRON : type;
    }

    /**
     * 校验并按类型规范化调度配置（固定间隔校验下限，一次性校验未来时间，CRON 校验合法性+间隔下限）
     */
    private String resolveScheduleConf(JobScheduleTypeEnum type, String conf) {
        if (StrUtil.isBlank(conf)) {
            throw JobErrorCode.JOB_PARAM_CRON_INVALID.exception("调度配置不能为空");
        }
        return conf.trim();
    }

    private void validateSchedule(JobScheduleTypeEnum type, String conf) {
        switch (type) {
            case CRON -> validateCron(conf);
            case FIXED_INTERVAL -> validateInterval(conf);
            case ONCE -> validateOnce(conf);
            default -> throw JobErrorCode.JOB_PARAM_CRON_INVALID.exception("不支持的调度类型: " + type);
        }
    }

    /**
     * 校验 cron 表达式合法性，并限制最小调度间隔（防高频任务拖垮调度线程池与下游服务）
     */
    private void validateCron(String cronExpression) {
        if (StrUtil.isBlank(cronExpression) || !CronExpression.isValidExpression(cronExpression)) {
            throw JobErrorCode.JOB_PARAM_CRON_INVALID.exception("cron 表达式无效: " + cronExpression);
        }
        try {
            CronExpression expression = new CronExpression(cronExpression);
            Date first = expression.getNextValidTimeAfter(new Date());
            if (first == null) {
                throw JobErrorCode.JOB_PARAM_CRON_INVALID.exception("cron 表达式无未来触发时间: " + cronExpression);
            }
            Date second = expression.getNextValidTimeAfter(first);
            if (second != null && second.getTime() - first.getTime() < MIN_INTERVAL_MS) {
                throw JobErrorCode.JOB_PARAM_CRON_INVALID.exception(
                    "cron 调度间隔过小（最小 " + (MIN_INTERVAL_MS / 1000) + " 秒）: " + cronExpression);
            }
        } catch (ParseException e) {
            throw JobErrorCode.JOB_PARAM_CRON_INVALID.exception("cron 表达式无效: " + cronExpression);
        }
    }

    /**
     * 校验固定间隔（毫秒，不低于下限）
     */
    private void validateInterval(String conf) {
        long interval;
        try {
            interval = Long.parseLong(conf.trim());
        } catch (NumberFormatException e) {
            throw JobErrorCode.JOB_PARAM_CRON_INVALID.exception("固定间隔必须为毫秒数: " + conf);
        }
        if (interval < MIN_INTERVAL_MS) {
            throw JobErrorCode.JOB_PARAM_CRON_INVALID.exception(
                "固定间隔过小（最小 " + (MIN_INTERVAL_MS / 1000) + " 秒）: " + conf);
        }
    }

    /**
     * 校验一次性触发时间（ISO 格式，须在未来）
     */
    private void validateOnce(String conf) {
        LocalDateTime fireTime = parseOnceTime(conf);
        if (fireTime.isBefore(LocalDateTime.now())) {
            throw JobErrorCode.JOB_PARAM_CRON_INVALID.exception("一次性触发时间必须在未来: " + conf);
        }
    }

    private LocalDateTime parseOnceTime(String conf) {
        try {
            return LocalDateTime.parse(conf.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            throw JobErrorCode.JOB_PARAM_CRON_INVALID.exception(
                "一次性触发时间格式无效(需 ISO yyyy-MM-ddTHH:mm:ss): " + conf);
        }
    }

    // ==================== Trigger / JobDetail 构建 ====================

    private JobKey jobKey(String jobName, String jobGroup) {
        return JobKey.jobKey(jobName, StrUtil.blankToDefault(jobGroup, DEFAULT_GROUP));
    }

    /**
     * 按并发模式选择派发 Job 类（串行=集群互斥，并行=可重入）
     */
    private Class<? extends Job> dispatchJobClass(JobConcurrentModeEnum concurrentMode) {
        return concurrentMode == JobConcurrentModeEnum.PARALLEL
            ? ParallelDispatchJob.class : SerialDispatchJob.class;
    }

    private JobDetail buildJobDetail(JobKey jobKey, JobConcurrentModeEnum concurrentMode, String serviceName,
                                     String handlerName, String params, String description) {
        return JobBuilder.newJob(dispatchJobClass(concurrentMode))
            .withIdentity(jobKey)
            .withDescription(description)
            .usingJobData(AbstractDispatchJob.DATA_SERVICE_NAME, StrUtil.blankToDefault(serviceName, ""))
            .usingJobData(AbstractDispatchJob.DATA_HANDLER_NAME, StrUtil.blankToDefault(handlerName, ""))
            .usingJobData(AbstractDispatchJob.DATA_PARAMS, StrUtil.blankToDefault(params, ""))
            .storeDurably(true)
            .build();
    }

    /**
     * 按调度类型与 misfire 策略构建触发器
     */
    private Trigger buildTrigger(JobKey jobKey, JobScheduleTypeEnum type, String conf,
                                 JobMisfireStrategyEnum misfire) {
        TriggerBuilder<Trigger> builder = TriggerBuilder.newTrigger()
            .withIdentity(jobKey.getName(), jobKey.getGroup());
        return switch (type) {
            case CRON -> builder.withSchedule(cronSchedule(conf, misfire)).build();
            case FIXED_INTERVAL -> builder.withSchedule(intervalSchedule(conf, misfire)).build();
            case ONCE -> builder.startAt(toDate(parseOnceTime(conf))).build();
        };
    }

    /**
     * CRON 调度 + misfire 指令映射
     */
    private CronScheduleBuilder cronSchedule(String cron, JobMisfireStrategyEnum misfire) {
        CronScheduleBuilder builder = CronScheduleBuilder.cronSchedule(cron);
        return switch (misfire) {
            case IGNORE_MISFIRES -> builder.withMisfireHandlingInstructionIgnoreMisfires();
            case FIRE_AND_PROCEED -> builder.withMisfireHandlingInstructionFireAndProceed();
            case DO_NOTHING -> builder.withMisfireHandlingInstructionDoNothing();
            default -> builder;
        };
    }

    /**
     * 固定间隔调度（永久重复）+ misfire 指令映射
     */
    private SimpleScheduleBuilder intervalSchedule(String conf, JobMisfireStrategyEnum misfire) {
        long interval = Long.parseLong(conf.trim());
        SimpleScheduleBuilder builder = SimpleScheduleBuilder.simpleSchedule()
            .withIntervalInMilliseconds(interval)
            .repeatForever();
        return switch (misfire) {
            case IGNORE_MISFIRES -> builder.withMisfireHandlingInstructionIgnoreMisfires();
            case FIRE_AND_PROCEED -> builder.withMisfireHandlingInstructionFireNow();
            case DO_NOTHING -> builder.withMisfireHandlingInstructionNextWithExistingCount();
            default -> builder;
        };
    }

    // ==================== 视图对象装配 ====================

    private JobScheduleTypeEnum inferScheduleType(Trigger trigger) {
        if (trigger instanceof CronTrigger) {
            return JobScheduleTypeEnum.CRON;
        }
        if (trigger instanceof SimpleTrigger simpleTrigger) {
            return simpleTrigger.getRepeatCount() == 0 && simpleTrigger.getRepeatInterval() == 0
                ? JobScheduleTypeEnum.ONCE : JobScheduleTypeEnum.FIXED_INTERVAL;
        }
        return JobScheduleTypeEnum.CRON;
    }

    private String inferScheduleConf(Trigger trigger) {
        if (trigger instanceof CronTrigger cronTrigger) {
            return cronTrigger.getCronExpression();
        }
        if (trigger instanceof SimpleTrigger simpleTrigger) {
            if (simpleTrigger.getRepeatCount() == 0 && simpleTrigger.getRepeatInterval() == 0) {
                return toLocalDateTime(simpleTrigger.getStartTime())
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            return String.valueOf(simpleTrigger.getRepeatInterval());
        }
        return null;
    }

    private JobVO toVO(JobDetail detail) throws SchedulerException {
        JobVO vo = new JobVO()
            .setJobName(detail.getKey().getName())
            .setJobGroup(detail.getKey().getGroup())
            .setServiceName(detail.getJobDataMap().getString(AbstractDispatchJob.DATA_SERVICE_NAME))
            .setHandlerName(detail.getJobDataMap().getString(AbstractDispatchJob.DATA_HANDLER_NAME))
            .setParams(detail.getJobDataMap().getString(AbstractDispatchJob.DATA_PARAMS))
            .setDescription(detail.getDescription());
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(detail.getKey());
        if (!triggers.isEmpty()) {
            Trigger trigger = triggers.get(0);
            if (trigger instanceof CronTrigger cronTrigger) {
                vo.setCronExpression(cronTrigger.getCronExpression());
            }
            vo.setPrevFireTime(toLocalDateTime(trigger.getPreviousFireTime()));
            vo.setNextFireTime(toLocalDateTime(trigger.getNextFireTime()));
            vo.setStatus(scheduler.getTriggerState(trigger.getKey()).name());
        }
        return vo;
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Date toDate(LocalDateTime time) {
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }

    private Paging<JobVO> inMemoryPage(List<JobVO> all, int current, int size) {
        int total = all.size();
        int from = Math.min(Math.max((current - 1) * size, 0), total);
        int to = Math.min(from + size, total);
        int pages = size > 0 ? (total + size - 1) / size : 0;
        return new Paging<JobVO>()
            .setTotal(total)
            .setRecords(all.subList(from, to))
            .setCurrent(current)
            .setSize(size)
            .setPages(pages);
    }

}
