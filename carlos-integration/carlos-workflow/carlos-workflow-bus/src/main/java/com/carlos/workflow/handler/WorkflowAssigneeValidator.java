package com.carlos.workflow.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.workflow.enums.WorkflowErrorCode;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.dto.NodeJson;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 流程定义保存前的办理人表达式校验：逐个节点把 {@code permissionFlag} 对一遍词汇表
 * （{@link WorkflowAssigneeVocabulary}），有解析不了的表达式就整个请求失败，错误消息里列出
 * 节点编码与表达式，方便一次改完。
 * </p>
 *
 * <h2>⚠️ 这不是硬闸门，真正生效的是 fail-fast 监听器</h2>
 * <p>
 * 本校验只挂在 {@code POST /api/workflow/definition} 上。<b>设计器不走这个接口</b>——
 * SPA 直接调 Warm-Flow 自带的 {@code POST /warm-flow/save-json}（设计器数据接口，随
 * {@code warm-flow-plugin-ui-sb-web} 引入，本模块不包装也不拦截），因此定义<b>可以绕过本校验</b>到达引擎。
 * 而且即便本接口全部拦住，也只是「保存时」的问题：存量定义、DB 直改、以及 SpEL 求值本身就依赖运行时数据
 * （{@code #{@workflowAssignees.upLeader(#applicantId)}} 是否取得到人，保存期无从判断）。
 * </p>
 * <p>
 * 所以本类的定位是<b>尽力而为的前置提示</b>，防的是「静态词汇写错」这类一眼可见的错误；
 * 真正保证「配了办理人却解析不到人时不会静默变成无人鉴权节点」的是
 * {@link WorkflowAssigneeFailFastListener}（运行期，任务创建前拦截）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
public final class WorkflowAssigneeValidator {

    private WorkflowAssigneeValidator() {
    }

    /**
     * 校验定义 JSON 里所有节点的办理人表达式
     *
     * @param defJson 已解析的设计器定义 JSON
     * @throws com.carlos.workflow.exception.WorkflowException 存在无法解析的表达式（错误码 10705）
     */
    public static void validate(DefJson defJson) {
        if (defJson == null || CollUtil.isEmpty(defJson.getNodeList())) {
            return;
        }
        List<String> problems = new ArrayList<>();
        for (NodeJson node : defJson.getNodeList()) {
            if (node == null) {
                continue;
            }
            List<String> invalid = WorkflowAssigneeVocabulary.invalidExpressions(node.getPermissionFlag());
            if (CollUtil.isNotEmpty(invalid)) {
                problems.add(nodeLabel(node) + " 表达式[" + String.join("、", invalid) + "]");
            }
        }
        if (CollUtil.isNotEmpty(problems)) {
            throw WorkflowErrorCode.WORKFLOW_ASSIGNEE_EXPRESSION_INVALID.workflowException(
                "流程定义存在无法解析的办理人表达式（合法写法见 workflow-assignee-expression-spec.md §3）："
                    + String.join("；", problems));
        }
    }

    private static String nodeLabel(NodeJson node) {
        return "节点[" + StrUtil.blankToDefault(node.getNodeCode(), "?")
            + " " + StrUtil.blankToDefault(node.getNodeName(), "未命名") + "]";
    }

}
