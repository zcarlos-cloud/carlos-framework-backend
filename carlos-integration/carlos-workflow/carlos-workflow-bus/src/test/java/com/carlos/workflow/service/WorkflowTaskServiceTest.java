package com.carlos.workflow.service;

import com.carlos.core.pagination.Paging;
import com.carlos.workflow.api.enums.WorkflowActionEnum;
import com.carlos.workflow.api.pojo.param.ApiWorkflowCandidatePageParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowHandleParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowTaskPageParam;
import com.carlos.workflow.dto.WorkflowHandleResultDTO;
import com.carlos.workflow.dto.WorkflowHisTaskDTO;
import com.carlos.workflow.dto.WorkflowTaskDTO;
import com.carlos.workflow.exception.WorkflowException;
import com.carlos.workflow.manager.WorkflowOrgUserManager;
import com.carlos.workflow.manager.WorkflowTaskManager;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.dromara.warm.flow.core.service.HisTaskService;
import org.dromara.warm.flow.core.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 待办任务服务的动作分派与分页查询测试：引擎 Bean 与 Manager 全部 mock，只校验
 * 「按 action 分派到哪个引擎方法、传了什么参数」，以及列表查询是否原样下沉给 Manager。
 * </p>
 * <p>
 * 真实 SQL 由 {@code com.carlos.workflow.manager.WorkflowTaskManagerTest} 在 H2 上覆盖。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowTaskServiceTest {

    private static final String CURRENT_USER = "1001";

    private final TaskService engineTaskService = mock(TaskService.class);

    private final HisTaskService engineHisTaskService = mock(HisTaskService.class);

    private final WorkflowTaskManager taskManager = mock(WorkflowTaskManager.class);

    private final WorkflowOrgUserManager orgUserManager = mock(WorkflowOrgUserManager.class);

    private final PermissionHandler permissionHandler = mock(PermissionHandler.class);

    private final WorkflowTaskService service =
        new WorkflowTaskService(engineTaskService, engineHisTaskService);

    @BeforeEach
    void setUp() {
        // config 包不在本批次改动范围内，服务的新依赖用字段注入补充，单元测试同样用反射注入替身
        ReflectionTestUtils.setField(service, "taskManager", taskManager);
        ReflectionTestUtils.setField(service, "orgUserManager", orgUserManager);
        ReflectionTestUtils.setField(service, "permissionHandler", permissionHandler);
        when(permissionHandler.getHandler()).thenReturn(CURRENT_USER);
        when(permissionHandler.permissions()).thenReturn(List.of(CURRENT_USER));
    }

    @Test
    void handleDispatchesRejectLastToEngineRejectLast() {
        Instance instance = mock(Instance.class);
        when(instance.getId()).thenReturn(303L);
        when(engineTaskService.rejectLast(eq(101L), any(FlowParams.class))).thenReturn(instance);

        WorkflowHandleResultDTO result = service.handle(handleParam(WorkflowActionEnum.REJECT_LAST));

        assertThat(result.getAction()).isEqualTo(WorkflowActionEnum.REJECT_LAST);
        assertThat(result.getOperated()).isTrue();
        assertThat(result.getInstance()).isNotNull();
        assertThat(result.getInstance().getInstanceId()).isEqualTo(303L);

        FlowParams flowParams = captured(101L, "rejectLast");
        assertThat(flowParams.getHandler()).isEqualTo(CURRENT_USER);
        assertThat(flowParams.getPermissionFlag()).containsExactly(CURRENT_USER);
        assertThat(flowParams.getMessage()).isEqualTo("同意");
        // 退回上一节点不需要目标节点：引擎自己按历史记录回溯
        assertThat(flowParams.getNodeCode()).isNull();
        assertThat(flowParams.getAddHandlers()).isNull();
    }

    @Test
    void handleTranslatesRejectLastRefusalToOperationNotAllowed() {
        when(engineTaskService.rejectLast(eq(101L), any(FlowParams.class)))
            .thenThrow(new FlowException("没有可退回的任务!"));

        assertErrorCode("20704", () -> service.handle(handleParam(WorkflowActionEnum.REJECT_LAST)));
    }

    @Test
    void handleDispatchesTakeBackToEngineTaskBackWithCurrentUserAsHandler() {
        Instance instance = mock(Instance.class);
        when(engineTaskService.taskBack(eq(101L), any(FlowParams.class))).thenReturn(instance);

        // 故意传入伪造的 handler：拿回必须用当前登录用户去定位「我上一次办理的任务」
        ApiWorkflowHandleParam param = handleParam(WorkflowActionEnum.TAKE_BACK);
        param.setHandler("forged");
        WorkflowHandleResultDTO result = service.handle(param);

        assertThat(result.getAction()).isEqualTo(WorkflowActionEnum.TAKE_BACK);
        assertThat(result.getOperated()).isTrue();
        assertThat(captured(101L, "taskBack").getHandler()).isEqualTo(CURRENT_USER);
    }

    @Test
    void handleRejectsTakeBackWhenNoCurrentUserCanBeResolved() {
        when(permissionHandler.getHandler()).thenReturn(null);

        assertErrorCode("50701", () -> service.handle(handleParam(WorkflowActionEnum.TAKE_BACK)));
    }

    @Test
    void handleStillDispatchesFlowSkipAndHandlerAdjustActions() {
        when(engineTaskService.skip(eq(101L), any(FlowParams.class))).thenReturn(mock(Instance.class));
        service.handle(handleParam(WorkflowActionEnum.PASS));
        assertThat(captured(101L, "skip").getSkipType()).isEqualTo(SkipType.PASS.getKey());

        when(engineTaskService.transfer(eq(101L), any(FlowParams.class))).thenReturn(true);
        ApiWorkflowHandleParam transfer = handleParam(WorkflowActionEnum.TRANSFER);
        transfer.setTargetHandlers(List.of("2002"));
        assertThat(service.handle(transfer).getOperated()).isTrue();
        assertThat(captured(101L, "transfer").getAddHandlers()).containsExactly("2002");
    }

    @Test
    void todoPageHandsCurrentPermissionsToTheManager() {
        ApiWorkflowTaskPageParam param = new ApiWorkflowTaskPageParam();
        param.setNodeName("经理");
        Paging<WorkflowTaskDTO> expected = new Paging<>();
        when(taskManager.getTodoPage(any(), any())).thenReturn(expected);

        assertThat(service.todoPage(param)).isSameAs(expected);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(taskManager).getTodoPage(eq(param), captor.capture());
        assertThat(captor.getValue()).containsExactly(CURRENT_USER);
    }

    @Test
    void todoPageFailsLoudlyWhenNoPermissionHandlerIsWired() {
        ReflectionTestUtils.setField(service, "permissionHandler", null);

        assertErrorCode("50701", () -> service.todoPage(new ApiWorkflowTaskPageParam()));
    }

    @Test
    void donePageQueriesOnlyTheCurrentUser() {
        Paging<WorkflowHisTaskDTO> expected = new Paging<>();
        when(taskManager.getDonePage(any(), any())).thenReturn(expected);

        assertThat(service.donePage(new ApiWorkflowTaskPageParam())).isSameAs(expected);

        verify(taskManager).getDonePage(any(ApiWorkflowTaskPageParam.class), eq(CURRENT_USER));
    }

    @Test
    void candidatesForReductionOnlyIncludeTheTasksCurrentHandlers() {
        when(engineTaskService.getById(101L)).thenReturn(mock(Task.class));
        when(taskManager.listTaskHandlerIds(101L)).thenReturn(List.of("1001", "1002"));
        when(orgUserManager.getCandidatePage(any(), any(), any(), anyInt(), anyInt())).thenReturn(new Paging<>());

        ApiWorkflowCandidatePageParam param = new ApiWorkflowCandidatePageParam();
        param.setAction(WorkflowActionEnum.REDUCTION_SIGNATURE);
        param.setDeptId(12L);
        service.candidates(101L, param);

        verify(orgUserManager).getCandidatePage(eq(12L), eq(List.of("1001", "1002")), isNull(), eq(1), eq(10));
    }

    @Test
    void candidatesForOtherActionsExcludeTheTasksCurrentHandlers() {
        when(engineTaskService.getById(101L)).thenReturn(mock(Task.class));
        when(taskManager.listTaskHandlerIds(101L)).thenReturn(List.of("1001"));
        when(orgUserManager.getCandidatePage(any(), any(), any(), anyInt(), anyInt())).thenReturn(new Paging<>());

        ApiWorkflowCandidatePageParam param = new ApiWorkflowCandidatePageParam();
        param.setAction(WorkflowActionEnum.ADD_SIGNATURE);
        param.setCurrent(2);
        param.setSize(5);
        service.candidates(101L, param);

        verify(orgUserManager).getCandidatePage(isNull(), isNull(), eq(List.of("1001")), eq(2), eq(5));
    }

    @Test
    void candidatesForNullActionFallBackToExcludingHandlers() {
        when(engineTaskService.getById(101L)).thenReturn(mock(Task.class));
        when(taskManager.listTaskHandlerIds(101L)).thenReturn(List.of("1001"));
        when(orgUserManager.getCandidatePage(any(), any(), any(), anyInt(), anyInt())).thenReturn(new Paging<>());

        service.candidates(101L, null);

        verify(orgUserManager).getCandidatePage(isNull(), isNull(), eq(List.of("1001")), eq(1), eq(10));
    }

    @Test
    void candidatesRejectMissingTask() {
        when(engineTaskService.getById(404L)).thenReturn(null);

        assertErrorCode("20703", () -> service.candidates(404L, new ApiWorkflowCandidatePageParam()));
    }

    @Test
    void candidatesRejectNullTaskId() {
        assertErrorCode("10701", () -> service.candidates(null, new ApiWorkflowCandidatePageParam()));
    }

    /**
     * 抓取引擎收到的流转参数
     *
     * @param taskId     任务id
     * @param methodName 引擎方法名
     * @return FlowParams
     */
    private FlowParams captured(Long taskId, String methodName) {
        ArgumentCaptor<FlowParams> captor = ArgumentCaptor.forClass(FlowParams.class);
        switch (methodName) {
            case "skip" -> verify(engineTaskService).skip(eq(taskId), captor.capture());
            case "rejectLast" -> verify(engineTaskService).rejectLast(eq(taskId), captor.capture());
            case "taskBack" -> verify(engineTaskService).taskBack(eq(taskId), captor.capture());
            case "transfer" -> verify(engineTaskService).transfer(eq(taskId), captor.capture());
            default -> throw new IllegalArgumentException("未知的引擎方法: " + methodName);
        }
        return captor.getValue();
    }

    private ApiWorkflowHandleParam handleParam(WorkflowActionEnum action) {
        ApiWorkflowHandleParam param = new ApiWorkflowHandleParam();
        param.setTaskId(101L);
        param.setAction(action);
        param.setMessage("同意");
        return param;
    }

    private static void assertErrorCode(String expectedCode, ThrowingRunnable runnable) {
        Throwable thrown = catchThrowable(runnable::run);
        assertThat(thrown).isInstanceOf(WorkflowException.class);
        assertThat(((WorkflowException) thrown).getErrorCode()).isEqualTo(expectedCode);
    }

    /**
     * 断言用函数式接口
     */
    @FunctionalInterface
    private interface ThrowingRunnable {

        void run();
    }

}
