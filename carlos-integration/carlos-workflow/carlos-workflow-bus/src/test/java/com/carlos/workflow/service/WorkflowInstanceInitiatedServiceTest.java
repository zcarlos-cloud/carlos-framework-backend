package com.carlos.workflow.service;

import com.carlos.core.pagination.Paging;
import com.carlos.workflow.api.pojo.param.ApiWorkflowInstanceActionParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowInstancePageParam;
import com.carlos.workflow.dto.WorkflowInstanceDTO;
import com.carlos.workflow.exception.WorkflowException;
import com.carlos.workflow.manager.WorkflowInstanceManager;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.dromara.warm.flow.core.service.ChartService;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 「我发起的」分页与按实例拿回（{@code taskBackByInsId}）的服务层测试。
 * </p>
 * <p>
 * 引擎 Bean 与 Manager 全部 mock：这里校验的是「谁去查」「拿谁的身份去拿回」这两件事——
 * 分页必须把当前登录用户交给 Manager（而不是让前端传发起人），拿回必须带上
 * {@code handler} / {@code message} / {@code permissionFlag}；真实 SQL 由
 * {@code com.carlos.workflow.manager.WorkflowInstanceManagerTest} 用 H2 覆盖。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowInstanceInitiatedServiceTest {

    private static final String CURRENT_USER = "1001";

    private final InsService engineInstanceService = mock(InsService.class);

    private final DefService engineDefinitionService = mock(DefService.class);

    private final ChartService engineChartService = mock(ChartService.class);

    private final TaskService engineTaskService = mock(TaskService.class);

    private final WorkflowInstanceManager instanceManager = mock(WorkflowInstanceManager.class);

    private final PermissionHandler permissionHandler = mock(PermissionHandler.class);

    private final WorkflowInstanceService service = new WorkflowInstanceService(engineInstanceService,
        engineDefinitionService, engineChartService);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "taskService", engineTaskService);
        ReflectionTestUtils.setField(service, "instanceManager", instanceManager);
        ReflectionTestUtils.setField(service, "permissionHandler", permissionHandler);
        when(permissionHandler.getHandler()).thenReturn(CURRENT_USER);
    }

    // ==================== 「我发起的」分页 ====================

    @Test
    void myPageHandsTheCurrentUserToTheManagerAndReturnsItsResult() {
        ApiWorkflowInstancePageParam param = new ApiWorkflowInstancePageParam();
        param.setCurrent(2);
        param.setSize(5);
        param.setFlowName("请假");
        param.setFlowStatus("1");
        param.setBusinessId("BIZ-1");

        Paging<WorkflowInstanceDTO> expected = new Paging<>();
        expected.setTotal(1);
        when(instanceManager.getMyPage(any(ApiWorkflowInstancePageParam.class), eq(CURRENT_USER)))
            .thenReturn(expected);

        assertThat(service.myPage(param)).isSameAs(expected);

        ArgumentCaptor<ApiWorkflowInstancePageParam> captor =
            ArgumentCaptor.forClass(ApiWorkflowInstancePageParam.class);
        verify(instanceManager).getMyPage(captor.capture(), eq(CURRENT_USER));
        assertThat(captor.getValue().getFlowName()).isEqualTo("请假");
        assertThat(captor.getValue().getFlowStatus()).isEqualTo("1");
        assertThat(captor.getValue().getBusinessId()).isEqualTo("BIZ-1");
        assertThat(captor.getValue().getCurrent()).isEqualTo(2);
        assertThat(captor.getValue().getSize()).isEqualTo(5);
    }

    @Test
    void myPageFailsLoudlyWhenTheInstanceManagerIsNotWired() {
        WorkflowInstanceService unwired = new WorkflowInstanceService(engineInstanceService,
            engineDefinitionService, engineChartService);

        assertThat(catchThrowable(() -> unwired.myPage(new ApiWorkflowInstancePageParam())))
            .as("Manager 缺失时直接报 50701，而不是静默返回空列表——空列表会被当成「我确实没有申请」")
            .isInstanceOf(WorkflowException.class)
            .satisfies(thrown -> assertThat(((WorkflowException) thrown).getErrorCode()).isEqualTo("50701"));
    }

    // ==================== 拿回 ====================

    @Test
    void takeBackRejectsNullParam() {
        assertErrorCode("10701", () -> service.takeBack(null));
    }

    @Test
    void takeBackRejectsNullInstanceId() {
        assertErrorCode("10701", () -> service.takeBack(new ApiWorkflowInstanceActionParam()));
    }

    @Test
    void takeBackRejectsMissingInstanceBeforeTouchingTheEngine() {
        when(engineInstanceService.getById(404L)).thenReturn(null);

        ApiWorkflowInstanceActionParam param = new ApiWorkflowInstanceActionParam();
        param.setInstanceId(404L);

        assertErrorCode("20702", () -> service.takeBack(param));
        verify(engineTaskService, times(0)).taskBackByInsId(any(), any());
    }

    @Test
    void takeBackRunsInTheNameOfTheCurrentUserAndCarriesThePermissionFlags() {
        Instance instance = instance(303L);
        when(engineInstanceService.getById(303L)).thenReturn(instance);
        when(permissionHandler.permissions()).thenReturn(List.of(CURRENT_USER));
        Instance takenBack = instance(303L);
        when(engineTaskService.taskBackByInsId(eq(303L), any(FlowParams.class))).thenReturn(takenBack);

        ApiWorkflowInstanceActionParam param = new ApiWorkflowInstanceActionParam();
        param.setInstanceId(303L);
        param.setMessage("发起人拿回修改");

        assertThat(service.takeBack(param).getInstanceId()).isEqualTo(303L);

        ArgumentCaptor<FlowParams> captor = ArgumentCaptor.forClass(FlowParams.class);
        verify(engineTaskService).taskBackByInsId(eq(303L), captor.capture());
        assertThat(captor.getValue().getHandler())
            .as("引擎按 handler 定位「我办过的那一条」历史任务，必须由服务端给出当前用户")
            .isEqualTo(CURRENT_USER);
        assertThat(captor.getValue().getMessage()).isEqualTo("发起人拿回修改");
        assertThat(captor.getValue().getPermissionFlag()).containsExactly(CURRENT_USER);
    }

    @Test
    void takeBackTranslatesEngineRefusalToOperationNotAllowed() {
        Instance instance = instance(303L);
        when(engineInstanceService.getById(303L)).thenReturn(instance);
        when(engineTaskService.taskBackByInsId(eq(303L), any(FlowParams.class)))
            .thenThrow(new FlowException("未找到办理过的任务!"));

        ApiWorkflowInstanceActionParam param = new ApiWorkflowInstanceActionParam();
        param.setInstanceId(303L);

        assertErrorCode("20704", () -> service.takeBack(param));
    }

    @Test
    void takeBackRejectsWhenTheEngineReturnsNothing() {
        Instance instance = instance(303L);
        when(engineInstanceService.getById(303L)).thenReturn(instance);
        when(engineTaskService.taskBackByInsId(eq(303L), any(FlowParams.class))).thenReturn(null);

        ApiWorkflowInstanceActionParam param = new ApiWorkflowInstanceActionParam();
        param.setInstanceId(303L);

        assertErrorCode("20702", () -> service.takeBack(param));
    }

    // ==================== 夹具 ====================

    private static Instance instance(Long instanceId) {
        Instance instance = mock(Instance.class);
        when(instance.getId()).thenReturn(instanceId);
        return instance;
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
