package com.carlos.workflow.service;

import com.carlos.workflow.api.pojo.param.ApiWorkflowInstanceActionParam;
import com.carlos.workflow.dto.WorkflowHisTaskDTO;
import com.carlos.workflow.dto.WorkflowNodeDTO;
import com.carlos.workflow.exception.WorkflowException;
import com.carlos.workflow.manager.WorkflowOrgUserManager;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.HisTask;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.dromara.warm.flow.core.orm.agent.WarmQuery;
import org.dromara.warm.flow.core.service.ChartService;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.HisTaskService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.NodeService;
import org.dromara.warm.flow.core.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
 * 流程实例服务的实例级动作与两个只读端点测试：审批记录（某实例的全部历史任务）、
 * 可选跳转节点（自由跳转 / 驳回到指定节点）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowInstanceServiceTest {

    private static final String CURRENT_USER = "1001";

    private final InsService engineInstanceService = mock(InsService.class);

    private final DefService engineDefinitionService = mock(DefService.class);

    private final ChartService engineChartService = mock(ChartService.class);

    private final TaskService engineTaskService = mock(TaskService.class);

    private final HisTaskService engineHisTaskService = mock(HisTaskService.class);

    private final NodeService engineNodeService = mock(NodeService.class);

    private final WorkflowOrgUserManager orgUserManager = mock(WorkflowOrgUserManager.class);

    private final PermissionHandler permissionHandler = mock(PermissionHandler.class);

    private final WorkflowInstanceService service = new WorkflowInstanceService(engineInstanceService,
        engineDefinitionService, engineChartService);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "taskService", engineTaskService);
        ReflectionTestUtils.setField(service, "hisTaskService", engineHisTaskService);
        ReflectionTestUtils.setField(service, "nodeService", engineNodeService);
        ReflectionTestUtils.setField(service, "orgUserManager", orgUserManager);
        ReflectionTestUtils.setField(service, "permissionHandler", permissionHandler);
        when(permissionHandler.getHandler()).thenReturn(CURRENT_USER);
    }

    @Test
    void listHisTaskRejectsMissingInstance() {
        when(engineInstanceService.getById(303L)).thenReturn(null);

        assertErrorCode("20702", () -> service.listHisTask(303L));
    }

    @Test
    void listHisTaskOrdersByHisTaskIdDescAndResolvesNamesInOneBatch() {
        Instance instance = instance(303L, 7L, "nodeB");
        when(engineInstanceService.getById(303L)).thenReturn(instance);

        HisTask first = hisTask(201L, "1001", "1002,1003", "发起");
        HisTask second = hisTask(202L, "1001", null, "经理审批");
        WarmQuery<HisTask> query = warmQuery(List.of(second, first));
        when(engineHisTaskService.orderById()).thenReturn(query);

        Map<String, String> names = Map.of("1001", "张三", "1002", "李四", "1003", "王五");
        when(orgUserManager.mapNames(any())).thenReturn(names);

        List<WorkflowHisTaskDTO> result = service.listHisTask(303L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getHisTaskId()).isEqualTo(202L);
        assertThat(result.get(0).getApprover()).isEqualTo("张三");
        assertThat(result.get(1).getCollaborator()).isEqualTo("李四,王五");

        // 姓名必须一次性批量解析：逐行回查会退化成 N+1
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(orgUserManager, times(1)).mapNames(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder("1001", "1002", "1003");
    }

    @Test
    void listHisTaskKeepsRawIdsWhenOrgCannotResolveNames() {
        Instance instance = instance(303L, 7L, "nodeB");
        when(engineInstanceService.getById(303L)).thenReturn(instance);
        WarmQuery<HisTask> query = warmQuery(List.of(hisTask(201L, "9999", null, "发起")));
        when(engineHisTaskService.orderById()).thenReturn(query);
        when(orgUserManager.mapNames(any())).thenReturn(Map.of());

        assertThat(service.listHisTask(303L).get(0).getApprover())
            .as("org 解析不到姓名时回退为原始标识，避免列表整列为空")
            .isEqualTo("9999");
    }

    @Test
    void listAnyNodeKeepsOnlyVisitedPreviousNodesAndEverySuffixNode() {
        Instance instance = instance(303L, 7L, "nodeB");
        when(engineInstanceService.getById(303L)).thenReturn(instance);

        Node currentNode = node("nodeB", "经理审批", NodeType.BETWEEN);
        when(engineNodeService.getByDefIdAndNodeCode(7L, "nodeB")).thenReturn(currentNode);
        Node startNode = node("start", "发起", NodeType.START);
        when(engineNodeService.getStartNode(7L)).thenReturn(startNode);

        // 结构上的前置节点：nodeA 本实例走过，nodeX 没走到（并行分支）
        Node visitedPrevious = node("nodeA", "发起人确认", NodeType.BETWEEN);
        Node unvisitedPrevious = node("nodeX", "另一分支", NodeType.BETWEEN);
        when(engineNodeService.previousNodeList(7L, "nodeB"))
            .thenReturn(List.of(visitedPrevious, unvisitedPrevious));

        // 结构上的后继节点：nodeC 可跳，nodeG 是网关（引擎会拒绝），start 是开始节点（不可退回）
        Node suffix = node("nodeC", "财务审批", NodeType.BETWEEN);
        Node gateway = node("nodeG", "网关", NodeType.PARALLEL);
        when(engineNodeService.suffixNodeList(7L, "nodeB")).thenReturn(List.of(suffix, gateway, startNode));

        // 本实例走过 nodeA（历史上从 nodeA 跳到 nodeB）
        HisTask hisTask = hisTaskWithTarget("nodeA", "nodeB");
        when(engineHisTaskService.getByInsId(303L)).thenReturn(List.of(hisTask));

        List<WorkflowNodeDTO> result = service.listAnyNode(303L);

        assertThat(result).extracting(WorkflowNodeDTO::getNodeCode).containsExactly("nodeA", "nodeC");
        assertThat(result).extracting(WorkflowNodeDTO::getNodeName).containsExactly("发起人确认", "财务审批");
    }

    @Test
    void listAnyNodeAlsoHonoursTheTargetNodeCodeOfVisitedNodes() {
        Instance instance = instance(303L, 7L, "nodeB");
        when(engineInstanceService.getById(303L)).thenReturn(instance);
        Node currentNode = node("nodeB", "经理审批", NodeType.BETWEEN);
        when(engineNodeService.getByDefIdAndNodeCode(7L, "nodeB")).thenReturn(currentNode);
        Node startNode = node("start", "发起", NodeType.START);
        when(engineNodeService.getStartNode(7L)).thenReturn(startNode);
        Node visitedPrevious = node("nodeA", "发起人确认", NodeType.BETWEEN);
        when(engineNodeService.previousNodeList(7L, "nodeB")).thenReturn(List.of(visitedPrevious));
        when(engineNodeService.suffixNodeList(7L, "nodeB")).thenReturn(List.of());
        // 历史记录的 target_node_code 是逗号分隔的多个节点（并行网关场景）
        HisTask hisTask = hisTaskWithTarget("nodeA", "nodeB,nodeA");
        when(engineHisTaskService.getByInsId(303L)).thenReturn(List.of(hisTask));

        assertThat(service.listAnyNode(303L)).extracting(WorkflowNodeDTO::getNodeCode).containsExactly("nodeA");
    }

    @Test
    void listAnyNodeRejectsInstanceWhoseCurrentNodeNoLongerExists() {
        Instance instance = instance(303L, 7L, "nodeB");
        when(engineInstanceService.getById(303L)).thenReturn(instance);
        when(engineNodeService.getByDefIdAndNodeCode(7L, "nodeB")).thenReturn(null);

        assertErrorCode("50701", () -> service.listAnyNode(303L));
    }

    @Test
    void revokeRunsInTheNameOfTheCurrentUser() {
        Instance instance = instance(303L, 7L, "nodeB");
        when(engineInstanceService.getById(303L)).thenReturn(instance);
        when(engineTaskService.revoke(eq(303L), any(FlowParams.class))).thenReturn(instance);

        ApiWorkflowInstanceActionParam param = new ApiWorkflowInstanceActionParam();
        param.setInstanceId(303L);
        param.setMessage("撤销申请");

        assertThat(service.revoke(param).getInstanceId()).isEqualTo(303L);

        ArgumentCaptor<FlowParams> captor = ArgumentCaptor.forClass(FlowParams.class);
        verify(engineTaskService).revoke(eq(303L), captor.capture());
        assertThat(captor.getValue().getHandler()).isEqualTo(CURRENT_USER);
        assertThat(captor.getValue().getMessage()).isEqualTo("撤销申请");
    }

    @Test
    void revokeRejectsMissingInstanceBeforeTouchingTheEngine() {
        when(engineInstanceService.getById(404L)).thenReturn(null);

        ApiWorkflowInstanceActionParam param = new ApiWorkflowInstanceActionParam();
        param.setInstanceId(404L);

        assertErrorCode("20702", () -> service.revoke(param));
        verify(engineTaskService, times(0)).revoke(any(), any());
    }

    @Test
    void revokeRejectsNullInstanceId() {
        assertErrorCode("10701", () -> service.revoke(new ApiWorkflowInstanceActionParam()));
    }

    @Test
    void revokeTranslatesEngineRefusalToOperationNotAllowed() {
        Instance instance = instance(303L, 7L, "nodeB");
        when(engineInstanceService.getById(303L)).thenReturn(instance);
        when(engineTaskService.revoke(eq(303L), any(FlowParams.class)))
            .thenThrow(new FlowException("流程已结束，不可撤销!"));

        ApiWorkflowInstanceActionParam param = new ApiWorkflowInstanceActionParam();
        param.setInstanceId(303L);

        assertErrorCode("20704", () -> service.revoke(param));
    }

    @Test
    void terminateEndsTheInstanceAndReturnsIt() {
        Instance instance = instance(303L, 7L, "nodeB");
        when(engineInstanceService.getById(303L)).thenReturn(instance);
        Instance terminated = instance(303L, 7L, "end");
        when(engineTaskService.terminationByInsId(eq(303L), any(FlowParams.class))).thenReturn(terminated);

        ApiWorkflowInstanceActionParam param = new ApiWorkflowInstanceActionParam();
        param.setInstanceId(303L);
        param.setMessage("终止");

        assertThat(service.terminate(param).getNodeCode()).isEqualTo("end");
    }

    @Test
    void terminateRejectsWhenEngineReturnsNothing() {
        Instance instance = instance(303L, 7L, "nodeB");
        when(engineInstanceService.getById(303L)).thenReturn(instance);
        when(engineTaskService.terminationByInsId(eq(303L), any(FlowParams.class))).thenReturn(null);

        ApiWorkflowInstanceActionParam param = new ApiWorkflowInstanceActionParam();
        param.setInstanceId(303L);

        assertErrorCode("20702", () -> service.terminate(param));
    }

    @Test
    void suspendAndActivateCheckInstanceFirstAndDelegateToTheEngine() {
        Instance instance = instance(303L, 7L, "nodeB");
        when(engineInstanceService.getById(303L)).thenReturn(instance);
        when(engineInstanceService.unActive(303L)).thenReturn(true);
        when(engineInstanceService.active(303L)).thenReturn(true);

        assertThat(service.suspend(303L)).isTrue();
        assertThat(service.activate(303L)).isTrue();

        verify(engineInstanceService).unActive(303L);
        verify(engineInstanceService).active(303L);
    }

    @Test
    void suspendTranslatesEngineRefusalToOperationNotAllowed() {
        Instance instance = instance(303L, 7L, "nodeB");
        when(engineInstanceService.getById(303L)).thenReturn(instance);
        when(engineInstanceService.unActive(303L)).thenThrow(new FlowException("流程实例已挂起!"));

        assertErrorCode("20704", () -> service.suspend(303L));
    }

    @Test
    void suspendRejectsMissingInstance() {
        when(engineInstanceService.getById(404L)).thenReturn(null);

        assertErrorCode("20702", () -> service.suspend(404L));
    }

    private Instance instance(Long instanceId, Long definitionId, String nodeCode) {
        Instance instance = mock(Instance.class);
        when(instance.getId()).thenReturn(instanceId);
        when(instance.getDefinitionId()).thenReturn(definitionId);
        when(instance.getNodeCode()).thenReturn(nodeCode);
        return instance;
    }

    private Node node(String nodeCode, String nodeName, NodeType nodeType) {
        Node node = mock(Node.class);
        when(node.getNodeCode()).thenReturn(nodeCode);
        when(node.getNodeName()).thenReturn(nodeName);
        when(node.getNodeType()).thenReturn(nodeType.getKey());
        return node;
    }

    private HisTask hisTask(Long id, String approver, String collaborator, String nodeName) {
        HisTask hisTask = mock(HisTask.class);
        when(hisTask.getId()).thenReturn(id);
        when(hisTask.getApprover()).thenReturn(approver);
        when(hisTask.getCollaborator()).thenReturn(collaborator);
        when(hisTask.getNodeName()).thenReturn(nodeName);
        return hisTask;
    }

    private HisTask hisTaskWithTarget(String nodeCode, String targetNodeCode) {
        HisTask hisTask = mock(HisTask.class);
        when(hisTask.getNodeCode()).thenReturn(nodeCode);
        when(hisTask.getTargetNodeCode()).thenReturn(targetNodeCode);
        return hisTask;
    }

    private WarmQuery<HisTask> warmQuery(List<HisTask> hisTasks) {
        @SuppressWarnings("unchecked")
        WarmQuery<HisTask> query = mock(WarmQuery.class);
        when(query.desc()).thenReturn(query);
        when(query.list(any(HisTask.class))).thenReturn(hisTasks);
        return query;
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
