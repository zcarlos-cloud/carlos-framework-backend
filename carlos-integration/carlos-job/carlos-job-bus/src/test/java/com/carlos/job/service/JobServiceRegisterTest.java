package com.carlos.job.service;

import com.carlos.core.exception.BusinessException;
import com.carlos.job.enums.JobErrorCode;
import com.carlos.job.manager.JobInfoManager;
import com.carlos.job.manager.JobScheduleManager;
import com.carlos.job.pojo.dto.JobInfoDTO;
import com.carlos.job.pojo.dto.JobRegisterDTO;
import com.carlos.job.pojo.param.ApiJobRegisterParam;
import com.carlos.job.pojo.param.JobCreateParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Trigger;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 注解式任务幂等注册单元测试
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@ExtendWith(MockitoExtension.class)
class JobServiceRegisterTest {

    @Mock
    private JobScheduleManager scheduleManager;

    @Mock
    private JobInfoManager jobInfoManager;

    @InjectMocks
    private JobService jobService;

    @Test
    @DisplayName("应该创建任务并落库默认分组当任务不存在时")
    void should_create_task_when_absent() {
        when(jobInfoManager.getByJobKey("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(null);
        when(scheduleManager.exists("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(false);
        when(jobInfoManager.add(any(JobInfoDTO.class))).thenReturn(true);

        JobRegisterDTO result = jobService.registerIfAbsent(buildParam(null));

        assertTrue(result.getCreated());
        assertFalse(result.getExisted());
        assertFalse(result.getDrifted());
        assertEquals(Trigger.TriggerState.NORMAL.name(), result.getStatus());
        ArgumentCaptor<JobInfoDTO> infoCaptor = ArgumentCaptor.forClass(JobInfoDTO.class);
        verify(jobInfoManager).add(infoCaptor.capture());
        assertEquals(JobScheduleManager.DEFAULT_GROUP, infoCaptor.getValue().getJobGroup());
        assertEquals("orderSyncHandler", infoCaptor.getValue().getHandlerName());
        ArgumentCaptor<JobCreateParam> createCaptor = ArgumentCaptor.forClass(JobCreateParam.class);
        verify(scheduleManager).createJob(createCaptor.capture());
        assertEquals(JobScheduleManager.DEFAULT_GROUP, createCaptor.getValue().getJobGroup());
        verify(scheduleManager, never()).pauseJob(anyString(), anyString());
    }

    @Test
    @DisplayName("应该跳过创建且不覆盖任何字段当任务已存在时")
    void should_skip_and_not_overwrite_when_exists() {
        JobInfoDTO existing = new JobInfoDTO()
            .setJobName("orderSyncJob")
            .setJobGroup(JobScheduleManager.DEFAULT_GROUP)
            .setScheduleConf("0 0 4 * * ?")
            .setHandlerName("orderSyncHandler")
            .setServiceName("carlos-order")
            .setStatus(Trigger.TriggerState.NORMAL.name());
        when(jobInfoManager.getByJobKey("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(existing);

        JobRegisterDTO result = jobService.registerIfAbsent(buildParam(null));

        assertFalse(result.getCreated());
        assertTrue(result.getExisted());
        assertEquals(Trigger.TriggerState.NORMAL.name(), result.getStatus());
        verify(jobInfoManager, never()).add(any(JobInfoDTO.class));
        verify(scheduleManager, never()).createJob(any(JobCreateParam.class));
        // 库中 cron 与注解声明不一致，仍不覆盖
        assertEquals("0 0 4 * * ?", existing.getScheduleConf());
        // 但必须把"不一致"回给调用方，否则客户端的漂移汇总永远为空
        assertTrue(result.getDrifted());
    }

    @Test
    @DisplayName("应该不做漂移标记当已存在的任务与声明一致时")
    void should_not_report_drift_when_exists_and_matches() {
        JobInfoDTO existing = new JobInfoDTO()
            .setJobName("orderSyncJob")
            .setJobGroup(JobScheduleManager.DEFAULT_GROUP)
            .setScheduleConf("0 0 2 * * ?")
            .setHandlerName("orderSyncHandler")
            .setServiceName("carlos-order")
            .setStatus(Trigger.TriggerState.NORMAL.name());
        when(jobInfoManager.getByJobKey("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(existing);

        JobRegisterDTO result = jobService.registerIfAbsent(buildParam(null));

        assertTrue(result.getExisted());
        assertFalse(result.getDrifted());
    }

    @Test
    @DisplayName("应该标记漂移态并跳过当调度器存在但元数据缺失时")
    void should_report_drift_when_quartz_has_but_db_missing() {
        when(jobInfoManager.getByJobKey("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(null);
        when(scheduleManager.exists("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(true);

        JobRegisterDTO result = jobService.registerIfAbsent(buildParam(null));

        assertFalse(result.getCreated());
        assertFalse(result.getExisted());
        assertTrue(result.getDrifted());
        verify(jobInfoManager, never()).add(any(JobInfoDTO.class));
        verify(scheduleManager, never()).createJob(any(JobCreateParam.class));
    }

    @Test
    @DisplayName("应该收敛为已存在当并发注册撞唯一键时")
    void should_converge_to_existed_when_duplicate_key() {
        when(jobInfoManager.getByJobKey("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(null);
        when(scheduleManager.exists("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(false);
        when(jobInfoManager.add(any(JobInfoDTO.class)))
            .thenThrow(new DuplicateKeyException("uk_job_info_name_group"));

        JobRegisterDTO result = jobService.registerIfAbsent(buildParam(null));

        assertFalse(result.getCreated());
        assertTrue(result.getExisted());
        verify(scheduleManager, never()).createJob(any(JobCreateParam.class));
    }

    @Test
    @DisplayName("应该先建后暂停当注解声明初始状态为暂停时")
    void should_create_then_pause_when_status_paused() {
        when(jobInfoManager.getByJobKey("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(null);
        when(scheduleManager.exists("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(false);
        when(jobInfoManager.add(any(JobInfoDTO.class))).thenReturn(true);

        JobRegisterDTO result = jobService.registerIfAbsent(buildParam("PAUSED"));

        assertTrue(result.getCreated());
        assertEquals(Trigger.TriggerState.PAUSED.name(), result.getStatus());
        ArgumentCaptor<JobInfoDTO> infoCaptor = ArgumentCaptor.forClass(JobInfoDTO.class);
        verify(jobInfoManager).add(infoCaptor.capture());
        assertEquals(Trigger.TriggerState.PAUSED.name(), infoCaptor.getValue().getStatus());
        verify(scheduleManager).pauseJob("orderSyncJob", JobScheduleManager.DEFAULT_GROUP);
    }

    @Test
    @DisplayName("应该回滚控制面与执行面当暂停动作失败时")
    void should_rollback_both_planes_when_pause_fail() {
        when(jobInfoManager.getByJobKey("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(null);
        // 第一次 exists 判定为不存在，回滚阶段再次判定时执行面已建好
        when(scheduleManager.exists("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(false, true);
        when(jobInfoManager.add(any(JobInfoDTO.class))).thenReturn(true);
        doThrow(JobErrorCode.JOB_SCHEDULER_ERROR.exception("pause failed"))
            .when(scheduleManager).pauseJob("orderSyncJob", JobScheduleManager.DEFAULT_GROUP);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> jobService.registerIfAbsent(buildParam("PAUSED")));

        assertEquals(JobErrorCode.JOB_SCHEDULER_ERROR.getCode(), exception.getErrorCode());
        verify(scheduleManager).deleteJob("orderSyncJob", JobScheduleManager.DEFAULT_GROUP);
        verify(jobInfoManager).removeByJobKey("orderSyncJob", JobScheduleManager.DEFAULT_GROUP);
    }

    @Test
    @DisplayName("应该只回滚控制面当Quartz创建阶段就失败时")
    void should_rollback_metadata_only_when_quartz_create_fail() {
        when(jobInfoManager.getByJobKey("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(null);
        when(scheduleManager.exists("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(false);
        when(jobInfoManager.add(any(JobInfoDTO.class))).thenReturn(true);
        doThrow(JobErrorCode.JOB_SCHEDULER_ERROR.exception("quartz down"))
            .when(scheduleManager).createJob(any(JobCreateParam.class));

        assertThrows(BusinessException.class, () -> jobService.registerIfAbsent(buildParam(null)));

        verify(scheduleManager, never()).deleteJob(anyString(), anyString());
        verify(jobInfoManager).removeByJobKey("orderSyncJob", JobScheduleManager.DEFAULT_GROUP);
    }

    @Test
    @DisplayName("应该抛出参数异常当任务名称为空时")
    void should_throw_param_error_when_job_name_blank() {
        ApiJobRegisterParam param = buildParam(null).setJobName(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> jobService.registerIfAbsent(param));

        assertEquals(JobErrorCode.JOB_PARAM_NAME_EMPTY.getCode(), exception.getErrorCode());
        verify(jobInfoManager, never()).add(any(JobInfoDTO.class));
    }

    @Test
    @DisplayName("应该抛出参数异常当处理器名称为空时")
    void should_throw_param_error_when_handler_name_blank() {
        ApiJobRegisterParam param = buildParam(null).setHandlerName(" ");

        BusinessException exception = assertThrows(BusinessException.class, () -> jobService.registerIfAbsent(param));

        assertEquals(JobErrorCode.JOB_PARAM_HANDLER_EMPTY.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("应该抛出参数异常当目标服务名为空时")
    void should_throw_param_error_when_service_name_blank() {
        ApiJobRegisterParam param = buildParam(null).setServiceName(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> jobService.registerIfAbsent(param));

        assertEquals(JobErrorCode.JOB_PARAM_SERVICE_EMPTY.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("应该收敛为已创建且不回滚当执行面已被并发建好时")
    void should_converge_when_execution_plane_already_exists() {
        when(jobInfoManager.getByJobKey("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(null);
        when(scheduleManager.exists("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(false);
        when(jobInfoManager.add(any(JobInfoDTO.class))).thenReturn(true);
        // 控制面落库之后、建触发器之前，调度中心启动对账按这行把执行面建好了
        doThrow(JobErrorCode.JOB_ALREADY_EXISTS.exception("任务已存在"))
            .when(scheduleManager).createJob(any(JobCreateParam.class));

        JobRegisterDTO result = jobService.registerIfAbsent(buildParam(null));

        assertTrue(result.getCreated());
        // 两面都有即一致状态：删掉不属于本次的触发器会让任务凭空消失，且客户端收到 409 后不再重试
        verify(scheduleManager, never()).deleteJob(anyString(), anyString());
        verify(jobInfoManager, never()).removeByJobKey(anyString(), anyString());
    }

    @Test
    @DisplayName("应该返回库中真实状态当并发注册撞唯一键时")
    void should_return_real_status_when_duplicate_key() {
        JobInfoDTO winner = new JobInfoDTO().setStatus(Trigger.TriggerState.NORMAL.name());
        // 首次查询不存在；撞键后回查胜出方
        when(jobInfoManager.getByJobKey("orderSyncJob", JobScheduleManager.DEFAULT_GROUP))
            .thenReturn(null, winner);
        when(scheduleManager.exists("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(false);
        when(jobInfoManager.add(any(JobInfoDTO.class))).thenThrow(new DuplicateKeyException("dup"));

        // 本进程按注解算出的期望状态是 PAUSED，但库里的胜出方是 NORMAL
        JobRegisterDTO result = jobService.registerIfAbsent(buildParam("PAUSED"));

        assertTrue(result.getExisted());
        assertEquals(Trigger.TriggerState.NORMAL.name(), result.getStatus());
    }

    @Test
    @DisplayName("应该保留控制面元数据当执行面回滚失败时")
    void should_keep_metadata_when_quartz_delete_fail() {
        when(jobInfoManager.getByJobKey("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(null);
        when(scheduleManager.exists("orderSyncJob", JobScheduleManager.DEFAULT_GROUP)).thenReturn(false, true);
        when(jobInfoManager.add(any(JobInfoDTO.class))).thenReturn(true);
        doThrow(JobErrorCode.JOB_SCHEDULER_ERROR.exception("pause failed"))
            .when(scheduleManager).pauseJob("orderSyncJob", JobScheduleManager.DEFAULT_GROUP);
        doThrow(JobErrorCode.JOB_SCHEDULER_ERROR.exception("quartz down"))
            .when(scheduleManager).deleteJob("orderSyncJob", JobScheduleManager.DEFAULT_GROUP);

        assertThrows(BusinessException.class, () -> jobService.registerIfAbsent(buildParam("PAUSED")));

        // 执行面没清干净时删控制面，会造出"Quartz 有 / DB 无"的漂移态，故必须保留该行交给启动对账
        verify(jobInfoManager, never()).removeByJobKey(anyString(), anyString());
    }

    private ApiJobRegisterParam buildParam(String status) {
        return new ApiJobRegisterParam()
            .setJobName("orderSyncJob")
            .setServiceName("carlos-order")
            .setHandlerName("orderSyncHandler")
            .setScheduleType(1)
            .setScheduleConf("0 0 2 * * ?")
            .setStatus(status);
    }

}
