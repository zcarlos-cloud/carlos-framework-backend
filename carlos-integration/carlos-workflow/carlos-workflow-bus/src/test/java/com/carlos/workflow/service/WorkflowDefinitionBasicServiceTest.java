package com.carlos.workflow.service;

import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionBasicParam;
import com.carlos.workflow.dto.WorkflowDefinitionDTO;
import com.carlos.workflow.exception.WorkflowException;
import com.carlos.workflow.manager.WorkflowDefinitionManager;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 「按基础信息创建 / 更新流程定义」的服务层测试：引擎 Bean 与 Manager 全部 mock，
 * 校验的是服务层的校验规则、分派与错误码翻译，真实 SQL 由
 * {@code com.carlos.workflow.manager.WorkflowDefinitionManagerTest} 用 H2 覆盖。
 * </p>
 * <p>
 * 两条分支的引擎契约（见引擎 1.8.9 源码）：
 * </p>
 * <ul>
 *   <li>新增 → {@code DefService#checkAndSave}：补 version 后仅写主记录，<b>不创建节点</b>；
 *       主键由引擎的 {@code dataFillHandler} 在 save 前补上，因此调用时入参不带 id；</li>
 *   <li>更新 → {@code DefService#updateById}：只改主记录上被显式赋值的列。</li>
 * </ul>
 * <p>
 * {@code modelValue} 是本次改动的核心约束：新建时可缺省或显式给两个合法值，
 * 更新时必须与库中一致——经典与仿钉钉模型的节点结构不同，静默改会毁掉已有节点布局。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowDefinitionBasicServiceTest {

    private static final long CREATED_ID = 11L;

    private static final long EXISTING_ID = 7L;

    private final DefService engineDefinitionService = mock(DefService.class);

    private final WorkflowDefinitionManager definitionManager = mock(WorkflowDefinitionManager.class);

    private final WorkflowDefinitionService definitionService =
        new WorkflowDefinitionService(engineDefinitionService, definitionManager);

    // ==================== 新建 ====================

    @Test
    void createDefaultsModelValueToClassicsAndHandsTheWorkOffToTheEngine() {
        stubEngineCreating();
        when(engineDefinitionService.getById(CREATED_ID)).thenReturn(
            definition(CREATED_ID, "leave", "请假流程", "CLASSICS", "HR"));

        ApiWorkflowDefinitionBasicParam param = basicParam(null, "leave", "请假流程", null, "HR");

        WorkflowDefinitionDTO created = definitionService.createBasic(param);

        assertThat(created.getDefinitionId()).isEqualTo(CREATED_ID);
        assertThat(created.getFlowCode()).isEqualTo("leave");
        assertThat(created.getFlowName()).isEqualTo("请假流程");
        assertThat(created.getModelValue())
            .as("modelValue 缺省即 CLASSICS，且缺省判断在服务层完成，不能指望数据库默认值兜底")
            .isEqualTo("CLASSICS");
        assertThat(created.getCategory()).isEqualTo("HR");
        assertThat(created.getVersion())
            .as("version 由引擎补，返回值取自保存后的回查")
            .isEqualTo("1");

        assertThat(capturedDefinition("checkAndSave"))
            .satisfies(handed -> {
                assertThat(handed.getModelValue()).isEqualTo("CLASSICS");
                assertThat(handed.getCategory()).isEqualTo("HR");
            });
        verifyNoInteractions(definitionManager);
    }

    @Test
    void createLeavesThePrimaryKeyToTheEngineIdGenerator() {
        AtomicReference<Long> idAtCallTime = new AtomicReference<>();
        when(engineDefinitionService.checkAndSave(any(Definition.class))).thenAnswer(invocation -> {
            Definition handed = invocation.getArgument(0);
            idAtCallTime.set(handed.getId());
            handed.setId(CREATED_ID).setVersion("1");
            return true;
        });
        when(engineDefinitionService.getById(CREATED_ID)).thenReturn(
            definition(CREATED_ID, "leave", "请假流程", "CLASSICS", null));

        definitionService.createBasic(basicParam(null, "leave", "请假流程", "CLASSICS", null));

        assertThat(idAtCallTime.get())
            .as("入参必须不带 id：引擎的 save 会在写库前用 dataFillHandler 补主键，"
                + "服务层自己造 id 会绕过宿主的 ID 生成策略（雪花/租户隔离）")
            .isNull();
    }

    @Test
    void createAcceptsExplicitMimicModelAndTrimsTheTextFields() {
        stubEngineCreating();
        when(engineDefinitionService.getById(CREATED_ID)).thenReturn(
            definition(CREATED_ID, "leave", "请假流程", "MIMIC", null));

        definitionService.createBasic(basicParam(null, "  leave  ", " 请假流程 ", "MIMIC", null));

        assertThat(capturedDefinition("checkAndSave"))
            .satisfies(handed -> {
                assertThat(handed.getFlowCode()).isEqualTo("leave");
                assertThat(handed.getFlowName()).isEqualTo("请假流程");
                assertThat(handed.getModelValue()).isEqualTo("MIMIC");
            });
    }

    @Test
    void createRejectsBlankFlowCodeBeforeTouchingTheEngine() {
        assertErrorCode("10701",
            () -> definitionService.createBasic(basicParam(null, "   ", "请假流程", null, null)));

        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void createRejectsMissingFlowNameBeforeTouchingTheEngine() {
        assertErrorCode("10701",
            () -> definitionService.createBasic(basicParam(null, "leave", null, null, null)));

        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void createRejectsIllegalModelValueBeforeTouchingTheEngine() {
        assertErrorCode("10701",
            () -> definitionService.createBasic(basicParam(null, "leave", "请假流程", "MIMICX", null)));

        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void createRejectsNullParam() {
        assertErrorCode("10701", () -> definitionService.createBasic(null));
    }

    @Test
    void createTranslatesEngineFailureToEngineError() {
        when(engineDefinitionService.checkAndSave(any(Definition.class)))
            .thenThrow(new FlowException("Duplicate entry 'leave'"));

        assertErrorCode("50701",
            () -> definitionService.createBasic(basicParam(null, "leave", "请假流程", null, null)));
    }

    @Test
    void createFailsWhenTheEngineReportsFalse() {
        when(engineDefinitionService.checkAndSave(any(Definition.class))).thenReturn(false);

        assertErrorCode("50701",
            () -> definitionService.createBasic(basicParam(null, "leave", "请假流程", null, null)));
    }

    // ==================== 更新 ====================

    @Test
    void updateChangesTheNameAndKeepsTheModelValue() {
        when(definitionManager.getDtoById(EXISTING_ID))
            .thenReturn(existing("leave", "请假流程", "CLASSICS", "HR"));
        when(engineDefinitionService.updateById(any(Definition.class))).thenReturn(true);
        when(engineDefinitionService.getById(EXISTING_ID)).thenReturn(
            definition(EXISTING_ID, "leave", "请假流程V2", "CLASSICS", "ADMIN"));

        WorkflowDefinitionDTO updated = definitionService.updateBasic(
            basicParam(EXISTING_ID, "leave", "请假流程V2", "CLASSICS", "ADMIN"));

        assertThat(updated.getFlowName()).isEqualTo("请假流程V2");
        assertThat(updated.getCategory()).isEqualTo("ADMIN");

        assertThat(capturedDefinition("updateById"))
            .satisfies(handed -> {
                assertThat(handed.getId()).isEqualTo(EXISTING_ID);
                assertThat(handed.getFlowCode()).isEqualTo("leave");
                assertThat(handed.getFlowName()).isEqualTo("请假流程V2");
                assertThat(handed.getCategory()).isEqualTo("ADMIN");
                assertThat(handed.getModelValue()).isEqualTo("CLASSICS");
                assertThat(handed.getVersion())
                    .as("基础信息更新不碰 version：改版本是引擎的设计器保存/复制语义")
                    .isNull();
            });
    }

    @Test
    void updateKeepsStoredValuesWhenTheRequestOmitsThem() {
        when(definitionManager.getDtoById(EXISTING_ID))
            .thenReturn(existing("leave", "请假流程", "CLASSICS", "HR"));
        when(engineDefinitionService.updateById(any(Definition.class))).thenReturn(true);
        when(engineDefinitionService.getById(EXISTING_ID)).thenReturn(
            definition(EXISTING_ID, "leave", "请假流程", "CLASSICS", "HR"));

        definitionService.updateBasic(basicParam(EXISTING_ID, "  ", null, null, null));

        assertThat(capturedDefinition("updateById"))
            .as("留空表示保持原值：主记录这两列非空，用空串覆盖没有意义，"
                + "而传 null 会被 MyBatis-Plus 的默认更新策略忽略、只赋 id 的实体更会拼出空 SET 子句")
            .satisfies(handed -> {
                assertThat(handed.getFlowCode()).isEqualTo("leave");
                assertThat(handed.getFlowName()).isEqualTo("请假流程");
                assertThat(handed.getCategory()).isEqualTo("HR");
                assertThat(handed.getModelValue()).isEqualTo("CLASSICS");
            });
    }

    @Test
    void updateAllowsClearingTheCategoryWithAnEmptyString() {
        when(definitionManager.getDtoById(EXISTING_ID))
            .thenReturn(existing("leave", "请假流程", "CLASSICS", "HR"));
        when(engineDefinitionService.updateById(any(Definition.class))).thenReturn(true);
        when(engineDefinitionService.getById(EXISTING_ID)).thenReturn(
            definition(EXISTING_ID, "leave", "请假流程", "CLASSICS", ""));

        definitionService.updateBasic(basicParam(EXISTING_ID, "leave", "请假流程", null, ""));

        assertThat(capturedDefinition("updateById").getCategory())
            .as("category 允许清空：null 才是「不改」，空串是「改成空」")
            .isEmpty();
    }

    @Test
    void updateRejectsMissingDefinitionBeforeTouchingTheEngine() {
        when(definitionManager.getDtoById(404L)).thenReturn(null);

        assertErrorCode("20701",
            () -> definitionService.updateBasic(basicParam(404L, "leave", "请假流程", null, null)));

        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void updateRejectsAModelValueThatDiffersFromTheStoredOne() {
        when(definitionManager.getDtoById(EXISTING_ID))
            .thenReturn(existing("leave", "请假流程", "CLASSICS", "HR"));

        Throwable thrown = catchThrowable(() -> definitionService.updateBasic(
            basicParam(EXISTING_ID, "leave", "请假流程", "MIMIC", "HR")));

        assertThat(thrown).isInstanceOf(WorkflowException.class);
        assertThat(((WorkflowException) thrown).getErrorCode()).isEqualTo("10701");
        assertThat(thrown.getMessage())
            .as("文案要点明「保存后不可修改」，否则前端只能拿到一个无信息量的参数错误")
            .contains("保存后不可修改");

        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void updateRejectsAnIllegalModelValueEvenWhenItMatchesNothing() {
        when(definitionManager.getDtoById(EXISTING_ID))
            .thenReturn(existing("leave", "请假流程", "CLASSICS", "HR"));

        assertErrorCode("10701", () -> definitionService.updateBasic(
            basicParam(EXISTING_ID, "leave", "请假流程", "MIMICX", "HR")));

        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void updateRejectsNullDefinitionId() {
        assertErrorCode("10701",
            () -> definitionService.updateBasic(basicParam(null, "leave", "请假流程", null, null)));

        verifyNoInteractions(engineDefinitionService);
    }

    @Test
    void updateTreatsAnEngineFalseAsAMissingDefinition() {
        when(definitionManager.getDtoById(EXISTING_ID))
            .thenReturn(existing("leave", "请假流程", "CLASSICS", "HR"));
        when(engineDefinitionService.updateById(any(Definition.class))).thenReturn(false);

        assertErrorCode("20701",
            () -> definitionService.updateBasic(basicParam(EXISTING_ID, "leave", "请假流程", null, null)));
    }

    @Test
    void updateTranslatesEngineFailureToEngineError() {
        when(definitionManager.getDtoById(EXISTING_ID))
            .thenReturn(existing("leave", "请假流程", "CLASSICS", "HR"));
        when(engineDefinitionService.updateById(any(Definition.class)))
            .thenThrow(new FlowException("Connection is closed"));

        assertErrorCode("50701",
            () -> definitionService.updateBasic(basicParam(EXISTING_ID, "leave", "请假流程", null, null)));
    }

    // ==================== 事务 ====================

    @Test
    void bothBasicWriteMethodsOpenATransactionForAnyException() {
        assertTransactional("createBasic");
        assertTransactional("updateBasic");
    }

    /**
     * 写方法必须显式声明 {@code rollbackFor = Exception.class}：只写 {@code @Transactional} 时
     * 默认只回滚 {@code RuntimeException}，而引擎的 {@code saveDef} 会抛受检的 {@code Exception}
     */
    private static void assertTransactional(String methodName) {
        Method method;
        try {
            method = WorkflowDefinitionService.class.getMethod(methodName, ApiWorkflowDefinitionBasicParam.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("WorkflowDefinitionService 缺少方法 " + methodName, e);
        }
        Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);
        assertThat(transactional)
            .as("WorkflowDefinitionService#%s 是写方法，必须开启事务", methodName)
            .isNotNull();
        assertThat(transactional.rollbackFor())
            .as("WorkflowDefinitionService#%s 必须声明 rollbackFor = Exception.class", methodName)
            .contains(Exception.class);
    }

    // ==================== 夹具 ====================

    /**
     * 引擎的 {@code checkAndSave} 会在 save 前补主键与 version，mock 里必须复现这两个副作用，
     * 否则服务层回查用到的 id 永远是 null
     */
    private void stubEngineCreating() {
        when(engineDefinitionService.checkAndSave(any(Definition.class))).thenAnswer(invocation -> {
            Definition handed = invocation.getArgument(0);
            handed.setId(CREATED_ID).setVersion("1");
            return true;
        });
    }

    private Definition capturedDefinition(String engineMethodName) {
        ArgumentCaptor<Definition> captor = ArgumentCaptor.forClass(Definition.class);
        if ("checkAndSave".equals(engineMethodName)) {
            verify(engineDefinitionService).checkAndSave(captor.capture());
        } else {
            verify(engineDefinitionService).updateById(captor.capture());
        }
        return captor.getValue();
    }

    private static ApiWorkflowDefinitionBasicParam basicParam(Long definitionId, String flowCode, String flowName,
                                                              String modelValue, String category) {
        ApiWorkflowDefinitionBasicParam param = new ApiWorkflowDefinitionBasicParam();
        param.setDefinitionId(definitionId);
        param.setFlowCode(flowCode);
        param.setFlowName(flowName);
        param.setModelValue(modelValue);
        param.setCategory(category);
        return param;
    }

    private static WorkflowDefinitionDTO existing(String flowCode, String flowName, String modelValue,
                                                  String category) {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setDefinitionId(EXISTING_ID);
        dto.setFlowCode(flowCode);
        dto.setFlowName(flowName);
        dto.setModelValue(modelValue);
        dto.setCategory(category);
        return dto;
    }

    private static Definition definition(Long id, String flowCode, String flowName, String modelValue,
                                         String category) {
        Definition definition = new FlowDefinition();
        definition.setId(id);
        definition.setFlowCode(flowCode);
        definition.setFlowName(flowName);
        definition.setModelValue(modelValue);
        definition.setCategory(category);
        definition.setVersion("1");
        definition.setIsPublish(0);
        definition.setActivityStatus(1);
        definition.setCreateTime(new Date(0L));
        return definition;
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
