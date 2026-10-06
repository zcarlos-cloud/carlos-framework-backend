package com.carlos.job.listener;

import cn.hutool.core.util.StrUtil;
import com.carlos.job.config.JobProperties;
import com.carlos.job.dispatch.AbstractDispatchJob;
import com.carlos.job.manager.JobInfoManager;
import com.carlos.job.pojo.dto.JobInfoDTO;
import com.carlos.message.api.MessageSendApi;
import com.carlos.message.pojo.param.MessageSendParam;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 任务失败报警监听器：任务最终失败（重试用尽）后通过 carlos-message 发送报警
 * </p>
 * <p>
 * 挂在 Quartz {@link JobListener} 扩展点，与派发主链路解耦；消息服务不可达时降级，
 * 仅记日志不阻断调度。接收人取 job_info.alarm_receivers（逗号分隔），按负责人路由。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Slf4j
public class JobAlarmListener implements JobListener {

    public static final String NAME = "jobAlarmListener";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JobProperties jobProperties;

    private final JobInfoManager jobInfoManager;

    private final ObjectProvider<MessageSendApi> messageSendApiProvider;

    public JobAlarmListener(JobProperties jobProperties, JobInfoManager jobInfoManager,
                            ObjectProvider<MessageSendApi> messageSendApiProvider) {
        this.jobProperties = jobProperties;
        this.jobInfoManager = jobInfoManager;
        this.messageSendApiProvider = messageSendApiProvider;
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
        if (jobException == null || !Boolean.TRUE.equals(jobProperties.getAlarm().getEnabled())) {
            return;
        }
        JobKey jobKey = context.getJobDetail().getKey();
        try {
            JobInfoDTO info = jobInfoManager.getByJobKey(jobKey.getName(), jobKey.getGroup());
            // 仅在最终失败（重试已用尽）时报警，避免每次重试都刷报警
            if (hasRetryQuotaLeft(context, info)) {
                return;
            }
            sendAlarm(context, info, jobException);
        } catch (Exception e) {
            // 报警失败不阻断调度主链路
            log.error("任务失败报警发送异常: {}", jobKey, e);
        }
    }

    /**
     * 是否还有重试配额（有则本次失败先不报警，等重试结果）
     */
    private boolean hasRetryQuotaLeft(JobExecutionContext context, JobInfoDTO info) {
        int maxRetry = info == null || info.getFailRetryCount() == null ? 0 : info.getFailRetryCount();
        if (maxRetry <= 0) {
            return false;
        }
        int retryIndex = safeRetryIndex(context);
        return retryIndex < maxRetry;
    }

    private int safeRetryIndex(JobExecutionContext context) {
        String value = context.getMergedJobDataMap()
            .getString(AbstractDispatchJob.DATA_RETRY_INDEX);
        if (StrUtil.isBlank(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 发送报警消息（接收人为空则跳过；消息服务不可用则降级记日志）
     */
    private void sendAlarm(JobExecutionContext context, JobInfoDTO info, JobExecutionException jobException) {
        String receivers = info == null ? null : info.getAlarmReceivers();
        if (StrUtil.isBlank(receivers)) {
            log.debug("任务无报警接收人，跳过报警: {}", context.getJobDetail().getKey());
            return;
        }
        MessageSendApi messageSendApi = messageSendApiProvider.getIfAvailable();
        if (messageSendApi == null) {
            log.warn("消息服务不可用，任务失败报警降级: {}", context.getJobDetail().getKey());
            return;
        }
        MessageSendParam param = buildAlarmParam(context, info, receivers, jobException);
        messageSendApi.sendAsync(param);
        log.info("任务失败报警已发送: {}, receivers={}", context.getJobDetail().getKey(), receivers);
    }

    private MessageSendParam buildAlarmParam(JobExecutionContext context, JobInfoDTO info,
                                             String receivers, JobExecutionException jobException) {
        JobKey jobKey = context.getJobDetail().getKey();
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("jobName", jobKey.getName());
        templateParams.put("jobGroup", jobKey.getGroup());
        templateParams.put("serviceName", info == null ? "" : StrUtil.blankToDefault(info.getServiceName(), ""));
        templateParams.put("handlerName", info == null ? "" : StrUtil.blankToDefault(info.getHandlerName(), ""));
        templateParams.put("errorMsg", StrUtil.maxLength(jobException.getMessage(), 500));
        templateParams.put("failTime", LocalDateTime.now().format(TIME_FMT));

        List<MessageSendParam.ReceiverParam> receiverParams = Arrays.stream(receivers.split(","))
            .map(String::trim)
            .filter(StrUtil::isNotBlank)
            .map(id -> {
                MessageSendParam.ReceiverParam receiver = new MessageSendParam.ReceiverParam();
                receiver.setReceiverId(id);
                receiver.setReceiverType(1);
                return receiver;
            })
            .toList();

        MessageSendParam param = new MessageSendParam();
        param.setTemplateCode(jobProperties.getAlarm().getTemplateCode());
        param.setTemplateParams(templateParams);
        param.setReceivers(receiverParams);
        param.setSenderId(jobKey.getName());
        param.setSenderName("调度中心");
        param.setSenderSystem(jobProperties.getAlarm().getSenderSystem());
        param.setPriority(2);
        return param;
    }

}
