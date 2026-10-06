package com.carlos.workflow.service;

import com.carlos.workflow.dto.WorkflowDefinitionDTO;
import com.carlos.workflow.dto.WorkflowDefinitionExportDTO;
import com.carlos.workflow.exception.WorkflowException;
import com.carlos.workflow.manager.WorkflowDefinitionManager;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.json.JsonConvert;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.utils.ServiceLoaderUtil;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 流程定义「导入 / 导出 / 激活 / 挂起」的服务层测试。
 * </p>
 * <p>
 * 引擎 Bean 与 Manager 全部 mock：这里校验的是「服务层的编排与错误码翻译」，即导入前是否先
 * 把非 JSON 挡在引擎之外、导出前是否先确认定义存在、激活 / 挂起的引擎拒绝是否翻译成 20704；
 * 真实 SQL 与 Mapper 注入由 {@code com.carlos.workflow.manager.WorkflowDefinitionManagerTest} 覆盖，
 * 导出端点的响应包装由 {@code com.carlos.workflow.controller.WorkflowDefinitionExportEndpointTest} 覆盖。
 * </p>
 * <p>
 * <b>为什么本类要在 {@code @BeforeAll} 里自己装配 {@code FlowEngine.jsonConvert}：</b>
 * 导入前的「是不是 JSON」判定刻意复用引擎自己的 {@code JsonConvert}（不另写一套 JSON 校验），
 * 而该静态字段由引擎的 {@code WarmFlow#init()} 通过 SPI 赋值（见
 * {@code org.dromara.warm.flow.core.config.WarmFlow} 第 156 行）。纯单元测试不起 Spring 上下文，
 * 这里就用引擎自己的装配语句补上，SPI 只声明了 Jackson 3 一个实现，与运行期完全一致。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowDefinitionImportExportServiceTest {

    /**
     * 一份最小的合法流程定义 JSON：能解析成 {@code DefJson}，因此会真的走到引擎调用
     */
    private static final String VALID_JSON = """
        {"flowCode":"leave","flowName":"请假流程","nodeList":[]}""";

    private final DefService engineDefinitionService = mock(DefService.class);

    private final WorkflowDefinitionManager definitionManager = mock(WorkflowDefinitionManager.class);

    private final WorkflowDefinitionService definitionService =
        new WorkflowDefinitionService(engineDefinitionService, definitionManager);

    @BeforeAll
    static void initJsonConvertLikeTheEngineDoes() {
        FlowEngine.jsonConvert = ServiceLoaderUtil.loadFirst(JsonConvert.class);
    }

    // ==================== 导入 ====================

    @Test
    void importRejectsMissingFile() {
        assertErrorCode("10701", () -> definitionService.importDefinition(null));
        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void importRejectsEmptyFile() {
        assertErrorCode("10701", () -> definitionService.importDefinition(file(new byte[0])));
        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void importRejectsBlankContent() {
        assertErrorCode("10701", () -> definitionService.importDefinition(file("   \n  ")));
        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void importRejectsContentThatIsNotJsonAtAll() {
        assertErrorCode("10701", () -> definitionService.importDefinition(file("这显然不是 JSON")));
        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void importRejectsJsonThatIsNotAnObject() {
        assertErrorCode("10701", () -> definitionService.importDefinition(file("[\"leave\"]")));
        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void importHandsTheUploadedContentToTheEngineAndReturnsTheStoredDefinition() throws Exception {
        FlowDefinition imported = new FlowDefinition();
        imported.setId(9L);
        imported.setFlowCode("leave");
        when(engineDefinitionService.importIs(any(InputStream.class))).thenReturn(imported);

        FlowDefinition stored = new FlowDefinition();
        stored.setId(9L);
        stored.setFlowCode("leave");
        stored.setFlowName("请假流程");
        stored.setVersion("2");
        when(engineDefinitionService.getById(9L)).thenReturn(stored);

        WorkflowDefinitionDTO dto = definitionService.importDefinition(file(VALID_JSON));

        assertThat(dto.getDefinitionId()).isEqualTo(9L);
        assertThat(dto.getFlowName()).isEqualTo("请假流程");
        assertThat(dto.getVersion())
            .as("回查一次是为了带上库里的默认值（版本、发布时间等），与设计器保存的口径一致")
            .isEqualTo("2");

        ArgumentCaptor<InputStream> captor = ArgumentCaptor.forClass(InputStream.class);
        verify(engineDefinitionService).importIs(captor.capture());
        assertThat(new String(captor.getValue().readAllBytes(), StandardCharsets.UTF_8))
            .as("交给引擎的必须是上传文件原文")
            .contains("\"flowCode\":\"leave\"");
    }

    @Test
    void importFallsBackToTheEngineEntityWhenTheDefinitionCannotBeReadBack() {
        FlowDefinition imported = new FlowDefinition();
        imported.setId(9L);
        imported.setFlowCode("leave");
        when(engineDefinitionService.importIs(any(InputStream.class))).thenReturn(imported);
        when(engineDefinitionService.getById(9L)).thenReturn(null);

        assertThat(definitionService.importDefinition(file(VALID_JSON)).getDefinitionId()).isEqualTo(9L);
    }

    @Test
    void importTranslatesEngineRefusalToJsonInvalid() {
        when(engineDefinitionService.importIs(any(InputStream.class)))
            .thenThrow(new FlowException("流程定义不存在!"));

        assertErrorCode("10703", () -> definitionService.importDefinition(file(VALID_JSON)));
    }

    // ==================== 导出 ====================

    @Test
    void exportRejectsNullId() {
        assertErrorCode("10701", () -> definitionService.exportDefinition(null));
        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void exportRejectsMissingDefinitionBeforeTouchingTheEngine() {
        when(definitionManager.getDtoById(404L)).thenReturn(null);

        assertErrorCode("20701", () -> definitionService.exportDefinition(404L));
        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void exportReturnsTheEngineJsonTogetherWithTheFileNamingInfo() {
        when(definitionManager.getDtoById(7L)).thenReturn(definition(7L, "leave", "3"));
        when(engineDefinitionService.exportJson(7L)).thenReturn(VALID_JSON);

        WorkflowDefinitionExportDTO export = definitionService.exportDefinition(7L);

        assertThat(export.getDefinitionId()).isEqualTo(7L);
        assertThat(export.getFlowCode()).isEqualTo("leave");
        assertThat(export.getVersion()).isEqualTo("3");
        assertThat(export.getContent()).isEqualTo(VALID_JSON);
        verify(engineDefinitionService).exportJson(7L);
    }

    @Test
    void exportTranslatesEngineRefusalToEngineError() {
        when(definitionManager.getDtoById(7L)).thenReturn(definition(7L, "leave", "3"));
        when(engineDefinitionService.exportJson(7L)).thenThrow(new FlowException("流程定义不存在!"));

        assertErrorCode("50701", () -> definitionService.exportDefinition(7L));
    }

    @Test
    void exportRejectsBlankEngineOutput() {
        when(definitionManager.getDtoById(7L)).thenReturn(definition(7L, "leave", "3"));
        when(engineDefinitionService.exportJson(7L)).thenReturn("  ");

        assertErrorCode("50701", () -> definitionService.exportDefinition(7L));
    }

    // ==================== 激活 / 挂起 ====================

    @Test
    void activateRejectsNullId() {
        assertErrorCode("10701", () -> definitionService.activate(null));
        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void activateRejectsMissingDefinitionBeforeTouchingTheEngine() {
        when(definitionManager.getDtoById(404L)).thenReturn(null);

        assertErrorCode("20701", () -> definitionService.activate(404L));
        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void activateDelegatesToTheEngineAndReturnsTrue() {
        when(definitionManager.getDtoById(7L)).thenReturn(definition(7L, "leave", "3"));
        when(engineDefinitionService.active(7L)).thenReturn(true);

        assertThat(definitionService.activate(7L)).isTrue();

        verify(engineDefinitionService).active(7L);
    }

    @Test
    void activateTranslatesEngineRefusalToOperationNotAllowed() {
        when(definitionManager.getDtoById(7L)).thenReturn(definition(7L, "leave", "3"));
        when(engineDefinitionService.active(7L)).thenThrow(new FlowException("流程定义已激活!"));

        assertErrorCode("20704", () -> definitionService.activate(7L));
    }

    @Test
    void activateFailsWhenTheEngineReportsFalse() {
        when(definitionManager.getDtoById(7L)).thenReturn(definition(7L, "leave", "3"));
        when(engineDefinitionService.active(7L)).thenReturn(false);

        assertErrorCode("20704", () -> definitionService.activate(7L));
    }

    @Test
    void suspendRejectsNullId() {
        assertErrorCode("10701", () -> definitionService.suspend(null));
        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void suspendRejectsMissingDefinitionBeforeTouchingTheEngine() {
        when(definitionManager.getDtoById(404L)).thenReturn(null);

        assertErrorCode("20701", () -> definitionService.suspend(404L));
        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void suspendDelegatesToTheEngineAndReturnsTrue() {
        when(definitionManager.getDtoById(7L)).thenReturn(definition(7L, "leave", "3"));
        when(engineDefinitionService.unActive(7L)).thenReturn(true);

        assertThat(definitionService.suspend(7L)).isTrue();

        verify(engineDefinitionService).unActive(7L);
    }

    @Test
    void suspendTranslatesEngineRefusalToOperationNotAllowed() {
        when(definitionManager.getDtoById(7L)).thenReturn(definition(7L, "leave", "3"));
        when(engineDefinitionService.unActive(7L)).thenThrow(new FlowException("流程定义已挂起!"));

        assertErrorCode("20704", () -> definitionService.suspend(7L));
    }

    @Test
    void suspendFailsWhenTheEngineReportsFalse() {
        when(definitionManager.getDtoById(7L)).thenReturn(definition(7L, "leave", "3"));
        when(engineDefinitionService.unActive(7L)).thenReturn(false);

        assertErrorCode("20704", () -> definitionService.suspend(7L));
    }

    // ==================== 夹具 ====================

    private static MultipartFile file(String content) {
        return file(content.getBytes(StandardCharsets.UTF_8));
    }

    private static MultipartFile file(byte[] content) {
        return new MockMultipartFile("file", "definition.json", "application/json", content);
    }

    private static WorkflowDefinitionDTO definition(Long definitionId, String flowCode, String version) {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setDefinitionId(definitionId);
        dto.setFlowCode(flowCode);
        dto.setVersion(version);
        return dto;
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
