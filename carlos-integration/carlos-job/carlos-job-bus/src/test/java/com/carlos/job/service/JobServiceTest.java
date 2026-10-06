package com.carlos.job.service;

import com.carlos.core.exception.BusinessException;
import com.carlos.job.enums.JobErrorCode;
import com.carlos.job.manager.JobInfoManager;
import com.carlos.job.manager.JobScheduleManager;
import com.carlos.job.pojo.dto.JobInfoDTO;
import com.carlos.job.pojo.enums.JobConcurrentModeEnum;
import com.carlos.job.pojo.enums.JobMisfireStrategyEnum;
import com.carlos.job.pojo.enums.JobScheduleTypeEnum;
import com.carlos.job.pojo.param.JobCreateParam;
import com.carlos.job.pojo.vo.JobVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 定时任务业务单元测试
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobScheduleManager scheduleManager;

    @Mock
    private JobInfoManager jobInfoManager;

    @InjectMocks
    private JobService jobService;

    @Test
    @DisplayName("应该创建任务并双写元数据当参数完整时")
    void should_add_job_when_params_valid() {
        JobCreateParam param = buildCreateParam();
        when(jobInfoManager.add(any(JobInfoDTO.class))).thenReturn(true);

        jobService.addJob(param);

        ArgumentCaptor<JobInfoDTO> captor = ArgumentCaptor.forClass(JobInfoDTO.class);
        verify(jobInfoManager).add(captor.capture());
        JobInfoDTO info = captor.getValue();
        assertEquals("demoJob", info.getJobName());
        assertEquals(JobScheduleManager.DEFAULT_GROUP, info.getJobGroup());
        assertEquals(JobScheduleTypeEnum.CRON.getCode(), info.getScheduleType());
        assertEquals("0 0 2 * * ?", info.getScheduleConf());
        assertEquals(JobMisfireStrategyEnum.SMART_POLICY.getCode(), info.getMisfireStrategy());
        assertEquals(JobConcurrentModeEnum.SERIAL.getCode(), info.getConcurrentMode());
        assertEquals(Trigger.TriggerState.NORMAL.name(), info.getStatus());
        verify(scheduleManager).createJob(param);
    }

    @Test
    @DisplayName("应该回滚元数据当Quartz调度失败时")
    void should_rollback_metadata_when_quartz_create_fail() {
        JobCreateParam param = buildCreateParam();
        when(jobInfoManager.add(any(JobInfoDTO.class))).thenReturn(true);
        doThrow(JobErrorCode.JOB_SCHEDULER_ERROR.exception("quartz down"))
            .when(scheduleManager).createJob(param);

        BusinessException exception = assertThrows(BusinessException.class, () -> jobService.addJob(param));

        assertEquals(JobErrorCode.JOB_SCHEDULER_ERROR.getCode(), exception.getErrorCode());
        verify(jobInfoManager).removeByJobKey("demoJob", null);
    }

    @Test
    @DisplayName("应该抛出系统异常且不触碰Quartz当元数据落库失败时")
    void should_throw_and_skip_quartz_when_metadata_persist_fail() {
        JobCreateParam param = buildCreateParam();
        when(jobInfoManager.add(any(JobInfoDTO.class))).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> jobService.addJob(param));

        assertEquals(JobErrorCode.JOB_SCHEDULER_ERROR.getCode(), exception.getErrorCode());
        verify(scheduleManager, never()).createJob(any());
    }

    @Test
    @DisplayName("应该抛出参数异常当任务名称为空时")
    void should_throw_param_error_when_job_name_blank() {
        JobCreateParam param = new JobCreateParam()
            .setServiceName("carlos-message")
            .setHandlerName("demoHandler")
            .setCronExpression("0 0 2 * * ?");

        BusinessException exception = assertThrows(BusinessException.class, () -> jobService.addJob(param));

        assertEquals(JobErrorCode.JOB_PARAM_NAME_EMPTY.getCode(), exception.getErrorCode());
        verify(scheduleManager, never()).createJob(any());
    }

    @Test
    @DisplayName("应该抛出参数异常当目标服务为空时")
    void should_throw_param_error_when_service_name_blank() {
        JobCreateParam param = new JobCreateParam()
            .setJobName("demoJob")
            .setHandlerName("demoHandler")
            .setCronExpression("0 0 2 * * ?");

        BusinessException exception = assertThrows(BusinessException.class, () -> jobService.addJob(param));

        assertEquals(JobErrorCode.JOB_PARAM_SERVICE_EMPTY.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("应该抛出不存在异常当暂停不存在的任务时")
    void should_throw_not_found_when_pause_missing_job() {
        when(scheduleManager.exists("missingJob", null)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> jobService.pauseJob("missingJob", null));

        assertEquals(JobErrorCode.JOB_NOT_FOUND.getCode(), exception.getErrorCode());
        verify(scheduleManager, never()).pauseJob("missingJob", null);
    }

    @Test
    @DisplayName("应该同步元数据状态为暂停当暂停任务时")
    void should_sync_paused_status_when_pause_job() {
        when(scheduleManager.exists("demoJob", null)).thenReturn(true);

        jobService.pauseJob("demoJob", null);

        verify(scheduleManager).pauseJob("demoJob", null);
        verify(jobInfoManager).updateStatus("demoJob", null, Trigger.TriggerState.PAUSED.name());
    }

    @Test
    @DisplayName("应该同步元数据状态为正常当恢复任务时")
    void should_sync_normal_status_when_resume_job() {
        when(scheduleManager.exists("demoJob", null)).thenReturn(true);

        jobService.resumeJob("demoJob", null);

        verify(scheduleManager).resumeJob("demoJob", null);
        verify(jobInfoManager).updateStatus("demoJob", null, Trigger.TriggerState.NORMAL.name());
    }

    @Test
    @DisplayName("应该同时删除执行面与控制面当任务两边都存在时")
    void should_delete_both_planes_when_delete_job() {
        when(scheduleManager.exists("demoJob", null)).thenReturn(true);
        when(jobInfoManager.getByJobKey("demoJob", null)).thenReturn(new JobInfoDTO());

        jobService.deleteJob("demoJob", null);

        verify(scheduleManager).deleteJob("demoJob", null);
        verify(jobInfoManager).removeByJobKey("demoJob", null);
    }

    @Test
    @DisplayName("应该只删控制面当Quartz缺失但DB有残留时")
    void should_delete_only_db_when_quartz_missing() {
        when(scheduleManager.exists("driftJob", null)).thenReturn(false);
        when(jobInfoManager.getByJobKey("driftJob", null)).thenReturn(new JobInfoDTO());

        jobService.deleteJob("driftJob", null);

        verify(scheduleManager, never()).deleteJob(any(), any());
        verify(jobInfoManager).removeByJobKey("driftJob", null);
    }

    @Test
    @DisplayName("应该抛出不存在异常当任务两边都不存在时")
    void should_throw_not_found_when_delete_missing_everywhere() {
        when(scheduleManager.exists("ghostJob", null)).thenReturn(false);
        when(jobInfoManager.getByJobKey("ghostJob", null)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> jobService.deleteJob("ghostJob", null));

        assertEquals(JobErrorCode.JOB_NOT_FOUND.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("应该回滚元数据为旧值当Quartz更新失败时")
    void should_rollback_metadata_when_quartz_update_fail() {
        com.carlos.job.pojo.param.JobUpdateParam param = new com.carlos.job.pojo.param.JobUpdateParam()
            .setJobName("demoJob")
            .setCronExpression("0 0 3 * * ?");
        JobInfoDTO oldInfo = new JobInfoDTO()
            .setJobName("demoJob")
            .setJobGroup(JobScheduleManager.DEFAULT_GROUP)
            .setScheduleConf("0 0 2 * * ?");
        when(jobInfoManager.getByJobKey("demoJob", null)).thenReturn(oldInfo);
        when(jobInfoManager.updateByJobKey(any(JobInfoDTO.class))).thenReturn(true);
        doThrow(JobErrorCode.JOB_SCHEDULER_ERROR.exception("quartz down"))
            .when(scheduleManager).updateJob(param);

        BusinessException exception = assertThrows(BusinessException.class, () -> jobService.updateJob(param));

        assertEquals(JobErrorCode.JOB_SCHEDULER_ERROR.getCode(), exception.getErrorCode());
        // 第二次 updateByJobKey 是回滚旧值
        verify(jobInfoManager).updateByJobKey(eq(oldInfo));
    }

    @Test
    @DisplayName("应该抛出不存在异常当更新元数据缺失的任务时")
    void should_throw_not_found_when_update_metadata_missing() {
        com.carlos.job.pojo.param.JobUpdateParam param = new com.carlos.job.pojo.param.JobUpdateParam()
            .setJobName("missingJob")
            .setCronExpression("0 0 3 * * ?");
        when(jobInfoManager.getByJobKey("missingJob", null)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> jobService.updateJob(param));

        assertEquals(JobErrorCode.JOB_NOT_FOUND.getCode(), exception.getErrorCode());
        verify(scheduleManager, never()).updateJob(any());
    }

    @Test
    @DisplayName("应该抛出不存在异常当查询详情不存在时")
    void should_throw_not_found_when_detail_missing() {
        when(scheduleManager.getJobInfo("missingJob", null)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> jobService.getJobInfo("missingJob", null));

        assertEquals(JobErrorCode.JOB_NOT_FOUND.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("应该返回任务详情当任务存在时")
    void should_return_job_info_when_job_exists() {
        JobVO vo = new JobVO().setJobName("demoJob").setJobGroup("DEFAULT");
        when(scheduleManager.getJobInfo("demoJob", null)).thenReturn(vo);

        JobVO result = jobService.getJobInfo("demoJob", null);

        assertEquals("demoJob", result.getJobName());
        assertEquals("DEFAULT", result.getJobGroup());
    }

    private JobCreateParam buildCreateParam() {
        return new JobCreateParam()
            .setJobName("demoJob")
            .setServiceName("carlos-message")
            .setHandlerName("demoHandler")
            .setCronExpression("0 0 2 * * ?");
    }

}
