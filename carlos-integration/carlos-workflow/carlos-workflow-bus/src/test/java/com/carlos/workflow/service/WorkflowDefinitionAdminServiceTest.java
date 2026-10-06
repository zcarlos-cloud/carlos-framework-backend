package com.carlos.workflow.service;

import com.carlos.core.pagination.Paging;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionPageParam;
import com.carlos.workflow.dto.WorkflowDefinitionDTO;
import com.carlos.workflow.exception.WorkflowException;
import com.carlos.workflow.manager.WorkflowDefinitionManager;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.service.DefService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 流程定义管理端动作（分页 / 详情 / 发布 / 撤销发布 / 复制 / 删除）的服务层测试。
 * </p>
 * <p>
 * 引擎 Bean 与 Manager 全部 mock：这里校验的是「服务层的编排与错误码翻译」，即
 * 分页是否原样下沉给 Manager、发布之前是否先确认定义存在、引擎的 {@code FlowException}
 * 是否被翻译成 {@code 20704}；真实 SQL 与 Mapper 注入由
 * {@code com.carlos.workflow.manager.WorkflowDefinitionManagerTest} 用 H2 覆盖。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowDefinitionAdminServiceTest {

    private final DefService engineDefinitionService = mock(DefService.class);

    private final WorkflowDefinitionManager definitionManager = mock(WorkflowDefinitionManager.class);

    private final WorkflowDefinitionService definitionService =
        new WorkflowDefinitionService(engineDefinitionService, definitionManager);

    @Test
    void pageHandsTheWholeParamOverToTheManager() {
        ApiWorkflowDefinitionPageParam param = new ApiWorkflowDefinitionPageParam();
        param.setCurrent(2);
        param.setSize(5);
        param.setFlowName("请假");
        param.setFlowCode("leave");
        param.setCategory("HR");
        param.setIsPublish(1);

        Paging<WorkflowDefinitionDTO> expected = new Paging<>();
        expected.setTotal(1);
        when(definitionManager.getPage(any(ApiWorkflowDefinitionPageParam.class))).thenReturn(expected);

        assertThat(definitionService.page(param)).isSameAs(expected);

        ArgumentCaptor<ApiWorkflowDefinitionPageParam> captor =
            ArgumentCaptor.forClass(ApiWorkflowDefinitionPageParam.class);
        verify(definitionManager).getPage(captor.capture());
        assertThat(captor.getValue().getFlowName()).isEqualTo("请假");
        assertThat(captor.getValue().getFlowCode()).isEqualTo("leave");
        assertThat(captor.getValue().getCategory()).isEqualTo("HR");
        assertThat(captor.getValue().getIsPublish()).isEqualTo(1);
        assertThat(captor.getValue().getCurrent()).isEqualTo(2);
        assertThat(captor.getValue().getSize()).isEqualTo(5);
    }

    @Test
    void detailRejectsNullIdWithParamInvalid() {
        assertErrorCode("10701", () -> definitionService.detail(null));
    }

    @Test
    void detailRejectsMissingDefinition() {
        when(definitionManager.getDtoById(404L)).thenReturn(null);

        assertErrorCode("20701", () -> definitionService.detail(404L));
    }

    @Test
    void detailReturnsManagerProjection() {
        when(definitionManager.getDtoById(7L)).thenReturn(definition(7L, "leave"));

        assertThat(definitionService.detail(7L).getFlowCode()).isEqualTo("leave");
    }

    @Test
    void publishRejectsEmptyIdsWithParamInvalid() {
        assertErrorCode("10701", () -> definitionService.publish(List.of()));
    }

    @Test
    void publishRejectsMissingDefinitionBeforeTouchingTheEngine() {
        when(definitionManager.getDtoById(404L)).thenReturn(null);

        assertErrorCode("20701", () -> definitionService.publish(List.of(404L)));
        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void publishCallsTheEngineOncePerDefinition() {
        when(definitionManager.getDtoById(1L)).thenReturn(definition(1L, "leave"));
        when(definitionManager.getDtoById(2L)).thenReturn(definition(2L, "travel"));
        when(engineDefinitionService.publish(anyLong())).thenReturn(true);

        assertThat(definitionService.publish(List.of(1L, 2L))).isTrue();

        verify(engineDefinitionService).publish(1L);
        verify(engineDefinitionService).publish(2L);
    }

    @Test
    void publishTranslatesEngineRefusalToOperationNotAllowed() {
        when(definitionManager.getDtoById(1L)).thenReturn(definition(1L, "leave"));
        when(engineDefinitionService.publish(1L)).thenThrow(new FlowException("没有画流程图，不可发布"));

        assertErrorCode("20704", () -> definitionService.publish(List.of(1L)));
    }

    @Test
    void publishFailsWhenTheEngineReportsFalse() {
        when(definitionManager.getDtoById(1L)).thenReturn(definition(1L, "leave"));
        when(engineDefinitionService.publish(1L)).thenReturn(false);

        assertErrorCode("20704", () -> definitionService.publish(List.of(1L)));
    }

    @Test
    void unpublishCallsTheEngineOncePerDefinition() {
        when(definitionManager.getDtoById(1L)).thenReturn(definition(1L, "leave"));
        when(engineDefinitionService.unPublish(1L)).thenReturn(true);

        assertThat(definitionService.unpublish(List.of(1L))).isTrue();

        verify(engineDefinitionService).unPublish(1L);
    }

    @Test
    void unpublishTranslatesEngineRefusalToOperationNotAllowed() {
        when(definitionManager.getDtoById(1L)).thenReturn(definition(1L, "leave"));
        when(engineDefinitionService.unPublish(1L))
            .thenThrow(new FlowException("流程定义已开启流程任务，不可撤销!"));

        assertErrorCode("20704", () -> definitionService.unpublish(List.of(1L)));
    }

    @Test
    void copyReturnsTheDefinitionIdThatAppearedAfterCopying() {
        when(definitionManager.getDtoById(7L)).thenReturn(definition(7L, "leave"));
        when(definitionManager.listByFlowCode("leave"))
            .thenReturn(List.of(definition(7L, "leave")))
            .thenReturn(List.of(definition(7L, "leave"), definition(8L, "leave")));
        when(engineDefinitionService.copyDef(7L)).thenReturn(true);

        assertThat(definitionService.copy(7L)).isEqualTo(8L);

        verify(engineDefinitionService).copyDef(7L);
    }

    @Test
    void copyRejectsMissingDefinitionBeforeTouchingTheEngine() {
        when(definitionManager.getDtoById(404L)).thenReturn(null);

        assertErrorCode("20701", () -> definitionService.copy(404L));
        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void copyTranslatesEngineRefusalToOperationNotAllowed() {
        when(definitionManager.getDtoById(7L)).thenReturn(definition(7L, "leave"));
        when(definitionManager.listByFlowCode("leave")).thenReturn(List.of(definition(7L, "leave")));
        when(engineDefinitionService.copyDef(7L)).thenThrow(new FlowException("流程定义不存在!"));

        assertErrorCode("20704", () -> definitionService.copy(7L));
    }

    @Test
    void copyFailsWhenTheNewDefinitionCannotBeLocated() {
        when(definitionManager.getDtoById(7L)).thenReturn(definition(7L, "leave"));
        when(definitionManager.listByFlowCode("leave")).thenReturn(List.of(definition(7L, "leave")));
        when(engineDefinitionService.copyDef(7L)).thenReturn(true);

        assertErrorCode("20701", () -> definitionService.copy(7L));
    }

    @Test
    void removePassesEveryIdToTheEngineInOneCall() {
        when(definitionManager.getDtoById(1L)).thenReturn(definition(1L, "leave"));
        when(definitionManager.getDtoById(2L)).thenReturn(definition(2L, "travel"));
        when(engineDefinitionService.removeDef(List.of(1L, 2L))).thenReturn(true);

        assertThat(definitionService.remove(List.of(1L, 2L))).isTrue();

        verify(engineDefinitionService).removeDef(List.of(1L, 2L));
    }

    @Test
    void removeTranslatesEngineRefusalToOperationNotAllowed() {
        when(definitionManager.getDtoById(1L)).thenReturn(definition(1L, "leave"));
        when(engineDefinitionService.removeDef(List.of(1L)))
            .thenThrow(new FlowException("流程定义已开启流程任务，不可删除!"));

        assertErrorCode("20704", () -> definitionService.remove(List.of(1L)));
    }

    private static WorkflowDefinitionDTO definition(Long definitionId, String flowCode) {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setDefinitionId(definitionId);
        dto.setFlowCode(flowCode);
        return dto;
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

}
