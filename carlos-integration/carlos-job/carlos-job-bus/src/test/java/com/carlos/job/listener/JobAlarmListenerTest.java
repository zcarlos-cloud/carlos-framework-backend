package com.carlos.job.listener;

import com.carlos.job.config.JobProperties;
import com.carlos.job.manager.JobInfoManager;
import com.carlos.job.pojo.dto.JobInfoDTO;
import com.carlos.message.api.MessageSendApi;
import com.carlos.message.pojo.param.MessageSendParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 任务失败报警监听器单元测试
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@ExtendWith(MockitoExtension.class)
class JobAlarmListenerTest {

    @Mock
    private JobInfoManager jobInfoManager;

    @Mock
    private MessageSendApi messageSendApi;

    @Mock
    private ObjectProvider<MessageSendApi> messageSendApiProvider;

    @Mock
    private JobExecutionContext context;

    @Mock
    private JobDetail jobDetail;

    private JobProperties jobProperties;

    private JobAlarmListener listener;

    private final JobKey jobKey = JobKey.jobKey("demoJob", "DEFAULT");

    @BeforeEach
    void setUp() {
        jobProperties = new JobProperties();
        jobProperties.getAlarm().setEnabled(true);
        listener = new JobAlarmListener(jobProperties, jobInfoManager, messageSendApiProvider);
        lenient().when(context.getJobDetail()).thenReturn(jobDetail);
        lenient().when(jobDetail.getKey()).thenReturn(jobKey);
        lenient().when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());
    }

    @Test
    @DisplayName("应该不报警当任务执行成功时")
    void should_not_alarm_when_success() {
        listener.jobWasExecuted(context, null);

        verify(messageSendApiProvider, never()).getIfAvailable();
    }

    @Test
    @DisplayName("应该不报警当开关关闭时")
    void should_not_alarm_when_disabled() {
        jobProperties.getAlarm().setEnabled(false);

        listener.jobWasExecuted(context, new JobExecutionException("boom"));

        verify(jobInfoManager, never()).getByJobKey(any(), any());
    }

    @Test
    @DisplayName("应该发送报警当失败且有接收人时")
    void should_send_alarm_when_fail_with_receivers() {
        JobInfoDTO info = new JobInfoDTO()
            .setServiceName("carlos-message")
            .setHandlerName("demoHandler")
            .setFailRetryCount(0)
            .setAlarmReceivers("1001,1002");
        when(jobInfoManager.getByJobKey("demoJob", "DEFAULT")).thenReturn(info);
        when(messageSendApiProvider.getIfAvailable()).thenReturn(messageSendApi);

        listener.jobWasExecuted(context, new JobExecutionException("boom"));

        ArgumentCaptor<MessageSendParam> captor = ArgumentCaptor.forClass(MessageSendParam.class);
        verify(messageSendApi).sendAsync(captor.capture());
        MessageSendParam param = captor.getValue();
        assertEquals("JOB_FAIL_ALARM", param.getTemplateCode());
        assertEquals(2, param.getReceivers().size());
        assertEquals("carlos-job", param.getSenderSystem());
        assertEquals("demoJob", param.getTemplateParams().get("jobName"));
    }

    @Test
    @DisplayName("应该不报警当无接收人时")
    void should_not_alarm_when_no_receivers() {
        JobInfoDTO info = new JobInfoDTO().setFailRetryCount(0).setAlarmReceivers(null);
        when(jobInfoManager.getByJobKey("demoJob", "DEFAULT")).thenReturn(info);

        listener.jobWasExecuted(context, new JobExecutionException("boom"));

        verify(messageSendApi, never()).sendAsync(any());
    }

    @Test
    @DisplayName("应该不报警当还有重试配额时")
    void should_not_alarm_when_retry_quota_left() {
        JobInfoDTO info = new JobInfoDTO().setFailRetryCount(3).setAlarmReceivers("1001");
        when(jobInfoManager.getByJobKey("demoJob", "DEFAULT")).thenReturn(info);
        // 当前是第 0 次失败，重试配额 3 未用尽
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());

        listener.jobWasExecuted(context, new JobExecutionException("boom"));

        verify(messageSendApi, never()).sendAsync(any());
    }

    @Test
    @DisplayName("应该降级不阻断当消息服务不可用时")
    void should_degrade_when_message_service_unavailable() {
        JobInfoDTO info = new JobInfoDTO().setFailRetryCount(0).setAlarmReceivers("1001");
        when(jobInfoManager.getByJobKey("demoJob", "DEFAULT")).thenReturn(info);
        when(messageSendApiProvider.getIfAvailable()).thenReturn(null);

        // 不抛异常即通过
        listener.jobWasExecuted(context, new JobExecutionException("boom"));

        verify(messageSendApi, never()).sendAsync(any());
    }

}
