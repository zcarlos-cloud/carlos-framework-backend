package com.carlos.workflow.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.core.auth.CurrentUser;
import com.carlos.core.auth.UserContext;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgDepartment;
import com.carlos.org.api.ApiOrgRole;
import com.carlos.org.api.pojo.ao.OrgDepartmentAO;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.position.api.ApiOrgUserPosition;
import com.carlos.workflow.handler.WorkflowAssigneeVocabulary.Expression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.handler.PermissionHandler;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * 办理人权限处理器：把节点上配置的办理人表达式展开成<b>用户 id</b>。
 * </p>
 *
 * <h2>词汇一致性（本实现最关键的一个决定）</h2>
 * <p>
 * 引擎的 {@code TaskServiceImpl#checkAuth} 做的是<b>交集</b>判定，比较的两侧分别是
 * {@code PermissionHandler#permissions()}（当前用户）与任务的 {@code flow_user.processed_by}
 * （节点办理人），而后者是 {@link #convertPermissions(List)} <b>展开之后</b>的结果。
 * 两侧词汇必须一致，否则交集恒为空，所有真实审批都以 20704 失败。
 * </p>
 * <p>
 * 本实现选择<b>全量转用户 id</b>（规范里的方案 A）：
 * </p>
 * <ul>
 *   <li>{@link #convertPermissions(List)} 把 {@code role:5} / {@code dept:12} 等展开成用户 id；</li>
 *   <li>{@link #permissions()} 因此只返回<b>当前用户的用户 id</b>，不返回角色/部门标识——
 *       展开后不会再有 {@code role:5} 这类标识参与比较，返回它们既无意义，
 *       又可能因「角色 id 恰好等于某个用户 id」而误放行。</li>
 * </ul>
 * <p>
 * 展开必须依赖 org 数据，而 {@code convertPermissions} 是引擎唯一能拿到该数据的位置，
 * 这是选择方案 A 的根本原因。
 * </p>
 *
 * <h2>支持的表达式（规范 §3 静态词汇）</h2>
 * <table border="1">
 *   <caption>表达式与来源</caption>
 *   <tr><th>写法</th><th>展开</th><th>org 契约</th></tr>
 *   <tr><td>{@code user:1001}</td><td>字面量</td><td>不需要调用</td></tr>
 *   <tr><td>{@code user:1001,1002}</td><td>字面量</td><td>不需要调用</td></tr>
 *   <tr><td>{@code role:5}</td><td>角色下用户</td><td>{@link ApiOrgRole#listUsersByRoleId}</td></tr>
 *   <tr><td>{@code dept:12}</td><td>部门内用户（不含子部门）</td><td>{@link ApiOrgDepartment#listUsersByDeptId}</td></tr>
 *   <tr><td>{@code dept:12:rec}</td><td>部门及全部子部门内的用户</td><td>{@link ApiOrgDepartment#listUsersByDeptIdRecursive}</td></tr>
 *   <tr><td>{@code deptLeader:12}</td><td>部门负责人</td><td>{@link ApiOrgDepartment#getDepartmentById}</td></tr>
 *   <tr><td>{@code deptLeader:12:2}</td><td>向上第 2 级部门负责人</td><td>同上，逐级解析 {@code parentId}</td></tr>
 *   <tr><td>{@code position:7}</td><td>岗位上的全部用户</td><td>{@link ApiOrgUserPosition#listUsersByPositionId}</td></tr>
 *   <tr><td>{@code roleInDept:5:12}</td><td>部门 12 内持有角色 5 的人</td><td>部门用户 ∩ 角色用户，两次调用</td></tr>
 * </table>
 * <p>规范 §3 的静态词汇至此<b>全部支持</b>。依赖发起人的词汇（规范 §4：
 * {@code upLeader} / {@code upLeader:2}）<b>不在本处理器内</b>——本类的
 * {@link #convertPermissions(List)} 拿不到流程上下文，只能由 SpEL 助手 Bean
 * {@code @workflowAssignees} 承担（见 {@link WorkflowAssignees}）。</p>
 *
 * <h2>取值口径</h2>
 * <ul>
 *   <li><b>部门递归</b>：{@code dept:12:rec} 的部门集合由 org 侧展开（一次远程往返），
 *       本处理器只发一次调用，不为每个子部门各调一次；</li>
 *   <li><b>任职状态</b>：{@code position:7} 与 {@code dept:12:rec} 都不按
 *       {@code org_user_position.position_status} 过滤（与 org 侧
 *       {@code listByReportTo} 口径一致，避免漏掉试用期人员），只过滤明确禁用的用户账号；</li>
 *   <li><b>未知类型与非法参数</b>：形如 {@code 类型:参数} 但类型不在词汇表内、
 *       或参数不合法（非数字、缺参、多参）时<b>记 WARN 日志并跳过</b>，
 *       不影响同一次请求里其它表达式的解析，也不阻断审批。类型识别与参数校验在
 *       {@link WorkflowAssigneeVocabulary} ——本类不再自己维护一份类型表。</li>
 * </ul>
 * <p>
 * ⚠️ <b>跳过的实际后果</b>：被跳过的表达式、以及其余也没解析到人的表达式合起来为空时，
 * 任务的 {@code flow_user.processed_by} 为空，而引擎的 {@code checkAuth} 只在「任务办理人非空」
 * 时才做交集校验——也就是说该节点<b>不再被引擎鉴权拦住任何人</b>，同时它也不会出现在任何人的
 * 待办列表里。这与 SpEL 路径（{@code @workflowAssignees} 返回空 → 引擎回退为原样保留表达式字符串
 * → 谁也匹配不上）的后果不同。
 * </p>
 * <p>
 * <b>这两条路径现已由 {@link WorkflowAssigneeFailFastListener} 在创建待办前拦下</b>：节点配了办理人
 * 却展开为空时，任务不再被创建，而是直接抛出异常（{@code carlos.workflow.assignee.fail-fast=false}
 * 可降级为仅告警）。本类保持「解析不到人就返回空列表」的语义不变，判断「空列表算不算错」的职责
 * 归监听器——这样处理器只负责展开，是否符合节点意图由流转层决定。
 * </p>
 *
 * <h2>其它语义</h2>
 * <ul>
 *   <li><b>并集去重</b>：多个表达式（引擎按 {@code @@} 拆分后逐个传入，
 *       本实现额外容忍单个元素内仍含分隔符）取并集后按 userId 去重，保持首次出现顺序；</li>
 *   <li><b>空展开不是错误</b>：解析不到人时返回空列表，由节点自身的会签/或签规则决定后果
 *       （「算不算配置错误」由 {@link WorkflowAssigneeFailFastListener} 判定）；</li>
 *   <li><b>不认识就不猜</b>：不属于词汇表形状的 token（SpEL 求值结果、流程变量值、裸用户 id）
 *       原样保留，交由引擎比较；形如 {@code 类型:参数} 但类型不在词汇表内则跳过并记 WARN——
 *       这类标识参与交集永远不会命中，原样保留只会把配置错误藏起来；</li>
 *   <li><b>禁用用户</b>：org 侧不过滤 {@code org_user.user_status}，本实现过滤掉明确禁用的用户
 *       （状态 0）；状态为 1（启用）、2（锁定，临时状态）或 null（未知）均保留；</li>
 *   <li><b>租户</b>：当前一律走无租户变体（{@code X-Tenant-Id} 由 FeignRequestInterceptor 从当前
 *       请求透传）。后台任务/超时升级等<b>无请求上下文</b>的链路会静默回落到平台租户，
 *       届时需改用 {@code ...AndTenant} 变体并显式传入 tenantId。</li>
 * </ul>
 *
 * <h2>⚠️ 注册方式：必须走 Spring Bean，{@code warm-flow.permission-handler-path} 必须留空</h2>
 * <p>
 * {@code FlowEngine.initBean} 对 {@code warm-flow.permission-handler-path} 的处理是
 * {@code Class#getConstructor()} + {@code newInstance()}，即<b>绕过 Spring 的无参构造</b>。
 * 本类依赖 org Feign 客户端注入，没有无参构造器，配了该路径只会实例化失败（异常被引擎吞掉后
 * 回退到 Spring Bean）。因此：
 * </p>
 * <ul>
 *   <li>保持 {@code warm-flow.permission-handler-path} <b>为空</b>；</li>
 *   <li>由 {@code WorkflowAutoConfiguration} 以 {@code @Bean + @ConditionalOnMissingBean} 注册，
 *       宿主可提供自己的 {@code PermissionHandler} 覆盖本实现。</li>
 * </ul>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class OrgPermissionHandler implements PermissionHandler {

    /**
     * 用户状态：禁用，不可办理
     */
    private static final Integer USER_STATUS_DISABLED = 0;

    private final ApiOrgRole apiOrgRole;

    private final ApiOrgDepartment apiOrgDepartment;

    private final ApiOrgUserPosition apiOrgUserPosition;

    /**
     * 当前用户的权限标识。
     *
     * <p><b>只返回用户 id</b>：节点侧已由 {@link #convertPermissions(List)} 展开为同一种词汇，
     * 详见类注释「词汇一致性」。未登录（无请求上下文）时返回空列表。</p>
     *
     * @return 当前用户的用户 id 列表
     */
    @Override
    public List<String> permissions() {
        String userId = currentUserId();
        return StrUtil.isEmpty(userId) ? new ArrayList<>() : List.of(userId);
    }

    /**
     * 当前办理人（用户 id），引擎写入 {@code create_by} / {@code update_by} / 审批人。
     *
     * @return 当前用户 id；未登录时为 null（引擎会退回实体自身的值）
     */
    @Override
    public String getHandler() {
        return currentUserId();
    }

    /**
     * 把节点办理人表达式展开为去重后的用户 id 列表。
     *
     * @param permissions 引擎传入的表达式列表，元素形如 {@code role:5}、{@code dept:12:rec}
     * @return 展开后的用户 id 列表；入参为空时返回空列表，展开不到人时不报错
     */
    @Override
    public List<String> convertPermissions(List<String> permissions) {
        Set<String> resolved = new LinkedHashSet<>();
        for (Expression expression : WorkflowAssigneeVocabulary.parseAll(permissions)) {
            appendExpression(resolved, expression);
        }
        return new ArrayList<>(resolved);
    }

    /**
     * 解析单条表达式，失败只丢弃这一条
     *
     * @param resolved   累积结果
     * @param expression 单条表达式
     */
    private void appendExpression(Set<String> resolved, Expression expression) {
        try {
            resolveExpression(resolved, expression);
        } catch (Exception e) {
            // org 侧不可用等异常只跳过这一条：引擎对 convertPermissions 的兜底是「整体回退为未转换的
            // 表达式」，那样 role:5 这类标识永远匹配不到用户 id，会让所有审批失败——比丢掉一条更糟。
            log.error("办理人表达式 [{}] 解析失败，已跳过该表达式", expression.raw(), e);
        }
    }

    /**
     * 按类型分派单条表达式
     *
     * @param resolved   累积结果
     * @param expression 单条表达式
     */
    private void resolveExpression(Set<String> resolved, Expression expression) {
        switch (expression.kind()) {
            case LITERAL -> {
                // 无「类型:参数」形状：SpEL 求值结果、流程变量值或裸用户 id，原样交给引擎比较
                resolved.add(expression.raw());
            }
            case UNKNOWN_TYPE -> log.warn("办理人表达式 [{}] 的类型不在规范词汇表内，已跳过"
                + "（该标识参与交集永远不会命中办理人）", expression.raw());
            case KNOWN -> {
                if (expression.malformed()) {
                    log.warn("办理人表达式 [{}] 参数不合法（合法写法见 workflow-assignee-expression-spec.md §3），已跳过",
                        expression.raw());
                    return;
                }
                // 参数合法性已由词汇表判定，这里只做 org 侧展开；枚举 switch 无 default，
                // 新增 Type 时编译器会在这里报错，避免展开侧漏实现一个新词
                switch (expression.type()) {
                    case USER -> resolved.addAll(resolveUser(expression));
                    case ROLE -> resolved.addAll(resolveRole(expression));
                    case DEPT -> resolved.addAll(resolveDept(expression));
                    case DEPT_LEADER -> resolved.addAll(resolveDeptLeader(expression));
                    case ROLE_IN_DEPT -> resolved.addAll(resolveRoleInDept(expression));
                    case POSITION -> resolved.addAll(resolvePosition(expression));
                }
            }
        }
    }

    /**
     * {@code user:1001} / {@code user:1001,1002}：字面量，直接转成规范的用户 id 字符串。
     *
     * <p>刻意不校验用户是否存在：这是纯字面量展开，多一次 org 往返只会多一个失败点
     * （org 不可用时降级工厂返回空列表，会把点名指派的办理人也一并丢掉）。</p>
     *
     * @param expression 表达式
     * @return 用户 id 列表
     */
    private List<String> resolveUser(Expression expression) {
        List<String> userIds = new ArrayList<>();
        for (Long userId : WorkflowAssigneeVocabulary.userIds(expression.params().get(0))) {
            userIds.add(String.valueOf(userId));
        }
        return userIds;
    }

    /**
     * {@code role:5}：角色下的用户
     *
     * @param expression 表达式
     * @return 用户 id 列表
     */
    private List<String> resolveRole(Expression expression) {
        return userIds(apiOrgRole.listUsersByRoleId(WorkflowAssigneeVocabulary.id(expression.params().get(0))));
    }

    /**
     * {@code dept:12}（部门内用户，不含子部门）；{@code dept:12:rec}（部门及其全部子部门）
     *
     * <p>递归形式把部门集合的展开交给 org 侧完成：本方法只发<b>一次</b>远程调用，
     * 不为每个子部门各调一次 {@code listUsersByDeptId}。除 {@code rec} 之外的第二个参数
     * 一律视为非法（不猜测语义），该判定在词汇表里完成。</p>
     *
     * @param expression 表达式
     * @return 用户 id 列表
     */
    private List<String> resolveDept(Expression expression) {
        Long deptId = WorkflowAssigneeVocabulary.id(expression.params().get(0));
        if (expression.params().size() == 2) {
            return userIds(apiOrgDepartment.listUsersByDeptIdRecursive(deptId));
        }
        return userIds(apiOrgDepartment.listUsersByDeptId(deptId));
    }

    /**
     * {@code position:7}：岗位 7 上的全部用户
     *
     * <p>与 {@code role:5} 一样是「单参数 + 一次 org 调用」的形状。org 侧不过滤
     * {@code org_user_position.position_status}（见 {@code ApiOrgUserPosition#listUsersByPositionId}），
     * 本方法只按 {@link #userIds(Result)} 的统一口径过滤明确禁用的用户账号。</p>
     *
     * @param expression 表达式
     * @return 用户 id 列表
     */
    private List<String> resolvePosition(Expression expression) {
        return userIds(apiOrgUserPosition.listUsersByPositionId(WorkflowAssigneeVocabulary.id(expression.params().get(0))));
    }

    /**
     * {@code deptLeader:12} / {@code deptLeader:12:2}：部门负责人；带层级时沿 {@code parentId} 向上取。
     *
     * <p>层级语义：{@code n} 表示「自 deptId 起向上第 n 级」，{@code n=1} 即 deptId 本身，
     * 因此 {@code deptLeader:12} 等价于 {@code deptLeader:12:1}。链断（上级不存在）时返回空列表。
     * 层级取值范围由词汇表校验，这里不再重复判定。</p>
     *
     * @param expression 表达式
     * @return 用户 id 列表
     */
    private List<String> resolveDeptLeader(Expression expression) {
        Long targetDeptId = WorkflowAssigneeVocabulary.id(expression.params().get(0));
        int levels = Objects.requireNonNullElse(expression.params().size() == 2
            ? WorkflowAssigneeVocabulary.level(expression.params().get(1)) : 1, 1);
        for (int level = 1; level < levels; level++) {
            OrgDepartmentAO ancestor = department(targetDeptId);
            if (ancestor == null || ancestor.getParentId() == null) {
                log.warn("办理人表达式 [{}]：部门 [{}] 之上没有第 [{}] 级部门，已跳过",
                    expression.raw(), targetDeptId, level + 1);
                return new ArrayList<>();
            }
            targetDeptId = ancestor.getParentId();
        }
        OrgDepartmentAO target = department(targetDeptId);
        if (target == null || target.getLeaderId() == null) {
            log.warn("办理人表达式 [{}]：部门 [{}] 没有配置负责人，已跳过", expression.raw(), targetDeptId);
            return new ArrayList<>();
        }
        return List.of(String.valueOf(target.getLeaderId()));
    }

    /**
     * {@code roleInDept:5:12}：部门 12 内持有角色 5 的人
     *
     * <p>实现为「部门直接成员」与「角色持有者」两个集合求交，共两次 org 调用，
     * 避免逐用户查询角色（N+1）。因此本表达式的语义是「在部门 12 且持有角色 5」，
     * 不区分该角色授权是部门内授予（{@code org_user_role.dept_id}）还是全局授予。</p>
     *
     * @param expression 表达式
     * @return 用户 id 列表
     */
    private List<String> resolveRoleInDept(Expression expression) {
        Long roleId = WorkflowAssigneeVocabulary.id(expression.params().get(0));
        Long deptId = WorkflowAssigneeVocabulary.id(expression.params().get(1));
        Set<String> deptUserIds = new LinkedHashSet<>(userIds(apiOrgDepartment.listUsersByDeptId(deptId)));
        deptUserIds.retainAll(new LinkedHashSet<>(userIds(apiOrgRole.listUsersByRoleId(roleId))));
        return new ArrayList<>(deptUserIds);
    }

    /**
     * 取用户 id 集合：处理空响应与禁用用户
     *
     * @param result org 契约返回的统一响应
     * @return 用户 id 列表
     */
    private List<String> userIds(Result<List<OrgUserAO>> result) {
        if (result == null || !Boolean.TRUE.equals(result.getSuccess()) || CollUtil.isEmpty(result.getData())) {
            return new ArrayList<>();
        }
        return result.getData().stream()
            .filter(Objects::nonNull)
            .filter(user -> user.getId() != null)
            .filter(user -> !USER_STATUS_DISABLED.equals(user.getUserStatus()))
            .map(user -> String.valueOf(user.getId()))
            .distinct()
            .toList();
    }

    /**
     * 按 id 查部门，失败或不存在返回 null
     *
     * @param deptId 部门 id
     * @return 部门；查不到时为 null
     */
    private OrgDepartmentAO department(Long deptId) {
        Result<OrgDepartmentAO> result = apiOrgDepartment.getDepartmentById(deptId);
        if (result == null || !Boolean.TRUE.equals(result.getSuccess())) {
            return null;
        }
        return result.getData();
    }

    /**
     * 当前登录用户 id
     *
     * @return 用户 id；无请求上下文时为 null
     */
    private String currentUserId() {
        UserContext userContext = CurrentUser.getCurrentUser();
        return userContext == null ? null : userContext.getUserId();
    }

}
