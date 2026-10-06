package com.carlos.workflow.service;

import com.carlos.workflow.api.enums.WorkflowActionEnum;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionSaveParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowHandleParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowStartParam;
import com.carlos.workflow.dto.WorkflowChartDTO;
import com.carlos.workflow.dto.WorkflowHandleResultDTO;
import com.carlos.workflow.dto.WorkflowInstanceDTO;
import com.carlos.workflow.dto.WorkflowTaskDTO;
import com.carlos.workflow.exception.WorkflowException;
import com.carlos.workflow.manager.WorkflowDefinitionManager;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.dto.FlowDto;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Form;
import org.dromara.warm.flow.core.entity.HisTask;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.service.ChartService;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.HisTaskService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.plugin.json.JsonConvertJackson3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 服务层错误码翻译测试：引擎 Bean 全部 mock，校验引擎异常与缺失数据被翻译成预期的 WorkflowErrorCode。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowServiceErrorCodeTest {

    /**
     * 引擎在启动时通过 SPI 把 JsonConvert 注入 FlowEngine 的静态字段，测试上下文不启动引擎，此处手工注入
     */
    @BeforeAll
    static void initJsonConvert() {
        FlowEngine.jsonConvert = new JsonConvertJackson3();
    }

    private final TaskService engineTaskService = mock(TaskService.class);

    private final HisTaskService engineHisTaskService = mock(HisTaskService.class);

    private final InsService engineInstanceService = mock(InsService.class);

    private final DefService engineDefinitionService = mock(DefService.class);

    private final ChartService engineChartService = mock(ChartService.class);

    private final WorkflowTaskService taskService =
        new WorkflowTaskService(engineTaskService, engineHisTaskService);

    private final WorkflowInstanceService instanceService =
        new WorkflowInstanceService(engineInstanceService, engineDefinitionService, engineChartService);

    private final WorkflowDefinitionManager definitionManager = mock(WorkflowDefinitionManager.class);

    private final WorkflowDefinitionService definitionService =
        new WorkflowDefinitionService(engineDefinitionService, definitionManager);

    @Test
    void loadRejectsNullTaskIdWithParamInvalid() {
        assertErrorCode("10701", () -> taskService.load(null));
    }

    @Test
    void loadRejectsMissingTaskWithTaskNotFound() {
        when(engineTaskService.getById(404L)).thenReturn(null);

        assertErrorCode("20703", () -> taskService.load(404L));
    }

    @Test
    void loadTranslatesEngineFailureToOperationNotAllowed() {
        when(engineTaskService.getById(101L)).thenReturn(mock(Task.class));
        when(engineTaskService.load(eq(101L), any(FlowParams.class))).thenThrow(new FlowException("流程已完成!"));

        assertErrorCode("20704", () -> taskService.load(101L));
    }

    @Test
    void loadMapsTaskAndFormData() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(101L);
        when(task.getInstanceId()).thenReturn(303L);
        when(task.getNodeCode()).thenReturn("node1");
        when(task.getNodeName()).thenReturn("经理审批");
        when(task.getPermissionList()).thenReturn(List.of("user:1"));
        when(engineTaskService.getById(101L)).thenReturn(task);

        Form form = mock(Form.class);
        when(form.getId()).thenReturn(9L);
        when(form.getFormCode()).thenReturn("leaveForm");
        FlowDto flowDto = new FlowDto();
        flowDto.setForm(form);
        flowDto.setData(Map.of("days", 3));
        when(engineTaskService.load(eq(101L), any(FlowParams.class))).thenReturn(flowDto);

        WorkflowTaskDTO dto = taskService.load(101L);

        assertThat(dto.getTaskId()).isEqualTo(101L);
        assertThat(dto.getInstanceId()).isEqualTo(303L);
        assertThat(dto.getNodeName()).isEqualTo("经理审批");
        assertThat(dto.getPermissionList()).containsExactly("user:1");
        assertThat(dto.getForm()).isNotNull();
        assertThat(dto.getForm().getFormCode()).isEqualTo("leaveForm");
        assertThat(dto.getFormData()).isEqualTo(Map.of("days", 3));
    }

    @Test
    void loadHistoryRejectsMissingHisTaskWithTaskNotFound() {
        when(engineHisTaskService.getById(404L)).thenReturn(null);

        assertErrorCode("20703", () -> taskService.loadHistory(404L));
    }

    @Test
    void loadHistoryTranslatesEngineFailureToOperationNotAllowed() {
        when(engineHisTaskService.getById(202L)).thenReturn(mock(HisTask.class));
        when(engineTaskService.hisLoad(eq(202L), any(FlowParams.class))).thenThrow(new FlowException("当前流程节点丢失!"));

        assertErrorCode("20704", () -> taskService.loadHistory(202L));
    }

    @Test
    void handleRejectsMissingActionWithActionUnsupported() {
        ApiWorkflowHandleParam param = new ApiWorkflowHandleParam();
        param.setTaskId(101L);

        assertErrorCode("10702", () -> taskService.handle(param));
    }

    @Test
    void handleRejectsTransferWithoutTargetHandler() {
        ApiWorkflowHandleParam param = new ApiWorkflowHandleParam();
        param.setTaskId(101L);
        param.setAction(WorkflowActionEnum.TRANSFER);

        assertErrorCode("10704", () -> taskService.handle(param));
    }

    @Test
    void handleRejectsFreeJumpWithoutNodeCode() {
        ApiWorkflowHandleParam param = new ApiWorkflowHandleParam();
        param.setTaskId(101L);
        param.setAction(WorkflowActionEnum.FREE_JUMP);

        assertErrorCode("10701", () -> taskService.handle(param));
    }

    @Test
    void handleTranslatesEngineRefusalToOperationNotAllowed() {
        when(engineTaskService.skip(eq(101L), any(FlowParams.class)))
            .thenThrow(new FlowException("无法跳转到该节点,请检查当前用户是否有权限!"));

        assertErrorCode("20704", () -> taskService.handle(handleParam(WorkflowActionEnum.PASS)));
    }

    @Test
    void handlePassSkipsWithPassTypeAndReturnsInstance() {
        Instance instance = mock(Instance.class);
        when(instance.getId()).thenReturn(303L);
        when(instance.getNodeCode()).thenReturn("node2");
        when(engineTaskService.skip(eq(101L), any(FlowParams.class))).thenReturn(instance);

        WorkflowHandleResultDTO result = taskService.handle(handleParam(WorkflowActionEnum.PASS));

        assertThat(result.getAction()).isEqualTo(WorkflowActionEnum.PASS);
        assertThat(result.getOperated()).isTrue();
        assertThat(result.getInstance()).isNotNull();
        assertThat(result.getInstance().getInstanceId()).isEqualTo(303L);

        FlowParams flowParams = capturedSkipParams(101L);
        assertThat(flowParams.getSkipType()).isEqualTo(SkipType.PASS.getKey());
        assertThat(flowParams.getHandler()).isEqualTo("user:1");
        assertThat(flowParams.getAddHandlers()).isNull();
        assertThat(flowParams.getNodeCode()).isNull();
    }

    @Test
    void handleRejectSkipsWithRejectType() {
        when(engineTaskService.skip(eq(101L), any(FlowParams.class))).thenReturn(mock(Instance.class));

        ApiWorkflowHandleParam param = handleParam(WorkflowActionEnum.REJECT);
        param.setNodeCode("node1");
        taskService.handle(param);

        FlowParams flowParams = capturedSkipParams(101L);
        assertThat(flowParams.getSkipType()).isEqualTo(SkipType.REJECT.getKey());
        assertThat(flowParams.getNodeCode()).isEqualTo("node1");
    }

    @Test
    void handleAddSignatureCallsAddSignatureWithAddHandlers() {
        when(engineTaskService.addSignature(eq(101L), any(FlowParams.class))).thenReturn(true);

        ApiWorkflowHandleParam param = handleParam(WorkflowActionEnum.ADD_SIGNATURE);
        param.setTargetHandlers(List.of("user:2", "user:3"));
        WorkflowHandleResultDTO result = taskService.handle(param);

        assertThat(result.getOperated()).isTrue();
        assertThat(result.getInstance()).isNull();
        ArgumentCaptor<FlowParams> captor = ArgumentCaptor.forClass(FlowParams.class);
        verify(engineTaskService).addSignature(eq(101L), captor.capture());
        assertThat(captor.getValue().getAddHandlers()).containsExactly("user:2", "user:3");
        assertThat(captor.getValue().getReductionHandlers()).isNull();
    }

    @Test
    void handleReductionSignatureCallsReductionSignatureWithReductionHandlers() {
        when(engineTaskService.reductionSignature(eq(101L), any(FlowParams.class))).thenReturn(true);

        ApiWorkflowHandleParam param = handleParam(WorkflowActionEnum.REDUCTION_SIGNATURE);
        param.setTargetHandlers(List.of("user:2"));
        taskService.handle(param);

        ArgumentCaptor<FlowParams> captor = ArgumentCaptor.forClass(FlowParams.class);
        verify(engineTaskService).reductionSignature(eq(101L), captor.capture());
        assertThat(captor.getValue().getReductionHandlers()).containsExactly("user:2");
        assertThat(captor.getValue().getAddHandlers()).isNull();
    }

    @Test
    void startRejectsBlankFlowCode() {
        ApiWorkflowStartParam param = new ApiWorkflowStartParam();
        param.setBusinessId("BIZ-1");

        assertErrorCode("10701", () -> instanceService.start(param));
    }

    @Test
    void startRejectsUnpublishedDefinition() {
        when(engineDefinitionService.getPublishByFlowCode("leave")).thenReturn(null);

        assertErrorCode("20701", () -> instanceService.start(startParam()));
    }

    @Test
    void startTranslatesEngineRefusalToOperationNotAllowed() {
        when(engineDefinitionService.getPublishByFlowCode("leave")).thenReturn(mock(Definition.class));
        when(engineInstanceService.start(eq("BIZ-1"), any(FlowParams.class)))
            .thenThrow(new FlowException("流程缺少开始节点!"));

        assertErrorCode("20704", () -> instanceService.start(startParam()));
    }

    @Test
    void startPassesFlowCodeHandlerAndNextHandlersToEngine() {
        when(engineDefinitionService.getPublishByFlowCode("leave")).thenReturn(mock(Definition.class));
        Instance instance = mock(Instance.class);
        when(instance.getId()).thenReturn(303L);
        when(engineInstanceService.start(eq("BIZ-1"), any(FlowParams.class))).thenReturn(instance);

        ApiWorkflowStartParam param = startParam();
        param.setNextHandlers(List.of("user:9"));
        param.setVariable(Map.of("days", 3));
        WorkflowInstanceDTO dto = instanceService.start(param);

        assertThat(dto.getInstanceId()).isEqualTo(303L);
        ArgumentCaptor<FlowParams> captor = ArgumentCaptor.forClass(FlowParams.class);
        verify(engineInstanceService).start(eq("BIZ-1"), captor.capture());
        assertThat(captor.getValue().getFlowCode()).isEqualTo("leave");
        assertThat(captor.getValue().getHandler()).isEqualTo("user:1");
        assertThat(captor.getValue().getNextHandler()).containsExactly("user:9");
        assertThat(captor.getValue().getVariable()).isEqualTo(Map.of("days", 3));
    }

    @Test
    void chartRejectsMissingInstance() {
        when(engineInstanceService.getById(303L)).thenReturn(null);

        assertErrorCode("20702", () -> instanceService.chart(303L));
    }

    @Test
    void chartRejectsInstanceWithoutChartMetadata() {
        Instance instance = mock(Instance.class);
        when(engineInstanceService.getById(303L)).thenReturn(instance);

        assertErrorCode("50701", () -> instanceService.chart(303L));
    }

    @Test
    void chartMapsNodesStatusAndColors() {
        Instance instance = mock(Instance.class);
        when(instance.getId()).thenReturn(303L);
        when(instance.getDefinitionId()).thenReturn(7L);
        when(instance.getFlowName()).thenReturn("请假流程");
        when(instance.getDefJson()).thenReturn(CHART_JSON);
        when(engineInstanceService.getById(303L)).thenReturn(instance);
        when(engineChartService.getChartRgb("CLASSICS"))
            .thenReturn(List.of("56,161,105", "245,158,11", "107,114,128"));

        WorkflowChartDTO dto = instanceService.chart(303L);

        assertThat(dto.getInstanceId()).isEqualTo(303L);
        assertThat(dto.getDefinitionId()).isEqualTo(7L);
        assertThat(dto.getFlowCode()).isEqualTo("leave");
        assertThat(dto.getChartStatusColor()).hasSize(3);
        assertThat(dto.getNodes()).hasSize(2);
        assertThat(dto.getNodes().get(0).getNodeCode()).isEqualTo("node1");
        assertThat(dto.getNodes().get(0).getStatus()).isEqualTo(2);
        assertThat(dto.getNodes().get(0).getSkips()).hasSize(1);
        assertThat(dto.getNodes().get(0).getSkips().get(0).getNextNodeCode()).isEqualTo("node2");
        assertThat(dto.getNodes().get(1).getStatus()).isEqualTo(1);
        assertThat(dto.getSourceJson()).isEqualTo(CHART_JSON);
    }

    @Test
    void definitionSaveRejectsUnknownAssigneeExpressionListingNodeCodes() {
        ApiWorkflowDefinitionSaveParam param = new ApiWorkflowDefinitionSaveParam();
        param.setDefinitionId(7L);
        param.setDefJson(DEFINITION_JSON_WITH_INVALID_ASSIGNEE);

        Throwable thrown = catchThrowable(() -> definitionService.save(param));

        assertThat(thrown).isInstanceOf(WorkflowException.class);
        assertThat(((WorkflowException) thrown).getErrorCode()).isEqualTo("10705");
        assertThat(thrown.getMessage()).contains("approve").contains("manager:5");
        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void definitionSaveAcceptsDefinitionWithValidAssigneeExpressions() throws Exception {
        ApiWorkflowDefinitionSaveParam param = new ApiWorkflowDefinitionSaveParam();
        param.setDefinitionId(7L);
        param.setDefJson(DEFINITION_JSON_WITH_VALID_ASSIGNEES);
        Definition saved = mock(Definition.class);
        when(engineDefinitionService.getById(7L)).thenReturn(saved);

        definitionService.save(param);

        verify(engineDefinitionService).saveDef(any(DefJson.class), eq(true));
    }

    @Test
    void definitionSaveRejectsBlankJson() {
        ApiWorkflowDefinitionSaveParam param = new ApiWorkflowDefinitionSaveParam();

        assertErrorCode("10701", () -> definitionService.save(param));
    }

    @Test
    void definitionSaveTranslatesImportFailureToDefinitionJsonInvalid() {
        ApiWorkflowDefinitionSaveParam param = new ApiWorkflowDefinitionSaveParam();
        param.setDefJson("{");
        when(engineDefinitionService.importJson("{")).thenThrow(new FlowException("json转换异常"));

        assertErrorCode("10703", () -> definitionService.save(param));
    }

    @Test
    void definitionSaveTranslatesSaveDefFailureToDefinitionJsonInvalid() throws Exception {
        ApiWorkflowDefinitionSaveParam param = new ApiWorkflowDefinitionSaveParam();
        param.setDefinitionId(7L);
        param.setDefJson(CHART_JSON);
        doThrow(new FlowException("流程缺少开始节点!"))
            .when(engineDefinitionService).saveDef(any(), eq(true));

        assertErrorCode("10703", () -> definitionService.save(param));
    }

    @Test
    void definitionListPublishedMapsDefinitions() {
        Definition definition = mock(Definition.class);
        when(definition.getId()).thenReturn(7L);
        when(definition.getFlowCode()).thenReturn("leave");
        when(definition.getIsPublish()).thenReturn(1);
        when(engineDefinitionService.list(any())).thenReturn(List.of(definition));

        assertThat(definitionService.listPublished())
            .hasSize(1)
            .allSatisfy(dto -> {
                assertThat(dto.getDefinitionId()).isEqualTo(7L);
                assertThat(dto.getFlowCode()).isEqualTo("leave");
                assertThat(dto.getIsPublish()).isEqualTo(1);
            });
    }

    private FlowParams capturedSkipParams(Long taskId) {
        ArgumentCaptor<FlowParams> captor = ArgumentCaptor.forClass(FlowParams.class);
        verify(engineTaskService).skip(eq(taskId), captor.capture());
        return captor.getValue();
    }

    private ApiWorkflowHandleParam handleParam(WorkflowActionEnum action) {
        ApiWorkflowHandleParam param = new ApiWorkflowHandleParam();
        param.setTaskId(101L);
        param.setAction(action);
        param.setHandler("user:1");
        param.setMessage("同意");
        return param;
    }

    private ApiWorkflowStartParam startParam() {
        ApiWorkflowStartParam param = new ApiWorkflowStartParam();
        param.setFlowCode("leave");
        param.setBusinessId("BIZ-1");
        param.setHandler("user:1");
        return param;
    }

    private static void assertErrorCode(String expectedCode, ThrowingRunnable runnable) {
        Throwable thrown = catchThrowable(runnable::run);
        assertThat(thrown).isInstanceOf(WorkflowException.class);
        assertThat(((WorkflowException) thrown).getErrorCode()).isEqualTo(expectedCode);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {

        void run();
    }

    /**
     * 引擎在实例上维护的流程图元数据片段：node1 已办理、node2 待办理
     */
    private static final String CHART_JSON = """
        {"id":7,"flowCode":"leave","flowName":"请假流程","modelValue":"CLASSICS","nodeList":[
          {"nodeCode":"node1","nodeName":"发起","nodeType":0,"status":2,
           "skipList":[{"nowNodeCode":"node1","nextNodeCode":"node2","skipType":"PASS","status":2}]},
          {"nodeCode":"node2","nodeName":"经理审批","nodeType":1,"status":1,"skipList":[]}]}
        """;

    /**
     * 含未知词汇类型（{@code manager:5}）的定义：保存期校验必须拦下
     */
    private static final String DEFINITION_JSON_WITH_INVALID_ASSIGNEE = """
        {"id":7,"flowCode":"leave","flowName":"请假流程","modelValue":"CLASSICS","nodeList":[
          {"nodeCode":"start","nodeName":"发起","nodeType":0},
          {"nodeCode":"approve","nodeName":"经理审批","nodeType":1,"permissionFlag":"manager:5"}]}
        """;

    /**
     * 办理人表达式全部合法的定义（静态词汇 + SpEL）
     */
    private static final String DEFINITION_JSON_WITH_VALID_ASSIGNEES = """
        {"id":7,"flowCode":"leave","flowName":"请假流程","modelValue":"CLASSICS","nodeList":[
          {"nodeCode":"start","nodeName":"发起","nodeType":0},
          {"nodeCode":"approve","nodeName":"经理审批","nodeType":1,"permissionFlag":"role:5@@user:1001,1002"},
          {"nodeCode":"escalate","nodeName":"超时升级","nodeType":1,
           "permissionFlag":"#{@workflowAssignees.upLeader(#applicantId)}"}]}
        """;

}
