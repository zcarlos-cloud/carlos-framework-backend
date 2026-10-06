package com.carlos.job.listener;

import cn.hutool.core.util.StrUtil;
import com.carlos.job.dispatch.AbstractDispatchJob;
import com.carlos.job.manager.JobInfoManager;
import com.carlos.job.pojo.dto.JobInfoDTO;
import com.carlos.job.pojo.enums.JobTriggerTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.util.Date;

/**
 * <p>
 * 失败重试监听器：任务执行失败后按 job_info.failRetryCount 进行重调度重试
 * </p>
 * <p>
 * Quartz 原生重调度模式：失败后挂一个立即触发的一次性 SimpleTrigger 重跑任务，
 * retryIndex 进 JobDataMap 追踪重试序号。不占调度线程等待、集群下可故障转移、
 * 每次重试独立落一条执行日志。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Slf4j
public class JobRetryListener implements JobListener {

    public static final String NAME = "jobRetryListener";

    /**
     * 重试触发器分组（与业务触发器隔离，便于清理）
     */
    public static final String RETRY_TRIGGER_GROUP = "CARLOS_JOB_RETRY";

    private final Scheduler scheduler;

    private final JobInfoManager jobInfoManager;

    public JobRetryListener(Scheduler scheduler, JobInfoManager jobInfoManager) {
        this.scheduler = scheduler;
        this.jobInfoManager = jobInfoManager;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        // 无需前置动作
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        // 无需处理
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        if (jobException == null) {
            return;
        }
        JobKey jobKey = context.getJobDetail().getKey();
        try {
            JobInfoDTO info = jobInfoManager.getByJobKey(jobKey.getName(), jobKey.getGroup());
            int maxRetry = info == null || info.getFailRetryCount() == null ? 0 : info.getFailRetryCount();
            int retryIndex = safeRetryIndex(context);
            if (retryIndex >= maxRetry) {
                log.info("任务重试次数用尽: {}.{}, retry={}/{}", jobKey.getGroup(), jobKey.getName(),
                    retryIndex, maxRetry);
                return;
            }
            scheduleRetry(context, jobKey, retryIndex + 1);
        } catch (Exception e) {
            // 重试调度失败不影响原失败结果
            log.error("任务失败重试调度异常: {}", jobKey, e);
        }
    }

    /**
     * 挂一个立即触发的一次性重试 Trigger，retryIndex 递增写入 JobDataMap
     */
    private void scheduleRetry(JobExecutionContext context, JobKey jobKey, int nextRetryIndex)
        throws SchedulerException {
        JobDataMap dataMap = new JobDataMap(context.getMergedJobDataMap());
        dataMap.put(AbstractDispatchJob.DATA_RETRY_INDEX, String.valueOf(nextRetryIndex));
        dataMap.put(AbstractDispatchJob.DATA_TRIGGER_TYPE, JobTriggerTypeEnum.RETRY.name());
        Trigger retryTrigger = TriggerBuilder.newTrigger()
            .withIdentity(jobKey.getName() + "_retry_" + nextRetryIndex, RETRY_TRIGGER_GROUP)
            .forJob(jobKey)
            .usingJobData(dataMap)
            .startAt(new Date())
            .build();
        scheduler.scheduleJob(retryTrigger);
        log.info("任务失败重试已调度: {}.{}, 第 {} 次", jobKey.getGroup(), jobKey.getName(), nextRetryIndex);
    }

    /**
     * 安全读取当前重试序号（JobDataMap 可能缺该键或非数字）
     */
    private int safeRetryIndex(JobExecutionContext context) {
        String value = context.getMergedJobDataMap().getString(AbstractDispatchJob.DATA_RETRY_INDEX);
        if (StrUtil.isBlank(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
