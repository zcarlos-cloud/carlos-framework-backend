package com.carlos.job.manager.impl;

import com.carlos.core.exception.BusinessException;
import com.carlos.job.enums.JobErrorCode;
import com.carlos.job.pojo.param.JobCreateParam;
import com.carlos.job.pojo.param.JobUpdateParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 任务调度封装单元测试
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@ExtendWith(MockitoExtension.class)
class JobScheduleManagerImplTest {

    @Mock
    private Scheduler scheduler;

    @InjectMocks
    private JobScheduleManagerImpl scheduleManager;

    private JobCreateParam createParam;

    @BeforeEach
    void setUp() {
        createParam = new JobCreateParam()
            .setJobName("demoJob")
            .setServiceName("carlos-message")
            .setHandlerName("demoJobHandler")
            .setCronExpression("0 0 2 * * ?");
    }

    @Test
    @DisplayName("应该成功创建任务当参数合法且任务不存在时")
    void should_create_job_successfully_when_params_valid() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        scheduleManager.createJob(createParam);

        verify(scheduler).scheduleJob(any(JobDetail.class), any());
    }

    @Test
    @DisplayName("应该抛出已存在异常当任务重复创建时")
    void should_throw_already_exists_when_job_duplicated() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> scheduleManager.createJob(createParam));

        assertEquals(JobErrorCode.JOB_ALREADY_EXISTS.getCode(), exception.getErrorCode());
        verify(scheduler, never()).scheduleJob(any(JobDetail.class), any());
    }

    @Test
    @DisplayName("应该抛出参数异常当cron表达式非法时")
    void should_throw_param_error_when_cron_invalid() {
        createParam.setCronExpression("not-a-cron");

        BusinessException exception = assertThrows(BusinessException.class,
            () -> scheduleManager.createJob(createParam));

        assertEquals(JobErrorCode.JOB_PARAM_CRON_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("应该抛出不存在异常当更新不存在的任务时")
    void should_throw_not_found_when_update_missing_job() throws SchedulerException {
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(null);
        JobUpdateParam updateParam = new JobUpdateParam()
            .setJobName("missingJob")
            .setCronExpression("0 0 3 * * ?");

        BusinessException exception = assertThrows(BusinessException.class,
            () -> scheduleManager.updateJob(updateParam));

        assertEquals(JobErrorCode.JOB_NOT_FOUND.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("应该抛出不存在异常当手动触发不存在的任务时")
    void should_throw_not_found_when_trigger_missing_job() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> scheduleManager.triggerJob("missingJob", null));

        assertEquals(JobErrorCode.JOB_NOT_FOUND.getCode(), exception.getErrorCode());
        verify(scheduler, never()).triggerJob(any(JobKey.class));
    }

    @Test
    @DisplayName("应该返回存在性当检查任务时")
    void should_return_exists_when_checking_job() throws SchedulerException {
        when(scheduler.checkExists(JobKey.jobKey("demoJob", "DEFAULT"))).thenReturn(true);
        when(scheduler.checkExists(JobKey.jobKey("otherJob", "DEFAULT"))).thenReturn(false);

        assertTrue(scheduleManager.exists("demoJob", null));
        assertFalse(scheduleManager.exists("otherJob", null));
    }

    // ==================== J1: 多触发类型 / 并发模式 / misfire / 预览 ====================

    @Test
    @DisplayName("应该创建固定间隔任务当scheduleType为2时")
    void should_create_fixed_interval_job_when_type_2() throws SchedulerException {
        createParam.setScheduleType(2).setScheduleConf("60000");
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        scheduleManager.createJob(createParam);

        verify(scheduler).scheduleJob(any(JobDetail.class), any(org.quartz.SimpleTrigger.class));
    }

    @Test
    @DisplayName("应该创建一次性任务当scheduleType为3且触发时间在未来时")
    void should_create_once_job_when_type_3() throws SchedulerException {
        createParam.setScheduleType(3).setScheduleConf("2099-01-01T00:00:00");
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        scheduleManager.createJob(createParam);

        verify(scheduler).scheduleJob(any(JobDetail.class), any());
    }

    @Test
    @DisplayName("应该抛出参数异常当一次性触发时间在过去时")
    void should_throw_when_once_time_in_past() {
        createParam.setScheduleType(3).setScheduleConf("2000-01-01T00:00:00");

        BusinessException exception = assertThrows(BusinessException.class,
            () -> scheduleManager.createJob(createParam));

        assertEquals(JobErrorCode.JOB_PARAM_CRON_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("应该抛出参数异常当固定间隔小于下限时")
    void should_throw_when_interval_too_small() {
        createParam.setScheduleType(2).setScheduleConf("1000");

        BusinessException exception = assertThrows(BusinessException.class,
            () -> scheduleManager.createJob(createParam));

        assertEquals(JobErrorCode.JOB_PARAM_CRON_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("应该抛出参数异常当固定间隔非数字时")
    void should_throw_when_interval_not_numeric() {
        createParam.setScheduleType(2).setScheduleConf("abc");

        BusinessException exception = assertThrows(BusinessException.class,
            () -> scheduleManager.createJob(createParam));

        assertEquals(JobErrorCode.JOB_PARAM_CRON_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("应该使用并行Job类当并发模式为2时")
    void should_use_parallel_job_class_when_concurrent_mode_parallel() throws SchedulerException {
        createParam.setConcurrentMode(2);
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        ArgumentCaptor<JobDetail> captor = ArgumentCaptor.forClass(JobDetail.class);

        scheduleManager.createJob(createParam);

        verify(scheduler).scheduleJob(captor.capture(), any());
        assertEquals(com.carlos.job.dispatch.ParallelDispatchJob.class, captor.getValue().getJobClass());
    }

    @Test
    @DisplayName("应该默认使用串行Job类当并发模式为空时")
    void should_use_serial_job_class_when_concurrent_mode_default() throws SchedulerException {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        ArgumentCaptor<JobDetail> captor = ArgumentCaptor.forClass(JobDetail.class);

        scheduleManager.createJob(createParam);

        verify(scheduler).scheduleJob(captor.capture(), any());
        assertEquals(com.carlos.job.dispatch.SerialDispatchJob.class, captor.getValue().getJobClass());
    }

    @Test
    @DisplayName("应该预览未来5次cron触发时间")
    void should_preview_cron_fire_times() {
        List<java.time.LocalDateTime> times = scheduleManager.previewFireTimes(1, "0 0 2 * * ?", 5);

        assertEquals(5, times.size());
        // 递增
        for (int i = 1; i < times.size(); i++) {
            assertTrue(times.get(i).isAfter(times.get(i - 1)));
        }
    }

    @Test
    @DisplayName("应该预览固定间隔触发时间")
    void should_preview_interval_fire_times() {
        List<java.time.LocalDateTime> times = scheduleManager.previewFireTimes(2, "60000", 3);

        assertEquals(3, times.size());
    }

    @Test
    @DisplayName("应该预览一次性触发时间为单个时间点")
    void should_preview_once_fire_time() {
        List<java.time.LocalDateTime> times = scheduleManager.previewFireTimes(3, "2099-01-01T00:00:00", 5);

        assertEquals(1, times.size());
        assertEquals(java.time.LocalDateTime.of(2099, 1, 1, 0, 0), times.get(0));
    }

    @Test
    @DisplayName("应该抛出参数异常当预览非法cron时")
    void should_throw_when_preview_invalid_cron() {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> scheduleManager.previewFireTimes(1, "bad-cron", 5));

        assertEquals(JobErrorCode.JOB_PARAM_CRON_INVALID.getCode(), exception.getErrorCode());
    }

}
