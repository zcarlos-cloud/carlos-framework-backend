package com.carlos.job.listener;

import com.carlos.job.dispatch.AbstractDispatchJob;
import com.carlos.job.manager.JobInfoManager;
import com.carlos.job.pojo.dto.JobInfoDTO;
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
import org.quartz.Scheduler;
import org.quartz.Trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 失败重试监听器单元测试
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@ExtendWith(MockitoExtension.class)
class JobRetryListenerTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private JobInfoManager jobInfoManager;

    @Mock
    private JobExecutionContext context;

    @Mock
    private JobDetail jobDetail;

    private JobRetryListener listener;

    private final JobKey jobKey = JobKey.jobKey("demoJob", "DEFAULT");

    @BeforeEach
    void setUp() {
        listener = new JobRetryListener(scheduler, jobInfoManager);
        // lenient: 成功场景不走 jobDetail 桩
        lenient().when(context.getJobDetail()).thenReturn(jobDetail);
        lenient().when(jobDetail.getKey()).thenReturn(jobKey);
    }

    @Test
    @DisplayName("应该不重试当任务执行成功时")
    void should_not_retry_when_success() {
        listener.jobWasExecuted(context, null);

        verify(jobInfoManager, never()).getByJobKey(any(), any());
    }

    @Test
    @DisplayName("应该调度重试当失败且重试次数未用尽时")
    void should_schedule_retry_when_fail_and_quota_left() throws Exception {
        JobInfoDTO info = new JobInfoDTO().setFailRetryCount(3);
        when(jobInfoManager.getByJobKey("demoJob", "DEFAULT")).thenReturn(info);
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());

        listener.jobWasExecuted(context, new JobExecutionException("boom"));

        ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(captor.capture());
        Trigger trigger = captor.getValue();
        assertEquals("demoJob_retry_1", trigger.getKey().getName());
        assertEquals("1", trigger.getJobDataMap().getString(AbstractDispatchJob.DATA_RETRY_INDEX));
    }

    @Test
    @DisplayName("应该不重试当重试次数用尽时")
    void should_not_retry_when_quota_exhausted() throws Exception {
        JobInfoDTO info = new JobInfoDTO().setFailRetryCount(2);
        when(jobInfoManager.getByJobKey("demoJob", "DEFAULT")).thenReturn(info);
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(AbstractDispatchJob.DATA_RETRY_INDEX, "2");
        when(context.getMergedJobDataMap()).thenReturn(dataMap);

        listener.jobWasExecuted(context, new JobExecutionException("boom"));

        verify(scheduler, never()).scheduleJob(any(Trigger.class));
    }

    @Test
    @DisplayName("应该不重试当任务元数据缺失时")
    void should_not_retry_when_metadata_missing() throws Exception {
        when(jobInfoManager.getByJobKey("demoJob", "DEFAULT")).thenReturn(null);
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());

        listener.jobWasExecuted(context, new JobExecutionException("boom"));

        verify(scheduler, never()).scheduleJob(any(Trigger.class));
    }

    @Test
    @DisplayName("应该按当前重试序号递增当已是重试触发时")
    void should_increment_retry_index_when_already_retrying() throws Exception {
        JobInfoDTO info = new JobInfoDTO().setFailRetryCount(5);
        when(jobInfoManager.getByJobKey("demoJob", "DEFAULT")).thenReturn(info);
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(AbstractDispatchJob.DATA_RETRY_INDEX, "2");
        when(context.getMergedJobDataMap()).thenReturn(dataMap);

        listener.jobWasExecuted(context, new JobExecutionException("boom"));

        ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(captor.capture());
        assertEquals("3", captor.getValue().getJobDataMap().getString(AbstractDispatchJob.DATA_RETRY_INDEX));
    }

}
