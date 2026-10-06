package com.carlos.workflow.handler;

import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgDepartment;
import com.carlos.org.api.ApiOrgRole;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.position.api.ApiOrgUserPosition;
import org.dromara.warm.flow.core.constant.FlowCons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <p>
 * <b>设计器 storageId 与运行期办理人词汇表的一致性契约测试</b>——本次改动里最贵的一条契约。
 * </p>
 *
 * <h2>为什么单独一个测试类</h2>
 * <p>
 * 设计器产出的 {@code storageId} 会被 {@code @@} 拼进 {@code flow_node.permission_flag}，
 * 运行期由 {@link WorkflowAssigneeVocabulary} 解析、{@link OrgPermissionHandler} 展开成用户 id。
 * 两侧是两份<b>独立演进</b>的代码：设计器换了写法（或词汇表换了判定），
 * 编译期、其余单测都不会报错——只有真实审批时才会发现「配了办理人却谁也不能办」，
 * 而那时错误已经落到生产流程定义里了。所以这里把「设计器产出 → 词汇表判定 → 展开调用」
 * 整条链路钉死。
 * </p>
 *
 * <h2>断言分三层</h2>
 * <ol>
 *   <li><b>词汇表判定</b>：每个页签产出的写法，落在哪个 {@code Kind}/{@code Type}，
 *       参数是什么、是否被 {@code malformed} 判为非法；</li>
 *   <li><b>保存期校验</b>：{@code invalidExpressions} 必须为空（设计器配出来的东西
 *       不能被自己的校验器拒掉）；</li>
 *   <li><b>运行期展开</b>：把设计器产出的写法喂给 {@link OrgPermissionHandler}，
 *       断言它调用了<b>哪一个</b> org 端点——尤其是 {@code dept:12} 与 {@code dept:12:rec}
 *       必须分别落到「非递归」「递归」两个方法上，这正是拆两个页签的意义。</li>
 * </ol>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class DesignerAssigneeContractTest {

    private ApiOrgRole apiOrgRole;

    private ApiOrgDepartment apiOrgDepartment;

    private ApiOrgUserPosition apiOrgUserPosition;

    private OrgPermissionHandler permissionHandler;

    @BeforeEach
    void setUp() {
        apiOrgRole = mock(ApiOrgRole.class);
        apiOrgDepartment = mock(ApiOrgDepartment.class);
        apiOrgUserPosition = mock(ApiOrgUserPosition.class);
        permissionHandler = new OrgPermissionHandler(apiOrgRole, apiOrgDepartment, apiOrgUserPosition);
    }

    // ==================== 第一层：词汇表判定 ====================

    @Test
    void userTabProducesBareUserIdThatTheVocabularyTreatsAsLiteral() {
        String storageId = DesignerHandlerType.USER.storageId(1001L);

        List<WorkflowAssigneeVocabulary.Expression> expressions = WorkflowAssigneeVocabulary.parse(storageId);

        assertThat(expressions).singleElement().satisfies(expression -> {
            assertThat(expression.kind()).isEqualTo(WorkflowAssigneeVocabulary.Kind.LITERAL);
            assertThat(expression.type()).isNull();
            assertThat(expression.malformed()).isFalse();
            assertThat(WorkflowAssigneeVocabulary.isPlainUserId(expression.raw()))
                .as("裸数字必须被判为「可用作办理人的字面量」，否则保存期校验会把它拒掉")
                .isTrue();
        });
    }

    @Test
    void roleTabProducesKnownRoleExpression() {
        assertKnown(DesignerHandlerType.ROLE, 5L, WorkflowAssigneeVocabulary.Type.ROLE, List.of("5"));
    }

    @Test
    void deptTabProducesKnownNonRecursiveDeptExpression() {
        assertKnown(DesignerHandlerType.DEPT, 12L, WorkflowAssigneeVocabulary.Type.DEPT, List.of("12"));
    }

    @Test
    void deptRecursiveTabProducesKnownRecursiveDeptExpression() {
        WorkflowAssigneeVocabulary.Expression expression =
            assertKnown(DesignerHandlerType.DEPT_RECURSIVE, 12L, WorkflowAssigneeVocabulary.Type.DEPT,
                List.of("12", DesignerStorageId.RECURSIVE_FLAG));

        assertThat(WorkflowAssigneeVocabulary.parse("dept:12:rec"))
            .as("dept:12:rec 若被词汇表判为非法，运行期会静默跳过——设计器的『部门(含下级)』页签就白做了")
            .containsExactly(expression);
    }

    @Test
    void positionTabProducesKnownPositionExpression() {
        assertKnown(DesignerHandlerType.POSITION, 7L, WorkflowAssigneeVocabulary.Type.POSITION, List.of("7"));
    }

    // ==================== 第二层：保存期校验放行 ====================

    @Test
    void savingTheDesignerOutputPassesValidation() {
        for (DesignerHandlerType type : DesignerHandlerType.values()) {
            String storageId = type.storageId(42L);
            assertThat(WorkflowAssigneeVocabulary.invalidExpressions(storageId))
                .as("[%s] 页签产出的 [%s] 必须能通过保存期校验（10705）", type.label(), storageId)
                .isEmpty();
            assertThat(WorkflowAssigneeVocabulary.parse(storageId))
                .as("[%s] 页签产出的 [%s] 必须能被解析成表达式", type.label(), storageId)
                .isNotEmpty();
        }
    }

    // ==================== 第三层：@@ 拼接与拆回 ====================

    @Test
    void joiningSelectionsWithTheEngineSeparatorRoundTripsThroughTheVocabulary() {
        List<String> storageIds = new ArrayList<>();
        for (DesignerHandlerType type : DesignerHandlerType.values()) {
            storageIds.add(type.storageId(42L));
        }

        String permissionFlag = String.join(WorkflowAssigneeVocabulary.EXPRESSION_SEPARATOR, storageIds);

        assertThat(WorkflowAssigneeVocabulary.EXPRESSION_SEPARATOR)
            .as("设计器拼串用的分隔符必须与引擎拆分用的常量是同一个")
            .isEqualTo(FlowCons.SPLIT_AT);
        assertThat(WorkflowAssigneeVocabulary.parse(permissionFlag))
            .as("设计器按 @@ 拼好的 permission_flag，词汇表要能原样拆回同一批 token")
            .extracting(WorkflowAssigneeVocabulary.Expression::raw)
            .containsExactlyElementsOf(storageIds);
        assertThat(WorkflowAssigneeVocabulary.invalidExpressions(permissionFlag)).isEmpty();
    }

    @Test
    void multipleSelectedUsersAreJoinedAndFlattenedBackToUserIds() {
        String permissionFlag = String.join(WorkflowAssigneeVocabulary.EXPRESSION_SEPARATOR,
            DesignerHandlerType.USER.storageId(1001L), DesignerHandlerType.USER.storageId(1002L));

        assertThat(WorkflowAssigneeVocabulary.parse(permissionFlag))
            .as("一次勾选多个用户时，每个用户各自是一个 @@ 表达式")
            .extracting(WorkflowAssigneeVocabulary.Expression::raw)
            .containsExactly("1001", "1002");
    }

    // ==================== 运行期展开：设计器写法必须驱动正确的 org 调用 ====================

    @Test
    void userTabSelectionExpandsToItselfWithoutCallingOrg() {
        String storageId = DesignerHandlerType.USER.storageId(1001L);

        assertThat(permissionHandler.convertPermissions(List.of(storageId)))
            .as("裸用户 id 是字面量，展开即自身——与 permissions() 返回的当前用户 id 逐字相同")
            .containsExactly("1001");
        verifyNoInteractions(apiOrgRole, apiOrgDepartment, apiOrgUserPosition);
    }

    @Test
    void roleTabSelectionExpandsThroughTheRoleEndpoint() {
        when(apiOrgRole.listUsersByRoleId(5L)).thenReturn(users(11L, 12L));

        assertThat(permissionHandler.convertPermissions(List.of(DesignerHandlerType.ROLE.storageId(5L))))
            .containsExactly("11", "12");
        verify(apiOrgRole).listUsersByRoleId(5L);
    }

    @Test
    void deptTabSelectionExpandsThroughTheNonRecursiveEndpoint() {
        when(apiOrgDepartment.listUsersByDeptId(12L)).thenReturn(users(21L, 22L));

        assertThat(permissionHandler.convertPermissions(List.of(DesignerHandlerType.DEPT.storageId(12L))))
            .containsExactly("21", "22");
        verify(apiOrgDepartment).listUsersByDeptId(12L);
        verify(apiOrgDepartment, never()).listUsersByDeptIdRecursive(12L);
    }

    @Test
    void deptRecursiveTabSelectionExpandsThroughTheRecursiveEndpoint() {
        when(apiOrgDepartment.listUsersByDeptIdRecursive(12L)).thenReturn(users(21L, 22L, 23L));

        assertThat(permissionHandler.convertPermissions(List.of(DesignerHandlerType.DEPT_RECURSIVE.storageId(12L))))
            .as("『部门(含下级)』页签与『部门』页签的唯一区别就在这一次调用的口径上")
            .containsExactly("21", "22", "23");
        verify(apiOrgDepartment).listUsersByDeptIdRecursive(12L);
        verify(apiOrgDepartment, never()).listUsersByDeptId(12L);
    }

    @Test
    void theTwoDeptTabsDoNotCollapseIntoTheSameExpression() {
        String plain = DesignerHandlerType.DEPT.storageId(12L);
        String recursive = DesignerHandlerType.DEPT_RECURSIVE.storageId(12L);

        assertThat(plain).isNotEqualTo(recursive);
        assertThat(WorkflowAssigneeVocabulary.parse(plain).get(0).params())
            .as("非递归形态只有一个参数")
            .hasSize(1);
        assertThat(WorkflowAssigneeVocabulary.parse(recursive).get(0).params())
            .as("递归形态的第二个参数是 rec 标记")
            .containsExactly("12", DesignerStorageId.RECURSIVE_FLAG);
    }

    @Test
    void positionTabSelectionExpandsThroughTheUserPositionEndpoint() {
        when(apiOrgUserPosition.listUsersByPositionId(7L)).thenReturn(users(31L, 32L));

        assertThat(permissionHandler.convertPermissions(List.of(DesignerHandlerType.POSITION.storageId(7L))))
            .as("设计器列的是 org_position 的主键，展开走 org_user_position.position_id，两者同一 id 空间")
            .containsExactly("31", "32");
        verify(apiOrgUserPosition).listUsersByPositionId(7L);
    }

    @Test
    void aNodeConfiguredFromEveryTabExpandsToTheUnionOfAllSelectedHandlers() {
        when(apiOrgRole.listUsersByRoleId(5L)).thenReturn(users(11L));
        when(apiOrgDepartment.listUsersByDeptId(12L)).thenReturn(users(21L));
        when(apiOrgDepartment.listUsersByDeptIdRecursive(13L)).thenReturn(users(22L));
        when(apiOrgUserPosition.listUsersByPositionId(7L)).thenReturn(users(31L));

        List<String> permissionFlag = List.of(String.join(WorkflowAssigneeVocabulary.EXPRESSION_SEPARATOR,
            DesignerHandlerType.USER.storageId(1001L),
            DesignerHandlerType.ROLE.storageId(5L),
            DesignerHandlerType.DEPT.storageId(12L),
            DesignerHandlerType.DEPT_RECURSIVE.storageId(13L),
            DesignerHandlerType.POSITION.storageId(7L)));

        assertThat(permissionHandler.convertPermissions(permissionFlag))
            .as("五个页签各自配一个办理人时，展开结果是并集且按首次出现顺序")
            .containsExactly("1001", "11", "21", "22", "31");
    }

    // ==================== 测试数据 ====================

    private static WorkflowAssigneeVocabulary.Expression assertKnown(DesignerHandlerType type, Long id,
                                                                    WorkflowAssigneeVocabulary.Type expectedType,
                                                                    List<String> expectedParams) {
        String storageId = type.storageId(id);
        assertThat(storageId).isNotNull();
        assertThat(WorkflowAssigneeVocabulary.parse(storageId))
            .as("[%s] 页签产出的 [%s] 必须被词汇表判为已知类型", type.label(), storageId)
            .singleElement()
            .satisfies(expression -> {
                assertThat(expression.kind()).isEqualTo(WorkflowAssigneeVocabulary.Kind.KNOWN);
                assertThat(expression.type()).isEqualTo(expectedType);
                assertThat(expression.params()).containsExactlyElementsOf(expectedParams);
                assertThat(expression.malformed())
                    .as("[%s] 的参数形状必须合法，否则运行期会被直接跳过", storageId)
                    .isFalse();
            });
        return WorkflowAssigneeVocabulary.parse(storageId).get(0);
    }

    private static Result<List<OrgUserAO>> users(long... ids) {
        List<OrgUserAO> users = new ArrayList<>();
        for (long id : ids) {
            OrgUserAO user = new OrgUserAO();
            user.setId(id);
            user.setUserStatus(1);
            users.add(user);
        }
        return Result.success(users);
    }

}
