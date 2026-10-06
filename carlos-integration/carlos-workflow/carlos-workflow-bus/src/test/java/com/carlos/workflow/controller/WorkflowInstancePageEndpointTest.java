package com.carlos.workflow.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.workflow.api.pojo.param.ApiWorkflowInstanceActionParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowInstancePageParam;
import com.carlos.workflow.dto.WorkflowInstanceDTO;
import com.carlos.workflow.service.WorkflowInstanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>
 * 「我发起的」列表与拿回两个前端端点的路径映射、参数绑定与统一信封测试。
 * </p>
 * <p>
 * 采用 {@code standaloneSetup}（与 {@code WorkflowControllerResultTest} 一致）：只装配这一个控制器，
 * 不启动 Spring 上下文、不连数据库，服务层用 mock 替身。真实 SQL 由
 * {@code com.carlos.workflow.manager.WorkflowInstanceManagerTest} 用 H2 覆盖。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowInstancePageEndpointTest {

    private final WorkflowInstanceService workflowInstanceService = mock(WorkflowInstanceService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WorkflowInstanceController(workflowInstanceService)).build();
    }

    @Test
    void pageBindsEveryFilterAndReturnsThePagingEnvelope() throws Exception {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        dto.setInstanceId(305L);
        dto.setDefinitionId(8L);
        dto.setFlowName("出差流程");
        dto.setBusinessId("BIZ-305");
        dto.setNodeCode("nodeB");
        dto.setNodeType(1);
        dto.setFlowStatus("1");
        dto.setActivityStatus(1);
        dto.setCreateBy("1001");

        Paging<WorkflowInstanceDTO> paging = new Paging<>();
        paging.setTotal(6);
        paging.setCurrent(2);
        paging.setSize(5);
        paging.setPages(2);
        paging.setRecords(List.of(dto));
        when(workflowInstanceService.myPage(any(ApiWorkflowInstancePageParam.class))).thenReturn(paging);

        mockMvc.perform(get("/workflow/instance/page")
                .param("current", "2")
                .param("size", "5")
                .param("flowName", "出差")
                .param("flowStatus", "1")
                .param("businessId", "BIZ-305"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.total").value(6))
            .andExpect(jsonPath("$.data.current").value(2))
            .andExpect(jsonPath("$.data.size").value(5))
            .andExpect(jsonPath("$.data.pages").value(2))
            .andExpect(jsonPath("$.data.records[0].instanceId").value(305))
            .andExpect(jsonPath("$.data.records[0].flowName").value("出差流程"))
            .andExpect(jsonPath("$.data.records[0].businessId").value("BIZ-305"))
            .andExpect(jsonPath("$.data.records[0].flowStatus").value("1"))
            .andExpect(jsonPath("$.data.records[0].createBy").value("1001"));

        ArgumentCaptor<ApiWorkflowInstancePageParam> captor =
            ArgumentCaptor.forClass(ApiWorkflowInstancePageParam.class);
        verify(workflowInstanceService).myPage(captor.capture());
        assertThat(captor.getValue().getCurrent()).isEqualTo(2);
        assertThat(captor.getValue().getSize()).isEqualTo(5);
        assertThat(captor.getValue().getFlowName()).isEqualTo("出差");
        assertThat(captor.getValue().getFlowStatus()).isEqualTo("1");
        assertThat(captor.getValue().getBusinessId()).isEqualTo("BIZ-305");
    }

    @Test
    void takeBackDelegatesToTheServiceAndReturnsTheInstanceEnvelope() throws Exception {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        dto.setInstanceId(303L);
        dto.setNodeCode("nodeB");
        dto.setFlowStatus("1");
        when(workflowInstanceService.takeBack(any(ApiWorkflowInstanceActionParam.class))).thenReturn(dto);

        mockMvc.perform(post("/workflow/instance/take-back")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"instanceId\":303,\"message\":\"发起人拿回修改\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.instanceId").value(303))
            .andExpect(jsonPath("$.data.nodeCode").value("nodeB"));

        ArgumentCaptor<ApiWorkflowInstanceActionParam> captor =
            ArgumentCaptor.forClass(ApiWorkflowInstanceActionParam.class);
        verify(workflowInstanceService).takeBack(captor.capture());
        assertThat(captor.getValue().getInstanceId()).isEqualTo(303L);
        assertThat(captor.getValue().getMessage()).isEqualTo("发起人拿回修改");
    }

}
