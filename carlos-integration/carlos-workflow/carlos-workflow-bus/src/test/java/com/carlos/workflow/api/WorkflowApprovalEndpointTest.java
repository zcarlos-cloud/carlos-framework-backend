package com.carlos.workflow.api;

import com.carlos.core.pagination.Paging;
import com.carlos.workflow.api.pojo.ao.WorkflowCandidateAO;
import com.carlos.workflow.api.pojo.ao.WorkflowHisTaskAO;
import com.carlos.workflow.api.pojo.ao.WorkflowInstanceAO;
import com.carlos.workflow.api.pojo.ao.WorkflowNodeAO;
import com.carlos.workflow.api.pojo.ao.WorkflowTaskAO;
import com.carlos.workflow.api.pojo.param.ApiWorkflowCandidatePageParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowInstanceActionParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowTaskPageParam;
import com.carlos.workflow.controller.WorkflowInstanceController;
import com.carlos.workflow.controller.WorkflowTaskController;
import com.carlos.workflow.dto.WorkflowCandidateDTO;
import com.carlos.workflow.dto.WorkflowHisTaskDTO;
import com.carlos.workflow.dto.WorkflowInstanceDTO;
import com.carlos.workflow.dto.WorkflowNodeDTO;
import com.carlos.workflow.dto.WorkflowTaskDTO;
import com.carlos.workflow.service.WorkflowInstanceService;
import com.carlos.workflow.service.WorkflowTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>
 * 新增审批端点的 HTTP 映射与参数绑定测试：待办 / 已办分页、候选办理人、审批记录、可选跳转节点，
 * 以及实例级动作（撤回 / 终止 / 挂起 / 激活）。
 * </p>
 * <p>
 * 这些端点全部是<b>前端（经网关）</b>的，落在 {@code /workflow/**}
 * （{@code com.carlos.workflow.controller} 下的两个控制器），不是服务间的 Feign 契约；
 * {@code /api/workflow/**} 那套只保留单个任务 / 单条历史任务的读取，由
 * {@code WorkflowControllerResultTest} 与 {@code WorkflowFeignContractTest} 覆盖。
 * </p>
 * <p>
 * 与 {@code WorkflowFeignContractTest}（比对 Feign 契约与实现的 path 是否逐字一致）互补：
 * 这里真正把请求打进 DispatcherServlet，能发现「路径写法合法但与已有的
 * {@code /workflow/task/{taskId}} 之类映射冲突/歧义」这类只在运行期暴露的问题。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowApprovalEndpointTest {

    private final WorkflowTaskService workflowTaskService = mock(WorkflowTaskService.class);

    private final WorkflowInstanceService workflowInstanceService = mock(WorkflowInstanceService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
            new WorkflowTaskController(workflowTaskService),
            new WorkflowInstanceController(workflowInstanceService)
        ).build();
    }

    @Test
    void todoPageBindsFiltersAndReturnsPagingEnvelope() throws Exception {
        WorkflowTaskDTO dto = new WorkflowTaskDTO();
        dto.setTaskId(101L);
        dto.setFlowName("请假流程");
        dto.setApprover("张三");
        Paging<WorkflowTaskDTO> paging = new Paging<>();
        paging.setTotal(7);
        paging.setCurrent(2);
        paging.setSize(3);
        paging.setPages(3);
        paging.setRecords(List.of(dto));
        when(workflowTaskService.todoPage(any())).thenReturn(paging);

        mockMvc.perform(get("/workflow/task/todo/page")
                .param("current", "2")
                .param("size", "3")
                .param("nodeName", "经理")
                .param("flowStatus", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.total").value(7))
            .andExpect(jsonPath("$.data.current").value(2))
            .andExpect(jsonPath("$.data.pages").value(3))
            .andExpect(jsonPath("$.data.records[0].taskId").value(101))
            .andExpect(jsonPath("$.data.records[0].flowName").value("请假流程"))
            .andExpect(jsonPath("$.data.records[0].approver").value("张三"));

        ArgumentCaptor<ApiWorkflowTaskPageParam> captor = ArgumentCaptor.forClass(ApiWorkflowTaskPageParam.class);
        verify(workflowTaskService).todoPage(captor.capture());
        assertThat(captor.getValue().getCurrent()).isEqualTo(2);
        assertThat(captor.getValue().getSize()).isEqualTo(3);
        assertThat(captor.getValue().getNodeName()).isEqualTo("经理");
        assertThat(captor.getValue().getFlowStatus()).isEqualTo("1");
    }

    @Test
    void donePageBindsFiltersAndReturnsPagingEnvelope() throws Exception {
        WorkflowHisTaskDTO dto = new WorkflowHisTaskDTO();
        dto.setHisTaskId(202L);
        dto.setSkipType("PASS");
        Paging<WorkflowHisTaskDTO> paging = new Paging<>();
        paging.setTotal(1);
        paging.setRecords(List.of(dto));
        when(workflowTaskService.donePage(any())).thenReturn(paging);

        mockMvc.perform(get("/workflow/task/done/page").param("nodeName", "经理"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records[0].hisTaskId").value(202))
            .andExpect(jsonPath("$.data.records[0].skipType").value("PASS"));

        ArgumentCaptor<ApiWorkflowTaskPageParam> captor = ArgumentCaptor.forClass(ApiWorkflowTaskPageParam.class);
        verify(workflowTaskService).donePage(captor.capture());
        assertThat(captor.getValue().getNodeName()).isEqualTo("经理");
    }

    @Test
    void candidatesBindsTaskIdActionDeptIdAndPaging() throws Exception {
        WorkflowCandidateDTO dto = new WorkflowCandidateDTO();
        dto.setUserId(1001L);
        dto.setName("张三");
        Paging<WorkflowCandidateDTO> paging = new Paging<>();
        paging.setRecords(List.of(dto));
        when(workflowTaskService.candidates(eq(101L), any())).thenReturn(paging);

        mockMvc.perform(get("/workflow/task/{taskId}/candidates", 101)
                .param("action", "REDUCTION_SIGNATURE")
                .param("deptId", "12")
                .param("current", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records[0].userId").value(1001))
            .andExpect(jsonPath("$.data.records[0].name").value("张三"));

        ArgumentCaptor<ApiWorkflowCandidatePageParam> captor =
            ArgumentCaptor.forClass(ApiWorkflowCandidatePageParam.class);
        verify(workflowTaskService).candidates(eq(101L), captor.capture());
        assertThat(captor.getValue().getAction().name()).isEqualTo("REDUCTION_SIGNATURE");
        assertThat(captor.getValue().getDeptId()).isEqualTo(12L);
        assertThat(captor.getValue().getSize()).isEqualTo(20);
    }

    @Test
    void hisTaskListReturnsEveryHistoryRecordAndKeepsThePathDistinctFromTaskDetail() throws Exception {
        WorkflowHisTaskDTO dto = new WorkflowHisTaskDTO();
        dto.setHisTaskId(201L);
        dto.setApprover("张三");
        when(workflowInstanceService.listHisTask(303L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/workflow/instance/{instanceId}/his-task", 303))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].hisTaskId").value(201))
            .andExpect(jsonPath("$.data[0].approver").value("张三"));
    }

    @Test
    void anyNodeListReturnsSelectableNodes() throws Exception {
        WorkflowNodeDTO node = new WorkflowNodeDTO();
        node.setNodeCode("nodeA");
        node.setNodeName("发起人确认");
        when(workflowInstanceService.listAnyNode(303L)).thenReturn(List.of(node));

        mockMvc.perform(get("/workflow/instance/{instanceId}/any-node", 303))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].nodeCode").value("nodeA"))
            .andExpect(jsonPath("$.data[0].nodeName").value("发起人确认"));
    }

    @Test
    void revokeBindsInstanceIdAndMessage() throws Exception {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        dto.setInstanceId(303L);
        dto.setFlowStatus("6");
        when(workflowInstanceService.revoke(any())).thenReturn(dto);

        mockMvc.perform(post("/workflow/instance/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"instanceId\":303,\"message\":\"撤销申请\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.instanceId").value(303))
            .andExpect(jsonPath("$.data.flowStatus").value("6"));

        ArgumentCaptor<ApiWorkflowInstanceActionParam> captor =
            ArgumentCaptor.forClass(ApiWorkflowInstanceActionParam.class);
        verify(workflowInstanceService).revoke(captor.capture());
        assertThat(captor.getValue().getInstanceId()).isEqualTo(303L);
        assertThat(captor.getValue().getMessage()).isEqualTo("撤销申请");
    }

    @Test
    void terminateBindsInstanceIdAndMessage() throws Exception {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        dto.setInstanceId(303L);
        when(workflowInstanceService.terminate(any())).thenReturn(dto);

        mockMvc.perform(post("/workflow/instance/terminate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"instanceId\":303,\"message\":\"终止\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.instanceId").value(303));

        verify(workflowInstanceService).terminate(any(ApiWorkflowInstanceActionParam.class));
    }

    @Test
    void suspendAndActivateArePostMappingsOnThePathVariable() throws Exception {
        when(workflowInstanceService.suspend(303L)).thenReturn(true);
        when(workflowInstanceService.activate(303L)).thenReturn(true);

        mockMvc.perform(post("/workflow/instance/{instanceId}/suspend", 303))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(post("/workflow/instance/{instanceId}/activate", 303))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));

        verify(workflowInstanceService).suspend(303L);
        verify(workflowInstanceService).activate(303L);
    }

    @Test
    void aoTypesAreUsedForEveryNewPayload() {
        assertThat(List.of(WorkflowCandidateAO.class, WorkflowNodeAO.class, WorkflowTaskAO.class,
            WorkflowHisTaskAO.class, WorkflowInstanceAO.class))
            .allSatisfy(ao -> assertThat(ao.getPackageName()).isEqualTo("com.carlos.workflow.api.pojo.ao"));
    }

}
