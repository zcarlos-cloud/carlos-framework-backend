package com.carlos.workflow.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.workflow.config.WorkflowProperties;
import com.carlos.workflow.enums.WorkflowErrorCode;
import com.carlos.workflow.handler.WorkflowAssigneeVocabulary.Expression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.listener.GlobalListener;
import org.dromara.warm.flow.core.listener.ListenerVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 办理人<b>失败即拒</b>全局监听器：节点配了办理人，却解析不到任何人时，拒绝创建待办任务并抛出异常，
 * 而不是让引擎静默地把这个节点变成「无人鉴权」。
 * </p>
 *
 * <h2>它防的是什么</h2>
 * <p>
 * 节点办理人写在 {@code flow_node.permission_flag}，展开结果就是待办任务的
 * {@code flow_user.processed_by}。引擎的 {@code TaskServiceImpl#checkAuth} 是
 * {@code AssertUtil.isTrue(CollUtil.isNotEmpty(permissions) && ...)}——注意 {@code AssertUtil#isTrue}
 * 是「为 true 才抛异常」，而 {@code permissions} 取自 {@code flow_user.processed_by}，
 * 因此<b>办理人为空时那一段断言整个短路，交集校验根本不会执行</b>：该节点的审批对任何人都是放行的，
 * 同时它也不会出现在任何人的待办列表里，问题只能靠人去翻 WARN 日志。
 * </p>
 * <p>
 * 另一条路径（SpEL 求值为空）不会变成「人人可批」：引擎的 {@code ExpressionUtil#evalVariable}
 * 在求值结果为空时<b>原样保留表达式字符串</b>，办理人成了一个谁也匹配不上的字面量，结果是「谁也批不了」。
 * 两条路径都由本监听器统一拦下——前者是安全漏洞，后者是配置错误，都不该只留一条 WARN。
 * </p>
 *
 * <h2>为什么挂在 {@code assignment} 而不是 {@code create}</h2>
 * <p>
 * 两个钩子在 1.8.9 里都属「任务创建」，但<b>时序相反</b>，这决定了异常能不能阻止坏状态落库：
 * </p>
 * <ul>
 *   <li>{@code assignment}（{@code ListenerUtil#executeAssignment}）在
 *       {@code TaskServiceImpl#skip} 第 216 行、{@code InsServiceImpl#start} 第 100 行触发，
 *       比写库的 {@code updateFlowInfo}（第 220 行）/ {@code saveFlowInfo}（第 103 行）<b>更早</b>；</li>
 *   <li>{@code create}（{@code ListenerUtil#endCreateListener}）在 {@code skip} 第 231 行、
 *       {@code start} 第 107 行触发，<b>晚于</b>所有写库动作。</li>
 * </ul>
 * <p>
 * 更要紧的是<b>回滚靠不靠得住</b>：Warm-Flow 1.8.9 的 {@code warm-flow-core} 与
 * {@code warm-flow-mybatis-plus-core} 两个 jar 里没有任何 {@code @Transactional}（已逐类核对），
 * 事务边界只能由调用方提供。整份依赖里唯一带 {@code @Transactional} 的是设计器控制器
 * {@code org.dromara.warm.flow.ui.controller.WarmFlowController}（{@code /warm-flow/save-json}、
 * {@code /warm-flow/execute/handle} 等 4 个方法）——那是引擎自带设计器的入口，<b>不是</b>本模块的审批入口
 * （{@code /api/workflow/task/handle} → {@code WorkflowTaskService#skip}）：
 * 本模块的服务层没有开事务。
 * </p>
 * <p>
 * 也就是说：{@code create} 钩子里抛异常时，本次流转的写操作（历史任务、实例状态、新待办、办理人）
 * 早已逐条提交完毕，异常会冒泡到调用方，但<b>能不能回滚取决于调用方走的是哪个入口</b>——
 * 从设计器端点进来有事务，从本模块端点进来没有。拦截控制不能依赖这种外部条件，
 * 所以真正的拦截点是 {@code assignment}：在那里失败，本次流转<b>一条语句都还没写</b>，
 * 与调用方是否开事务无关。
 * </p>
 * <p>
 * 关于「异常会不会被引擎吞掉」：{@code ListenerUtil#executeListener} 直接调用
 * {@code FlowEngine.globalListener().notify(...)}，没有任何 try/catch；{@code skip} / {@code start}
 * 内部也没有 try/catch。异常因此会原样冒泡出 {@code TaskService#skip} / {@code InsService#start}
 * （{@code WorkflowAssigneeFailFastListenerTest} 里用引擎自己的 {@code ListenerUtil#executeAssignment}
 * 做了运行期验证）。
 * </p>
 * <p>
 * {@code create} 仍然实现（同样的规则），作为兜底：所有创建路径目前都会先走 {@code assignment}，
 * 一旦将来某条路径只触发 {@code create}，至少还能大声失败，而不是无声地建出一个无人可批的节点。
 * </p>
 *
 * <h2>⚠️ 注册方式：必须走 Spring Bean，{@code warm-flow.global-listener-path} 必须留空</h2>
 * <p>
 * 与 {@link OrgPermissionHandler} 同一条硬约束：{@code FlowEngine.initBean} 对
 * {@code warm-flow.global-listener-path} 的处理是 {@code Class#getConstructor() + newInstance()}，
 * 绕过 Spring 的无参构造；本类依赖 {@link WorkflowProperties}，没有无参构造器，配了该路径只会实例化失败
 * （异常被引擎吞掉后回退到 Spring Bean）。因此：保持 {@code warm-flow.global-listener-path} 为空，
 * 由 {@code WorkflowAutoConfiguration} 以 {@code @Bean + @ConditionalOnMissingBean(GlobalListener.class)} 注册。
 * 引擎只认一个全局监听器，宿主提供自己的实现时本类退让。
 * </p>
 *
 * <h2>开关（{@value #FAIL_FAST_PROPERTY}，默认 true）</h2>
 * <p>
 * 置为 false 时降级为 WARN 日志、不抛异常，让存量流程能继续流转。⚠️ <b>降级是有安全代价的</b>：
 * 该节点会回到本类要修的那个状态——办理人为空、{@code checkAuth} 不拦任何人，等于该节点的审批
 * 对所有人开放。降级只应用于「先让业务跑通、再逐个修节点配置」的过渡期。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class WorkflowAssigneeFailFastListener implements GlobalListener {

    /**
     * 降级开关的配置项全名
     */
    public static final String FAIL_FAST_PROPERTY = "carlos.workflow.assignee.fail-fast";

    /**
     * 本类是 {@code Serializable}（{@code GlobalListener} 的要求），而配置对象不是；
     * 引擎只把它当静态单例持有、从不序列化，标记 transient 以免以后真被序列化时踩坑
     */
    private final transient WorkflowProperties properties;

    /**
     * 分派监听器：本节点要往下走、下一批待办即将创建时执行<b>（拦截点）</b>
     *
     * <p>此时写库还没开始，抛异常等于本次流转整体不成立。注意{@code node}/{@code task} 在分派事件里
     * 指的是<b>当前正在办理的</b>节点与任务，本次要创建的任务在 {@code nextNodes}/{@code nextTasks} 里，
     * 因此只校验后者。</p>
     *
     * @param listenerVariable 监听器变量
     */
    @Override
    public void assignment(ListenerVariable listenerVariable) {
        List<Node> nextNodes = listenerVariable.getNextNodes();
        List<Task> nextTasks = listenerVariable.getNextTasks();
        if (CollUtil.isEmpty(nextNodes) || CollUtil.isEmpty(nextTasks)) {
            return;
        }
        for (Node node : nextNodes) {
            verify(node, nextTask(node, nextTasks));
        }
    }

    /**
     * 创建监听器：待办任务已创建时执行（兜底；此时写库已完成，抛异常无法回滚，见类注释）
     *
     * @param listenerVariable 监听器变量
     */
    @Override
    public void create(ListenerVariable listenerVariable) {
        verify(listenerVariable.getNode(), listenerVariable.getTask());
    }

    private void verify(Node node, Task task) {
        if (node == null || task == null || Boolean.TRUE.equals(NodeType.isEnd(node.getNodeType()))) {
            return;
        }
        String permissionFlag = node.getPermissionFlag();
        if (StrUtil.isBlank(permissionFlag)) {
            // 空配置是引擎「本节点不鉴权」的设计语义（且被依赖），只对「配了却解析不到人」判错
            return;
        }
        List<String> assignees = task.getPermissionList();
        if (anybodyResolved(assignees)) {
            return;
        }
        String reason = reason(node, task, permissionFlag, assignees);
        if (!properties.getAssignee().isFailFast()) {
            log.warn("{}；{}={}，本次降级为仅告警、不拒绝创建该待办任务：该节点不会出现在任何人的待办里，"
                    + "且引擎的 checkAuth 对办理人为空的任务不生效——审批对所有人开放，请尽快修正节点配置",
                reason, FAIL_FAST_PROPERTY, false);
            return;
        }
        String failure = reason + "；已拒绝创建该待办任务";
        log.error(failure);
        throw WorkflowErrorCode.WORKFLOW_ASSIGNEE_UNRESOLVED.workflowException(failure);
    }

    /**
     * 展开结果里是否至少有一个能真正参与交集判定的办理人
     *
     * @param assignees 任务的办理人列表（即将写入 {@code flow_user.processed_by}）
     * @return 至少一个非空且不是「求值为空后被引擎原样保留的表达式」时为 true
     */
    private boolean anybodyResolved(List<String> assignees) {
        if (CollUtil.isEmpty(assignees)) {
            return false;
        }
        for (String assignee : assignees) {
            if (StrUtil.isNotBlank(assignee) && !WorkflowAssigneeVocabulary.isDynamicExpression(assignee)) {
                return true;
            }
        }
        return false;
    }

    private String reason(Node node, Task task, String permissionFlag, List<String> assignees) {
        List<String> unresolved = unresolvedExpressions(permissionFlag, assignees);
        return "节点 [" + node.getNodeCode() + " " + StrUtil.blankToDefault(node.getNodeName(), "未命名") + "] "
            + "配置了办理人表达式 [" + permissionFlag + "]，但未解析到任何办理人"
            + (unresolved.isEmpty() ? "" : "（求值为空的表达式：" + unresolved + "）")
            + "（taskId=" + task.getId() + "）"
            + "：办理人为空时引擎既不鉴权任何人、任务也不会出现在任何人的待办里";
    }

    /**
     * 求值为空、被引擎回退保留为表达式字符串的那些 token
     *
     * @param permissionFlag 节点配置
     * @param assignees      展开结果
     * @return 仍带 {@code #}/{@code $} 前缀的展开结果
     */
    private List<String> unresolvedExpressions(String permissionFlag, List<String> assignees) {
        List<String> unresolved = new ArrayList<>();
        if (CollUtil.isNotEmpty(assignees)) {
            for (String assignee : assignees) {
                if (WorkflowAssigneeVocabulary.isDynamicExpression(assignee)) {
                    unresolved.add(assignee);
                }
            }
        }
        if (unresolved.isEmpty()) {
            for (Expression expression : WorkflowAssigneeVocabulary.parse(permissionFlag)) {
                unresolved.add(expression.raw());
            }
        }
        return unresolved;
    }

    private Task nextTask(Node node, List<Task> nextTasks) {
        for (Task task : nextTasks) {
            if (task != null && Objects.equals(node.getNodeCode(), task.getNodeCode())) {
                return task;
            }
        }
        return null;
    }

}
