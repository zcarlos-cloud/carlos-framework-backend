package com.carlos.workflow.handler;

import cn.hutool.core.collection.CollUtil;
import com.carlos.core.response.Result;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.position.api.ApiOrgUserPosition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * 动态办理人的 SpEL 助手 Bean，Bean 名固定为 {@code workflowAssignees}，
 * 供 {@code flow_node.permission_flag} 里以 {@code #{@workflowAssignees.xxx(...)}} 调用。
 * </p>
 * <p>
 * 存在的理由：{@code PermissionHandler#convertPermissions(List)} 的签名<b>没有上下文参数</b>，
 * 拿不到「本流程实例是谁发起的」，所以依赖发起人的语义无法进静态词汇表
 * （见 {@code docs/design/workflow-assignee-expression-spec.md} §4），只能走 SpEL + 流程变量。
 * </p>
 *
 * <h2>Bean 名就是契约</h2>
 * <p>
 * SpEL 的 {@code @beanName} 由 {@code BeanFactoryResolver} 按 <b>Bean 名</b>解析，
 * 而 Bean 名来自 {@code WorkflowAutoConfiguration#workflowAssignees()} 的方法名。
 * <b>改方法名 = 改 SpEL 契约</b>，所有已发布流程定义里的表达式会一起失效，改名必须同步换版。
 * </p>
 *
 * <h2>{@code upLeader}：读申请人自己的 {@code report_to}</h2>
 * <p>
 * 「申请人的直属上级」需要读<b>申请人自己那条</b>任职记录的 {@code org_user_position.report_to}，
 * 即 {@link ApiOrgUserPosition#getSuperiorByUserId(Long)}。注意 {@code report_to} 上另一个方向
 * （{@link ApiOrgUserPosition#listSubordinatesByUserId(Long)}，查 {@code report_to = userId}）
 * 返回的是<b>下属</b>，与本方法相反，不可互相替代。
 * </p>
 * <p>
 * <b>多级</b>由本类循环 {@code levels} 次实现（逐层查上级的上级）。链断时返回空列表：
 * 空返回会让引擎把该表达式<b>原样保留为字面量</b>
 * （{@code ExpressionUtil#evalVariable} 在 SpEL 结果为空时回退），于是该节点谁都批不了（20704）。
 * 这是刻意选择——比按部门负责人顶替（部门负责人 ≠ 汇报线上的上级，申请人本人是部门负责人时
 * 还会把自己指派给自己）更安全，也更容易从 WARN 日志里发现。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class WorkflowAssignees {

    /**
     * 向上解析层数的上限，防御汇报线成环（A 的上级是 B、B 的上级是 A）
     */
    private static final int MAX_UP_LEVELS = 20;

    /**
     * 用户状态：禁用，不可办理
     */
    private static final Integer USER_STATUS_DISABLED = 0;

    private final ApiOrgUserPosition apiOrgUserPosition;

    /**
     * 取申请人的直属上级：{@code #{@workflowAssignees.upLeader(#applicantId)}}
     *
     * @param applicantId 申请人（发起人）用户 id，通常来自流程变量
     * @return 上级用户 id 列表；没有上级时为空列表
     */
    public List<String> upLeader(Long applicantId) {
        return upLeader(applicantId, 1);
    }

    /**
     * 取申请人上 N 级的上级：{@code #{@workflowAssignees.upLeader(#applicantId, 2)}}
     *
     * <p>{@code levels = 1} 即直属上级，{@code levels = 2} 是上级的上级，逐层向上查，
     * 每层只发一次远程调用（不做全量加载）。任一层出现下列情况即中止并返回空列表：
     * 查不到上级（用户没有任职记录、{@code report_to} 未配置）、上级账号已禁用、
     * 或汇报线成环。</p>
     *
     * @param applicantId 申请人（发起人）用户 id
     * @param levels      向上层数，从 1 开始；超过 {@value #MAX_UP_LEVELS} 视为配置异常
     * @return 上级用户 id 列表（最多 1 个）；没有上级时为空列表
     */
    public List<String> upLeader(Long applicantId, int levels) {
        if (applicantId == null || levels < 1 || levels > MAX_UP_LEVELS) {
            log.warn("workflowAssignees.upLeader 参数无效: applicantId={}, levels={}", applicantId, levels);
            return new ArrayList<>();
        }
        Set<Long> visited = new LinkedHashSet<>();
        visited.add(applicantId);
        Long current = applicantId;
        for (int level = 1; level <= levels; level++) {
            OrgUserAO superior = superior(current);
            if (superior == null || superior.getId() == null) {
                log.warn("workflowAssignees.upLeader 链断：用户 [{}] 没有配置上级（已向上 {} 级，目标 {} 级），"
                    + "本次返回空办理人，该节点解析不到人", current, level - 1, levels);
                return new ArrayList<>();
            }
            if (!visited.add(superior.getId())) {
                // 汇报线成环：继续向上只会在环里打转，按链断处理
                log.warn("workflowAssignees.upLeader 链异常：向上解析出现环（用户 [{}] 已出现过），已停止", superior.getId());
                return new ArrayList<>();
            }
            if (USER_STATUS_DISABLED.equals(superior.getUserStatus())) {
                log.warn("workflowAssignees.upLeader 中止：第 {} 级上级 [{}] 的账号已禁用，不能作为办理人",
                    level, superior.getId());
                return new ArrayList<>();
            }
            current = superior.getId();
        }
        return List.of(String.valueOf(current));
    }

    /**
     * 取某人的直属上级（读该用户自己的 {@code report_to}）
     *
     * <p>org 侧异常一律就地降级为 {@code null}（等价于链断），不向 SpEL 求值器抛出：
     * 一次 org 抖动不应该让整条办理人表达式求值失败。口径与
     * {@code OrgPermissionHandler#appendExpression} 的单条失败隔离一致。</p>
     *
     * @param userId 用户 id
     * @return 上级用户；无任职记录、未配置上级或 org 不可用时返回 null
     */
    private OrgUserAO superior(Long userId) {
        try {
            Result<OrgUserAO> result = apiOrgUserPosition.getSuperiorByUserId(userId);
            if (result == null || !Boolean.TRUE.equals(result.getSuccess())) {
                return null;
            }
            return result.getData();
        } catch (Exception e) {
            log.error("workflowAssignees.upLeader 查询用户 [{}] 的上级失败，按链断处理（本次返回空办理人）", userId, e);
            return null;
        }
    }

    /**
     * 取某人的直属下级：{@code #{@workflowAssignees.subordinates(#applicantId)}}
     *
     * <p>这是 {@code report_to} 上另一个方向：按 {@code report_to = userId} 查，
     * 语义为「直接下级」，不递归、不做任职状态过滤，与
     * {@link ApiOrgUserPosition#listSubordinatesByUserId(Long)} 完全一致。</p>
     *
     * @param userId 上级用户 id
     * @return 直接下级用户 id 列表；无下级时为空列表
     */
    public List<String> subordinates(Long userId) {
        if (userId == null) {
            log.warn("workflowAssignees.subordinates 参数无效: userId=null");
            return new ArrayList<>();
        }
        try {
            return userIds(apiOrgUserPosition.listSubordinatesByUserId(userId));
        } catch (Exception e) {
            // 与 upLeader 同一口径：org 异常不得向 SpEL 求值器抛出
            log.error("workflowAssignees.subordinates 查询用户 [{}] 的下级失败（本次返回空办理人）", userId, e);
            return new ArrayList<>();
        }
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

}
