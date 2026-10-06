package com.carlos.workflow.handler;

import com.carlos.core.auth.CurrentUser;
import com.carlos.core.auth.UserContext;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgDepartment;
import com.carlos.org.api.ApiOrgRole;
import com.carlos.org.api.pojo.ao.OrgDepartmentAO;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.position.api.ApiOrgUserPosition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <p>
 * {@link OrgPermissionHandler} 单元测试：逐条覆盖办理人表达式词汇表（规范 §3）、
 * {@code @@} 并集去重、未知/非法表达式的容错，以及 {@code permissions()} 与
 * {@code convertPermissions()} 的词汇一致性。
 * </p>
 * <p>
 * 全部协作者为 mock：本机没有可达的 org 服务与数据库，本测试只验证「怎么解析、怎么调 org」。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class OrgPermissionHandlerTest {

    private static final String EXPRESSION_SEPARATOR = "@@";

    private ApiOrgRole apiOrgRole;

    private ApiOrgDepartment apiOrgDepartment;

    private ApiOrgUserPosition apiOrgUserPosition;

    private OrgPermissionHandler handler;

    @BeforeEach
    void setUp() {
        apiOrgRole = mock(ApiOrgRole.class);
        apiOrgDepartment = mock(ApiOrgDepartment.class);
        apiOrgUserPosition = mock(ApiOrgUserPosition.class);
        handler = new OrgPermissionHandler(apiOrgRole, apiOrgDepartment, apiOrgUserPosition);
    }

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    // ==================== 静态词汇（规范 §3） ====================

    @Test
    void expandsLiteralUserExpression() {
        assertThat(handler.convertPermissions(List.of("user:1001")))
            .as("user:{id} 为字面量，不调用 org")
            .containsExactly("1001");
        verifyNoInteractions(apiOrgRole, apiOrgDepartment, apiOrgUserPosition);
    }

    @Test
    void expandsCommaSeparatedUserExpression() {
        assertThat(handler.convertPermissions(List.of("user:1001,1002")))
            .as("user:{id,id} 逗号分隔，展开为多个用户 id")
            .containsExactly("1001", "1002");
    }

    @Test
    void expandsRoleExpressionToRoleUsers() {
        when(apiOrgRole.listUsersByRoleId(5L)).thenReturn(userResult(user(11L), user(12L)));

        assertThat(handler.convertPermissions(List.of("role:5")))
            .containsExactly("11", "12");
        verify(apiOrgRole).listUsersByRoleId(5L);
    }

    @Test
    void expandsDeptExpressionToDirectDeptUsers() {
        when(apiOrgDepartment.listUsersByDeptId(12L)).thenReturn(userResult(user(21L), user(22L)));

        assertThat(handler.convertPermissions(List.of("dept:12")))
            .as("dept:{id} 为非递归语义，直接取该部门用户")
            .containsExactly("21", "22");
        verify(apiOrgDepartment).listUsersByDeptId(12L);
    }

    @Test
    void expandsRecursiveDeptExpressionToSubTreeUsers() {
        when(apiOrgDepartment.listUsersByDeptIdRecursive(12L)).thenReturn(userResult(user(21L), user(22L), user(23L)));

        assertThat(handler.convertPermissions(List.of("dept:12:rec")))
            .as("dept:{id}:rec 必须一次调用取回整棵子树的人，而不是逐个子部门各调一次")
            .containsExactly("21", "22", "23");
        verify(apiOrgDepartment).listUsersByDeptIdRecursive(12L);
        verify(apiOrgDepartment, never()).listUsersByDeptId(12L);
    }

    @Test
    void recursiveDeptExpressionIsNotAffectedByEmptySubTree() {
        when(apiOrgDepartment.listUsersByDeptIdRecursive(12L)).thenReturn(Result.success(List.of()));

        assertThat(handler.convertPermissions(List.of("dept:12:rec")))
            .as("子树里没有人时按空展开处理，不报错")
            .isEmpty();
    }

    @Test
    void skipsRecursiveDeptExpressionWithUnknownSecondParam() {
        assertThat(handler.convertPermissions(List.of("dept:12:xxx", "dept:12:rec:extra")))
            .as("只有 rec 是合法标记，其它第二参数一律非法，不猜语义")
            .isEmpty();
        verifyNoInteractions(apiOrgRole, apiOrgDepartment, apiOrgUserPosition);
    }

    @Test
    void expandsDeptLeaderExpression() {
        when(apiOrgDepartment.getDepartmentById(12L)).thenReturn(deptResult(department(12L, 1L, 31L)));

        assertThat(handler.convertPermissions(List.of("deptLeader:12")))
            .as("deptLeader:{id} 取部门负责人")
            .containsExactly("31");
        verify(apiOrgDepartment).getDepartmentById(12L);
    }

    @Test
    void expandsDeptLeaderExpressionWalkingUpLevels() {
        when(apiOrgDepartment.getDepartmentById(12L)).thenReturn(deptResult(department(12L, 99L, 31L)));
        when(apiOrgDepartment.getDepartmentById(99L)).thenReturn(deptResult(department(99L, null, 41L)));

        assertThat(handler.convertPermissions(List.of("deptLeader:12:2")))
            .as("deptLeader:{id}:2 沿 parentId 向上第 2 级取负责人（超时升级常用）")
            .containsExactly("41");
        verify(apiOrgDepartment).getDepartmentById(12L);
        verify(apiOrgDepartment).getDepartmentById(99L);
    }

    @Test
    void deptLeaderLevelOneEqualsPlainDeptLeader() {
        when(apiOrgDepartment.getDepartmentById(12L)).thenReturn(deptResult(department(12L, 99L, 31L)));

        assertThat(handler.convertPermissions(List.of("deptLeader:12:1")))
            .as("n=1 表示自 deptId 起向上第 1 级，即该部门本身，与 deptLeader:12 等价")
            .containsExactly("31");
    }

    @Test
    void returnsEmptyWhenDeptLeaderChainBreaks() {
        when(apiOrgDepartment.getDepartmentById(12L)).thenReturn(deptResult(department(12L, null, 31L)));

        assertThat(handler.convertPermissions(List.of("deptLeader:12:2")))
            .as("上级部门不存在时链断，按空展开处理，不报错")
            .isEmpty();
    }

    @Test
    void returnsEmptyWhenDeptHasNoLeader() {
        when(apiOrgDepartment.getDepartmentById(12L)).thenReturn(deptResult(department(12L, null, null)));

        assertThat(handler.convertPermissions(List.of("deptLeader:12"))).isEmpty();
    }

    @Test
    void returnsEmptyWhenDeptLeaderLevelExceedsLimit() {
        assertThat(handler.convertPermissions(List.of("deptLeader:12:99")))
            .as("层级超出防御上限（环或离谱配置），视为非法表达式")
            .isEmpty();
        verifyNoInteractions(apiOrgDepartment);
    }

    @Test
    void expandsRoleInDeptAsIntersectionOfDeptUsersAndRoleUsers() {
        when(apiOrgDepartment.listUsersByDeptId(12L)).thenReturn(userResult(user(21L), user(22L), user(23L)));
        when(apiOrgRole.listUsersByRoleId(5L)).thenReturn(userResult(user(22L), user(23L), user(24L)));

        assertThat(handler.convertPermissions(List.of("roleInDept:5:12")))
            .as("roleInDept:{roleId}:{deptId} = 部门成员 ∩ 角色持有者，两次调用且无 N+1")
            .containsExactly("22", "23");
    }

    @Test
    void expandsPositionExpressionToPositionUsers() {
        when(apiOrgUserPosition.listUsersByPositionId(7L)).thenReturn(userResult(user(31L), user(32L)));

        assertThat(handler.convertPermissions(List.of("position:7")))
            .as("position:{id} 取该岗位上的全部用户")
            .containsExactly("31", "32");
        verify(apiOrgUserPosition).listUsersByPositionId(7L);
    }

    @Test
    void positionExpressionDoesNotFilterResignedPositionRecordsItself() {
        when(apiOrgUserPosition.listUsersByPositionId(7L))
            .thenReturn(userResult(user(31L, 1), user(32L, 0)));

        assertThat(handler.convertPermissions(List.of("position:7")))
            .as("任职状态由 org 侧决定（不过滤 position_status），处理器只过滤明确禁用的用户账号")
            .containsExactly("31");
    }

    @Test
    void skipsPositionExpressionWithMalformedParams() {
        assertThat(handler.convertPermissions(List.of("position:abc", "position:")))
            .as("position: 只接受单个数字岗位ID")
            .isEmpty();
        verifyNoInteractions(apiOrgRole, apiOrgDepartment, apiOrgUserPosition);
    }

    @Test
    void skipsPositionExpressionWhenOrgCallFails() {
        when(apiOrgUserPosition.listUsersByPositionId(7L))
            .thenThrow(new IllegalStateException("org unavailable"));

        assertThat(handler.convertPermissions(List.of("position:7", "user:1001")))
            .as("org 调用异常只跳过该表达式，不影响同一次请求里其它表达式")
            .containsExactly("1001");
    }

    // ==================== 并集、去重与容错 ====================

    @Test
    void unionsAndDeduplicatesAcrossExpressions() {
        when(apiOrgRole.listUsersByRoleId(5L)).thenReturn(userResult(user(1001L), user(1002L)));
        when(apiOrgDepartment.listUsersByDeptId(12L)).thenReturn(userResult(user(1002L), user(1003L)));

        assertThat(handler.convertPermissions(List.of("user:1001", "role:5", "dept:12")))
            .as("多个表达式取并集后按 userId 去重，保持首次出现顺序")
            .containsExactly("1001", "1002", "1003");
    }

    @Test
    void unionsExpressionsPackedIntoOneElement() {
        when(apiOrgRole.listUsersByRoleId(5L)).thenReturn(userResult(user(11L), user(12L)));

        assertThat(handler.convertPermissions(List.of("user:11" + EXPRESSION_SEPARATOR + "role:5")))
            .as("引擎通常已按 @@ 拆好，本实现额外容忍单个元素内仍含分隔符")
            .containsExactly("11", "12");
    }

    @Test
    void keepsNonVocabularyTokensAsIs() {
        assertThat(handler.convertPermissions(List.of("1002", "${approvers}", "zhangsan")))
            .as("无「类型:参数」形状的 token 是 SpEL / 流程变量求值结果或裸用户 id，原样保留")
            .containsExactly("1002", "${approvers}", "zhangsan");
        verifyNoInteractions(apiOrgRole, apiOrgDepartment, apiOrgUserPosition);
    }

    @Test
    void skipsUnknownVocabularyTypeAndKeepsTheRest() {
        assertThat(handler.convertPermissions(List.of("manager:5", "user:1001")))
            .as("形如「类型:参数」但类型不在词汇表内的标识永远匹配不到办理人，跳过并记 WARN")
            .containsExactly("1001");
        verifyNoInteractions(apiOrgRole, apiOrgDepartment, apiOrgUserPosition);
    }

    @Test
    void skipsMalformedExpressions() {
        assertThat(handler.convertPermissions(List.of(
            "role:abc", "dept:", "user:", "user:1001,abc", "dept:12:rec:extra", "role:5:6", "roleInDept:5")))
            .as("参数非法（非数字、缺参、多参）一律跳过，不影响其它表达式")
            .isEmpty();
        verifyNoInteractions(apiOrgRole, apiOrgDepartment, apiOrgUserPosition);
    }

    @Test
    void returnsEmptyWhenExpansionFindsNobody() {
        when(apiOrgRole.listUsersByRoleId(5L)).thenReturn(Result.success(List.of()));

        assertThat(handler.convertPermissions(List.of("role:5")))
            .as("空展开不是错误：由节点自身的会签/或签规则决定后果")
            .isEmpty();
    }

    @Test
    void returnsEmptyForNullOrBlankInput() {
        assertThat(handler.convertPermissions(null)).isEmpty();
        assertThat(handler.convertPermissions(List.of("  ", ""))).isEmpty();
    }

    @Test
    void filtersDisabledUsersButKeepsUnknownAndLockedStatus() {
        when(apiOrgRole.listUsersByRoleId(5L)).thenReturn(userResult(
            user(11L, 1), user(12L, 0), user(13L, 2), user(14L, null)));

        assertThat(handler.convertPermissions(List.of("role:5")))
            .as("org 不过滤 org_user.user_status：过滤明确禁用（0），保留启用（1）、锁定（2，临时）与未知（null）")
            .containsExactly("11", "13", "14");
    }

    @Test
    void keepsOtherExpressionsWhenOneOrgCallFails() {
        when(apiOrgRole.listUsersByRoleId(5L)).thenThrow(new IllegalStateException("org unavailable"));

        assertThat(handler.convertPermissions(List.of("user:1001", "role:5")))
            .as("org 调用异常只跳过该表达式；整体回退为未转换表达式（引擎兜底）会让所有审批都失败")
            .containsExactly("1001");
    }

    @Test
    void ignoresBlankResponseData() {
        when(apiOrgRole.listUsersByRoleId(5L)).thenReturn(Result.success(null));

        assertThat(handler.convertPermissions(List.of("role:5"))).isEmpty();
    }

    // ==================== 词汇一致性（关键决定） ====================

    @Test
    void permissionsAndConvertPermissionsShareTheUserIdVocabulary() {
        CurrentUser.setCurrentUser(userContext("1001"));

        assertThat(handler.permissions())
            .as("convertPermissions 把节点侧展开成用户 id，permissions() 因此只能返回用户 id")
            .containsExactly("1001");
        assertThat(handler.convertPermissions(List.of("user:1001")))
            .as("两侧词汇相同时交集才非空，checkAuth 才会放行")
            .containsExactlyElementsOf(handler.permissions());
    }

    @Test
    void permissionsReturnOnlyUserIdEvenWhenContextCarriesRolesAndDepts() {
        UserContext context = userContext("1001");
        context.setRoleIds(Set.of("5"));
        context.setDepartmentIds(Set.of("12"));
        CurrentUser.setCurrentUser(context);

        assertThat(handler.permissions())
            .as("角色/部门标识不参与比较：展开后它们不会出现在任务侧，返回它们还可能因 id 撞号误放行")
            .containsExactly("1001");
    }

    @Test
    void permissionsIsEmptyWithoutRequestContext() {
        CurrentUser.clear();

        assertThat(handler.permissions()).isEmpty();
        assertThat(handler.getHandler()).isNull();
    }

    @Test
    void getHandlerReturnsCurrentUserId() {
        CurrentUser.setCurrentUser(userContext("1001"));

        assertThat(handler.getHandler())
            .as("引擎把 getHandler() 写入 create_by / update_by / 审批人")
            .isEqualTo("1001");
    }

    // ==================== 规范 §3 词汇表完整性 ====================

    @Test
    void supportsEveryStaticVocabularyEntryInTheSpec() {
        when(apiOrgRole.listUsersByRoleId(5L)).thenReturn(userResult(user(11L), user(12L)));
        when(apiOrgDepartment.listUsersByDeptId(12L)).thenReturn(userResult(user(12L)));
        when(apiOrgDepartment.listUsersByDeptIdRecursive(12L)).thenReturn(userResult(user(13L)));
        when(apiOrgDepartment.getDepartmentById(12L)).thenReturn(deptResult(department(12L, 99L, 14L)));
        when(apiOrgDepartment.getDepartmentById(99L)).thenReturn(deptResult(department(99L, null, 15L)));
        when(apiOrgUserPosition.listUsersByPositionId(7L)).thenReturn(userResult(user(16L)));

        assertThat(handler.convertPermissions(List.of(
            "user:1001", "user:1002,1003", "role:5", "dept:12", "dept:12:rec",
            "deptLeader:12", "deptLeader:12:2", "position:7", "roleInDept:5:12")))
            .as("规范 §3 的 9 条静态词汇必须全部能展开：此前 dept:12:rec 与 position:7 会打 WARN 并被跳过")
            .containsExactly("1001", "1002", "1003", "11", "12", "13", "14", "15", "16");
    }

    // ==================== 测试数据 ====================

    private static UserContext userContext(String userId) {
        UserContext context = new UserContext();
        context.setUserId(userId);
        return context;
    }

    private static OrgUserAO user(long id) {
        return user(id, 1);
    }

    private static OrgUserAO user(long id, Integer userStatus) {
        OrgUserAO user = new OrgUserAO();
        user.setId(id);
        user.setUserStatus(userStatus);
        return user;
    }

    private static Result<List<OrgUserAO>> userResult(OrgUserAO... users) {
        return Result.success(List.of(users));
    }

    private static OrgDepartmentAO department(Long id, Long parentId, Long leaderId) {
        OrgDepartmentAO department = new OrgDepartmentAO();
        department.setId(id);
        department.setParentId(parentId);
        department.setLeaderId(leaderId);
        return department;
    }

    private static Result<OrgDepartmentAO> deptResult(OrgDepartmentAO department) {
        return Result.success(department);
    }

}
