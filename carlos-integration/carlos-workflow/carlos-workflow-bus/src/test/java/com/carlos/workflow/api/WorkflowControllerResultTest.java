package com.carlos.workflow.api;

import com.carlos.core.pagination.Paging;
import com.carlos.workflow.api.enums.WorkflowActionEnum;
import com.carlos.workflow.api.pojo.ao.WorkflowChartAO;
import com.carlos.workflow.api.pojo.ao.WorkflowChartNodeAO;
import com.carlos.workflow.api.pojo.ao.WorkflowDefinitionAO;
import com.carlos.workflow.api.pojo.ao.WorkflowHandleResultAO;
import com.carlos.workflow.api.pojo.ao.WorkflowHisTaskAO;
import com.carlos.workflow.api.pojo.ao.WorkflowInstanceAO;
import com.carlos.workflow.api.pojo.ao.WorkflowTaskAO;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionBasicParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionPageParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionSaveParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowHandleParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowStartParam;
import com.carlos.workflow.apiimpl.ApiWorkflowDefinitionImpl;
import com.carlos.workflow.apiimpl.ApiWorkflowInstanceImpl;
import com.carlos.workflow.apiimpl.ApiWorkflowTaskImpl;
import com.carlos.workflow.controller.WorkflowDefinitionController;
import com.carlos.workflow.controller.WorkflowInstanceController;
import com.carlos.workflow.controller.WorkflowTaskController;
import com.carlos.workflow.dto.WorkflowChartDTO;
import com.carlos.workflow.dto.WorkflowChartNodeDTO;
import com.carlos.workflow.dto.WorkflowDefinitionDTO;
import com.carlos.workflow.dto.WorkflowHandleResultDTO;
import com.carlos.workflow.dto.WorkflowHisTaskDTO;
import com.carlos.workflow.dto.WorkflowInstanceDTO;
import com.carlos.workflow.dto.WorkflowTaskDTO;
import com.carlos.workflow.service.WorkflowDefinitionService;
import com.carlos.workflow.service.WorkflowInstanceService;
import com.carlos.workflow.service.WorkflowTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>
 * 工作流 REST 接口契约测试：用真实 MockMvc 走过 DispatcherServlet 派发，
 * 覆盖两套命名空间的路径映射、HTTP 方法、参数绑定与统一 Result 信封。
 * </p>
 * <p>
 * 两套命名空间同时装配在一个 standaloneSetup 里，正是为了钉住「同一路径不会同时存在两个 handler」：
 * </p>
 * <ul>
 *   <li><b>服务间（Feign）</b>：{@code com.carlos.workflow.apiimpl} 的三个实现类，前缀 {@code /api/workflow/**}；</li>
 *   <li><b>前端（经网关）</b>：{@code com.carlos.workflow.controller} 的三个控制器，前缀 {@code /workflow/**}。</li>
 * </ul>
 * <p>
 * 采用 {@code standaloneSetup}（与 {@code carlos-job} 的接口测试一致）：只装配这六个控制器，
 * 不启动 Spring 上下文、不连数据库，服务层用 mock 替身。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowControllerResultTest {

    private final WorkflowTaskService workflowTaskService = mock(WorkflowTaskService.class);

    private final WorkflowInstanceService workflowInstanceService = mock(WorkflowInstanceService.class);

    private final WorkflowDefinitionService workflowDefinitionService = mock(WorkflowDefinitionService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
            new ApiWorkflowTaskImpl(workflowTaskService),
            new ApiWorkflowInstanceImpl(workflowInstanceService),
            new ApiWorkflowDefinitionImpl(workflowDefinitionService),
            new WorkflowTaskController(workflowTaskService),
            new WorkflowInstanceController(workflowInstanceService),
            new WorkflowDefinitionController(workflowDefinitionService)
        ).build();
    }

    @Test
    void everyControllerIsRestrictedToServletWebApplicationsAndGatedByEnabledFlag() {
        for (Class<?> controller : List.of(ApiWorkflowTaskImpl.class, ApiWorkflowInstanceImpl.class,
            ApiWorkflowDefinitionImpl.class, WorkflowTaskController.class,
            WorkflowInstanceController.class, WorkflowDefinitionController.class)) {
            ConditionalOnWebApplication webCondition = controller.getAnnotation(ConditionalOnWebApplication.class);
            assertThat(webCondition)
                .as("%s 必须限定 Servlet 容器", controller.getSimpleName())
                .isNotNull();
            assertThat(webCondition.type()).isEqualTo(ConditionalOnWebApplication.Type.SERVLET);

            ConditionalOnProperty propertyCondition = controller.getAnnotation(ConditionalOnProperty.class);
            assertThat(propertyCondition)
                .as("%s 必须受 carlos.workflow.enabled 控制", controller.getSimpleName())
                .isNotNull();
            assertThat(propertyCondition.prefix()).isEqualTo("carlos.workflow");
            assertThat(propertyCondition.name()).containsExactly("enabled");
        }
    }

    @Test
    void taskDetailReturnsResultEnvelope() throws Exception {
        WorkflowTaskDTO dto = new WorkflowTaskDTO();
        dto.setTaskId(101L);
        dto.setNodeCode("node1");
        dto.setNodeName("经理审批");
        when(workflowTaskService.load(101L)).thenReturn(dto);

        mockMvc.perform(get("/api/workflow/task/{taskId}", 101))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.msg").value("操作成功"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.taskId").value(101))
            .andExpect(jsonPath("$.data.nodeCode").value("node1"))
            .andExpect(jsonPath("$.data.nodeName").value("经理审批"));
    }

    @Test
    void hisTaskDetailReturnsResultEnvelope() throws Exception {
        WorkflowHisTaskDTO dto = new WorkflowHisTaskDTO();
        dto.setHisTaskId(202L);
        dto.setSkipType("PASS");
        when(workflowTaskService.loadHistory(202L)).thenReturn(dto);

        mockMvc.perform(get("/api/workflow/his-task/{hisTaskId}", 202))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.hisTaskId").value(202))
            .andExpect(jsonPath("$.data.skipType").value("PASS"));
    }

    @Test
    void handleReturnsResultEnvelopeOnTheFrontendNamespace() throws Exception {
        when(workflowTaskService.handle(any())).thenReturn(WorkflowHandleResultDTO.builder()
            .action(WorkflowActionEnum.PASS).taskId(101L).operated(true).build());

        String body = """
            {"taskId":101,"action":"PASS","handler":"user:1","message":"同意"}
            """;

        mockMvc.perform(post("/workflow/task/handle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.taskId").value(101))
            .andExpect(jsonPath("$.data.operated").value(true))
            .andExpect(jsonPath("$.data.action").value("PASS"))
            .andExpect(jsonPath("$.data.instance").doesNotExist());
    }

    @Test
    void handleBindsRequestBodyIntoApiParam() throws Exception {
        when(workflowTaskService.handle(any())).thenReturn(WorkflowHandleResultDTO.builder()
            .action(WorkflowActionEnum.ADD_SIGNATURE).taskId(101L).operated(true).build());

        String body = """
            {"taskId":101,"action":"ADD_SIGNATURE","handler":"user:1",
             "targetHandlers":["user:2","user:3"],"variable":{"days":3}}
            """;

        mockMvc.perform(post("/workflow/task/handle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.action").value("ADD_SIGNATURE"));

        ArgumentCaptor<ApiWorkflowHandleParam> captor =
            ArgumentCaptor.forClass(ApiWorkflowHandleParam.class);
        verify(workflowTaskService).handle(captor.capture());
        assertThat(captor.getValue().getTaskId()).isEqualTo(101L);
        assertThat(captor.getValue().getAction()).isEqualTo(WorkflowActionEnum.ADD_SIGNATURE);
        assertThat(captor.getValue().getHandler()).isEqualTo("user:1");
        assertThat(captor.getValue().getTargetHandlers()).containsExactly("user:2", "user:3");
        assertThat(captor.getValue().getVariable()).containsEntry("days", 3);
    }

    @Test
    void instanceStartReturnsResultEnvelope() throws Exception {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        dto.setInstanceId(303L);
        dto.setBusinessId("BIZ-1");
        when(workflowInstanceService.start(any())).thenReturn(dto);

        String body = """
            {"flowCode":"leave","businessId":"BIZ-1","handler":"user:1"}
            """;

        mockMvc.perform(post("/api/workflow/instance/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.instanceId").value(303))
            .andExpect(jsonPath("$.data.businessId").value("BIZ-1"));

        ArgumentCaptor<ApiWorkflowStartParam> captor =
            ArgumentCaptor.forClass(ApiWorkflowStartParam.class);
        verify(workflowInstanceService).start(captor.capture());
        assertThat(captor.getValue().getFlowCode()).isEqualTo("leave");
        assertThat(captor.getValue().getBusinessId()).isEqualTo("BIZ-1");
    }

    @Test
    void instanceChartReturnsResultEnvelope() throws Exception {
        WorkflowChartNodeDTO node = new WorkflowChartNodeDTO();
        node.setNodeCode("node1");
        node.setStatus(2);
        WorkflowChartDTO dto = new WorkflowChartDTO();
        dto.setInstanceId(303L);
        dto.setDefinitionId(7L);
        dto.setChartStatusColor(List.of("56,161,105", "245,158,11", "107,114,128"));
        dto.setNodes(List.of(node));
        when(workflowInstanceService.chart(303L)).thenReturn(dto);

        mockMvc.perform(get("/api/workflow/instance/{instanceId}/chart", 303))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.instanceId").value(303))
            .andExpect(jsonPath("$.data.definitionId").value(7))
            .andExpect(jsonPath("$.data.chartStatusColor.length()").value(3))
            .andExpect(jsonPath("$.data.nodes[0].nodeCode").value("node1"))
            .andExpect(jsonPath("$.data.nodes[0].status").value(2));
    }

    @Test
    void publishedDefinitionsReturnResultEnvelope() throws Exception {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setDefinitionId(7L);
        dto.setFlowCode("leave");
        when(workflowDefinitionService.listPublished()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/workflow/definition/published"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].definitionId").value(7))
            .andExpect(jsonPath("$.data[0].flowCode").value("leave"));
    }

    @Test
    void definitionSaveReturnsResultEnvelope() throws Exception {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setDefinitionId(7L);
        dto.setFlowCode("leave");
        when(workflowDefinitionService.save(any())).thenReturn(dto);

        String body = """
            {"definitionId":7,"defJson":"{}","onlyNodeSkip":true,"publish":false}
            """;

        mockMvc.perform(post("/workflow/definition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.definitionId").value(7))
            .andExpect(jsonPath("$.data.flowCode").value("leave"));

        ArgumentCaptor<ApiWorkflowDefinitionSaveParam> captor =
            ArgumentCaptor.forClass(ApiWorkflowDefinitionSaveParam.class);
        verify(workflowDefinitionService).save(captor.capture());
        assertThat(captor.getValue().getDefinitionId()).isEqualTo(7L);
        assertThat(captor.getValue().getDefJson()).isEqualTo("{}");
        assertThat(captor.getValue().isOnlyNodeSkip()).isTrue();
        assertThat(captor.getValue().isPublish()).isFalse();
    }

    @Test
    void definitionBasicCreatesWhenDefinitionIdIsAbsent() throws Exception {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setDefinitionId(9L);
        dto.setFlowCode("leave");
        dto.setModelValue("CLASSICS");
        when(workflowDefinitionService.createBasic(any())).thenReturn(dto);

        String body = """
            {"flowCode":"leave","flowName":"请假流程","modelValue":"CLASSICS","category":"HR"}
            """;

        mockMvc.perform(post("/workflow/definition/basic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.definitionId").value(9))
            .andExpect(jsonPath("$.data.flowCode").value("leave"))
            .andExpect(jsonPath("$.data.modelValue").value("CLASSICS"));

        ArgumentCaptor<ApiWorkflowDefinitionBasicParam> captor =
            ArgumentCaptor.forClass(ApiWorkflowDefinitionBasicParam.class);
        verify(workflowDefinitionService).createBasic(captor.capture());
        assertThat(captor.getValue().getDefinitionId()).isNull();
        assertThat(captor.getValue().getFlowCode()).isEqualTo("leave");
        assertThat(captor.getValue().getFlowName()).isEqualTo("请假流程");
        assertThat(captor.getValue().getModelValue()).isEqualTo("CLASSICS");
        assertThat(captor.getValue().getCategory()).isEqualTo("HR");
        verify(workflowDefinitionService, never()).updateBasic(any());
    }

    @Test
    void definitionBasicUpdatesWhenDefinitionIdIsPresent() throws Exception {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setDefinitionId(7L);
        dto.setFlowName("请假流程V2");
        when(workflowDefinitionService.updateBasic(any())).thenReturn(dto);

        mockMvc.perform(post("/workflow/definition/basic")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"definitionId\":7,\"flowCode\":\"leave\",\"flowName\":\"请假流程V2\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.definitionId").value(7))
            .andExpect(jsonPath("$.data.flowName").value("请假流程V2"));

        ArgumentCaptor<ApiWorkflowDefinitionBasicParam> captor =
            ArgumentCaptor.forClass(ApiWorkflowDefinitionBasicParam.class);
        verify(workflowDefinitionService).updateBasic(captor.capture());
        assertThat(captor.getValue().getDefinitionId()).isEqualTo(7L);
        verify(workflowDefinitionService, never()).createBasic(any());
    }

    @Test
    void aoTypesAreUsedForEveryResponsePayload() {
        assertThat(List.of(
            WorkflowTaskAO.class, WorkflowHisTaskAO.class, WorkflowHandleResultAO.class,
            WorkflowInstanceAO.class, WorkflowChartAO.class, WorkflowDefinitionAO.class))
            .allSatisfy(ao -> assertThat(ao.getPackageName()).isEqualTo("com.carlos.workflow.api.pojo.ao"));
    }

    @Test
    void definitionPageBindsEveryFilterAndReturnsPagingEnvelope() throws Exception {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setDefinitionId(7L);
        dto.setFlowName("请假流程");
        Paging<WorkflowDefinitionDTO> paging = new Paging<>();
        paging.setTotal(6);
        paging.setCurrent(2);
        paging.setSize(5);
        paging.setPages(2);
        paging.setRecords(List.of(dto));
        when(workflowDefinitionService.page(any())).thenReturn(paging);

        mockMvc.perform(get("/workflow/definition/page")
                .param("current", "2")
                .param("size", "5")
                .param("flowName", "请假")
                .param("flowCode", "leave")
                .param("category", "HR")
                .param("isPublish", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.total").value(6))
            .andExpect(jsonPath("$.data.current").value(2))
            .andExpect(jsonPath("$.data.size").value(5))
            .andExpect(jsonPath("$.data.pages").value(2))
            .andExpect(jsonPath("$.data.records[0].definitionId").value(7))
            .andExpect(jsonPath("$.data.records[0].flowName").value("请假流程"));

        ArgumentCaptor<ApiWorkflowDefinitionPageParam> captor =
            ArgumentCaptor.forClass(ApiWorkflowDefinitionPageParam.class);
        verify(workflowDefinitionService).page(captor.capture());
        assertThat(captor.getValue().getCurrent()).isEqualTo(2);
        assertThat(captor.getValue().getSize()).isEqualTo(5);
        assertThat(captor.getValue().getFlowName()).isEqualTo("请假");
        assertThat(captor.getValue().getFlowCode()).isEqualTo("leave");
        assertThat(captor.getValue().getCategory()).isEqualTo("HR");
        assertThat(captor.getValue().getIsPublish()).isEqualTo(1);
    }

    @Test
    void definitionPageWorksWithoutAnyFilterOrPageNumber() throws Exception {
        Paging<WorkflowDefinitionDTO> paging = new Paging<>();
        paging.setRecords(List.of());
        when(workflowDefinitionService.page(any())).thenReturn(paging);

        mockMvc.perform(get("/workflow/definition/page"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records").isArray());

        ArgumentCaptor<ApiWorkflowDefinitionPageParam> captor =
            ArgumentCaptor.forClass(ApiWorkflowDefinitionPageParam.class);
        verify(workflowDefinitionService).page(captor.capture());
        assertThat(captor.getValue().getFlowName()).isNull();
        assertThat(captor.getValue().getIsPublish()).isNull();
        assertThat(captor.getValue().getCurrent()).isEqualTo(1);
        assertThat(captor.getValue().getSize()).isEqualTo(10);
    }

    @Test
    void definitionDetailBindsPathVariableAndReturnsResultEnvelope() throws Exception {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setDefinitionId(7L);
        dto.setFlowCode("leave");
        dto.setIsPublish(1);
        when(workflowDefinitionService.detail(7L)).thenReturn(dto);

        mockMvc.perform(get("/workflow/definition/{id}", 7))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.definitionId").value(7))
            .andExpect(jsonPath("$.data.flowCode").value("leave"))
            .andExpect(jsonPath("$.data.isPublish").value(1));

        verify(workflowDefinitionService).detail(7L);
    }

    @Test
    void definitionPublishBindsIdsAndReturnsResultEnvelope() throws Exception {
        when(workflowDefinitionService.publish(any())).thenReturn(true);

        mockMvc.perform(post("/workflow/definition/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[1,2]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(true));

        assertThat(capturedIds(workflowDefinitionService, "publish")).containsExactly(1L, 2L);
    }

    @Test
    void definitionUnpublishBindsIdsAndReturnsResultEnvelope() throws Exception {
        when(workflowDefinitionService.unpublish(any())).thenReturn(true);

        mockMvc.perform(post("/workflow/definition/unpublish")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[3]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(true));

        assertThat(capturedIds(workflowDefinitionService, "unpublish")).containsExactly(3L);
    }

    @Test
    void definitionCopyBindsIdAndReturnsTheNewDefinitionId() throws Exception {
        when(workflowDefinitionService.copy(1L)).thenReturn(9L);

        mockMvc.perform(post("/workflow/definition/copy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(9));

        verify(workflowDefinitionService).copy(1L);
    }

    @Test
    void definitionRemoveIsMappedOnTheCollectionUrlWithBodyIds() throws Exception {
        when(workflowDefinitionService.remove(any())).thenReturn(true);

        mockMvc.perform(delete("/workflow/definition")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[1,2]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(true));

        assertThat(capturedIds(workflowDefinitionService, "remove")).containsExactly(1L, 2L);
    }

    /**
     * 抓取批量端点收到的 id 列表：三个批量动作参数类型一致，按方法名分别校验入参
     */
    @SuppressWarnings("unchecked")
    private List<Long> capturedIds(WorkflowDefinitionService service, String methodName) throws Exception {
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        switch (methodName) {
            case "publish" -> verify(service).publish(captor.capture());
            case "unpublish" -> verify(service).unpublish(captor.capture());
            case "remove" -> verify(service).remove(captor.capture());
            default -> throw new IllegalArgumentException("未知的批量端点: " + methodName);
        }
        return captor.getValue();
    }

}
