package com.carlos.workflow.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.carlos.workflow.config.WorkflowProperties;
import com.carlos.workflow.exception.WorkflowException;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.invoker.FrameInvoker;
import org.dromara.warm.flow.core.listener.GlobalListener;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.dromara.warm.flow.core.utils.ListenerUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>
 * {@link WorkflowAssigneeFailFastListener} 单元测试：节点配了办理人却解析不到任何人时必须失败，
 * 空 {@code permission_flag}（引擎语义上的「本节点不鉴权」）必须放行。
 * </p>
 * <p>
 * 最后一组用例把监听器真的接到引擎的全局监听器槽位上，用引擎自己的
 * {@code ListenerUtil#executeAssignment}（{@code TaskServiceImpl#skip} 第 216 行、
 * {@code InsServiceImpl#start} 第 100 行调用的就是它）走一遍分派：这既是「异常不会被引擎吞掉」的
 * 运行期证据，也说明拦截点确实在写库之前。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowAssigneeFailFastListenerTest {

    private final WorkflowProperties properties = new WorkflowProperties();

    private final WorkflowAssigneeFailFastListener listener = new WorkflowAssigneeFailFastListener(properties);

    /**
     * 把本用例的监听器接到引擎的全局监听器槽位。
     * <p>
     * 引擎的 {@code FlowEngine#initGlobalListener} 走 {@code FrameInvoker#getBean}，用它的
     * {@code beanFunction} 取 Bean；这里换成一个只认识 {@code GlobalListener} 的函数即可。
     * 不还原也不影响其它用例：真正带引擎的上下文（{@code WorkflowContextStartupTest}）在
     * {@code initFlow} 阶段会自己重设 {@code beanFunction} 与该静态字段。
     * </p>
     */
    @BeforeAll
    static void hookEngineGlobalListenerSlot() {
        WorkflowAssigneeFailFastListener wired =
            new WorkflowAssigneeFailFastListener(new WorkflowProperties());
        FrameInvoker.setBeanFunction(type -> GlobalListener.class.equals(type) ? wired : null);
        FlowEngine.initGlobalListener(null);
        assertThat(FlowEngine.globalListener()).isSameAs(wired);
    }

    // ==================== 拦截：配了办理人却解析不到人 ====================

    @Test
    void assignmentFailsWhenConfiguredAssigneesResolveToNobody() {
        Node next = node("node3", "经理审批", NodeType.BETWEEN.getKey(), "role:abc");
        Task nextTask = task("node3", next);

        Throwable thrown = catchThrowable(() -> listener.assignment(assignmentEvent(next, nextTask)));

        assertThat(thrown).isInstanceOf(WorkflowException.class);
        assertThat(((WorkflowException) thrown).getErrorCode()).isEqualTo("50702");
        assertThat(thrown.getMessage())
            .as("错误消息必须点名节点与配置，否则运维只知道「审批失败」")
            .contains("node3")
            .contains("经理审批")
            .contains("role:abc");
    }

    @Test
    void assignmentFailsWhenOnlyUnresolvedDynamicExpressionsRemain() {
        String expression = "#{@workflowAssignees.upLeader(#applicantId)}";
        Node next = node("node3", "经理审批", NodeType.BETWEEN.getKey(), expression);

        Throwable thrown = catchThrowable(
            () -> listener.assignment(assignmentEvent(next, task("node3", next, List.of(expression)))));

        assertThat(thrown)
            .as("SpEL 求值为空时引擎把表达式原样保留成办理人，谁也匹配不上，同属「配了却没人」")
            .isInstanceOf(WorkflowException.class);
        assertThat(thrown.getMessage()).contains(expression);
    }

    @Test
    void assignmentPassesWhenAtLeastOneExpressionResolved() {
        Node next = node("node3", "经理审批", NodeType.BETWEEN.getKey(), "role:5@@manager:5");

        assertThatCode(() -> listener.assignment(assignmentEvent(next, task("node3", next, List.of("1001")))))
            .as("只要有一个表达式真的解析到人，该节点就不是「无人鉴权」")
            .doesNotThrowAnyException();

        assertThat(catchThrowable(() -> listener.assignment(assignmentEvent(next, task("node3", next, List.of())))))
            .isInstanceOf(WorkflowException.class);
    }

    @Test
    void createFailsForTheSameMisconfigurationAsBackstop() {
        Node node = node("node3", "经理审批", NodeType.BETWEEN.getKey(), "role:abc");
        Task task = task("node3", node, List.of());

        ListenerVariable variable = new ListenerVariable(mock(Definition.class), null, node, Map.of(), task);

        assertThat(catchThrowable(() -> listener.create(variable)))
            .as("create 是兜底钩子：它在写库之后，拦不住坏状态，但也不能无声通过")
            .isInstanceOf(WorkflowException.class);
    }

    // ==================== 放行 ====================

    @Test
    void assignmentPassesWhenEmptyFlagMeansNoAuthorizationByDesign() {
        Node next = node("node3", "经理审批", NodeType.BETWEEN.getKey(), null);

        assertThatCode(() -> listener.assignment(assignmentEvent(next, task("node3", next, List.of()))))
            .as("空 permission_flag 是引擎「本节点不做鉴权」的设计语义，部分流程依赖它，不能一律禁止")
            .doesNotThrowAnyException();

        Node blank = node("node4", "财务审批", NodeType.BETWEEN.getKey(), "  ");
        assertThatCode(() -> listener.assignment(assignmentEvent(blank, task("node4", blank, List.of()))))
            .doesNotThrowAnyException();
    }

    @Test
    void assignmentPassesWhenSomebodyResolved() {
        Node next = node("node3", "经理审批", NodeType.BETWEEN.getKey(), "role:5");

        assertThatCode(() -> listener.assignment(assignmentEvent(next, task("node3", next, List.of("1001", "1002")))))
            .doesNotThrowAnyException();
    }

    @Test
    void assignmentIgnoresNodesWithoutTasksAndEndNodes() {
        Node between = node("node3", "经理审批", NodeType.BETWEEN.getKey(), "role:5");
        Node end = node("node9", "结束", NodeType.END.getKey(), "role:5");

        assertThatCode(() -> listener.assignment(
            new ListenerVariable(mock(Definition.class), null, between, Map.of(), null, List.of(between, end), List.of())))
            .as("没有待创建任务就没有可鉴权的对象")
            .doesNotThrowAnyException();

        Task endTask = task("node9", end, List.of());
        assertThatCode(() -> listener.assignment(
            new ListenerVariable(mock(Definition.class), null, between, Map.of(), null, List.of(end), List.of(endTask))))
            .as("结束节点不产生待办任务（引擎的 create 钩子同样跳过它），不参与判定")
            .doesNotThrowAnyException();

        assertThatCode(() -> listener.create(
            new ListenerVariable(mock(Definition.class), null, end, Map.of(), endTask)))
            .doesNotThrowAnyException();
    }

    @Test
    void assignmentDoesNotRevalidateTheTaskBeingCompleted() {
        Node current = node("node2", "主管审批", NodeType.BETWEEN.getKey(), "role:abc");
        Node next = node("node3", "经理审批", NodeType.BETWEEN.getKey(), "user:1002");
        Task nextTask = task("node3", next, List.of("1002"));

        ListenerVariable variable = new ListenerVariable(mock(Definition.class), null, current, Map.of(),
            task("node2", current, List.of()), List.of(next), List.of(nextTask));

        assertThatCode(() -> listener.assignment(variable))
            .as("分派事件里的 node/task 是「正在办理」的那一对（可能是历史遗留的空办理人任务），"
                + "本次要创建的任务才在 nextNodes/nextTasks 里——只有后者才是拦截对象")
            .doesNotThrowAnyException();
    }

    @Test
    void assignmentPassesWhenAnotherNodeOfTheSameHopIsMisconfiguredAndFailsTheOtherwiseSameHop() {
        Node ok = node("node3", "经理审批", NodeType.BETWEEN.getKey(), "user:1002");
        Node broken = node("node4", "财务复核", NodeType.BETWEEN.getKey(), "dept:99");

        assertThat(catchThrowable(() -> listener.assignment(new ListenerVariable(mock(Definition.class), null, ok,
            Map.of(), null, List.of(ok, broken), List.of(task("node3", ok, List.of("1002")), task("node4", broken, List.of()))))))
            .as("并行分支里只要有一个节点配错就必须失败：放过它等于放过一条无人鉴权的分支")
            .isInstanceOf(WorkflowException.class);
    }

    // ==================== 降级开关 ====================

    @Test
    void downgradesToWarnWhenFailFastIsDisabled() {
        properties.getAssignee().setFailFast(false);
        Node next = node("node3", "经理审批", NodeType.BETWEEN.getKey(), "role:abc");

        Logger logger = (Logger) LoggerFactory.getLogger(WorkflowAssigneeFailFastListener.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatCode(() -> listener.assignment(assignmentEvent(next, task("node3", next, List.of()))))
                .as("carlos.workflow.assignee.fail-fast=false 时降级为仅告警，存量流程可以继续流转")
                .doesNotThrowAnyException();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list)
            .filteredOn(event -> event.getLevel() == Level.WARN)
            .anySatisfy(event -> assertThat(event.getFormattedMessage())
                .contains("node3")
                .contains("fail-fast"));
    }

    @Test
    void failFastIsEnabledByDefault() {
        assertThat(new WorkflowProperties().getAssignee().isFailFast())
            .as("默认必须 fail-closed：漏配的节点不能因为「默认宽松」而变成人人可批")
            .isTrue();
    }

    // ==================== 引擎真的会调用它，且不会吞掉异常 ====================

    @Test
    void engineAssignmentDispatchPropagatesTheFailure() {
        Node next = node("node3", "经理审批", NodeType.BETWEEN.getKey(), "role:abc");

        ListenerVariable variable = new ListenerVariable(mock(Definition.class), null, next, Map.of(),
            null, new ArrayList<>(List.of(next)), new ArrayList<>(List.of(task("node3", next, List.of()))));

        assertThat(catchThrowable(() -> ListenerUtil.executeAssignment(variable)))
            .as("引擎的 ListenerUtil#executeAssignment 里没有 try/catch，异常会一路冒泡回 "
                + "TaskService#skip / InsService#start 的调用方；而它是写库动作（updateFlowInfo / saveFlowInfo）"
                + "之前的那一步，因此失败时本次流转一条语句都还没写")
            .isInstanceOf(WorkflowException.class);
    }

    @Test
    void engineAssignmentDispatchPassesConfiguredAssigneesThrough() {
        Node next = node("node3", "经理审批", NodeType.BETWEEN.getKey(), "role:5");

        ListenerVariable variable = new ListenerVariable(mock(Definition.class), null, next, Map.of(),
            null, new ArrayList<>(List.of(next)), new ArrayList<>(List.of(task("node3", next, List.of("1001")))));

        assertThatCode(() -> ListenerUtil.executeAssignment(variable)).doesNotThrowAnyException();
    }

    // ==================== 测试数据 ====================

    private ListenerVariable assignmentEvent(Node nextNode, Task nextTask) {
        Node current = node("node2", "主管审批", NodeType.BETWEEN.getKey(), null);
        return new ListenerVariable(mock(Definition.class), null, current, Map.of(), null,
            new ArrayList<>(List.of(nextNode)), new ArrayList<>(List.of(nextTask)));
    }

    private static Node node(String code, String name, Integer nodeType, String permissionFlag) {
        Node node = mock(Node.class);
        when(node.getNodeCode()).thenReturn(code);
        when(node.getNodeName()).thenReturn(name);
        when(node.getNodeType()).thenReturn(nodeType);
        when(node.getPermissionFlag()).thenReturn(permissionFlag);
        return node;
    }

    private static Task task(String nodeCode, Node node) {
        return task(nodeCode, node, List.of());
    }

    private static Task task(String nodeCode, Node node, List<String> permissionList) {
        Task task = mock(Task.class);
        when(task.getNodeCode()).thenReturn(nodeCode);
        when(task.getId()).thenReturn(303L);
        when(task.getPermissionList()).thenReturn(permissionList);
        return task;
    }

}
