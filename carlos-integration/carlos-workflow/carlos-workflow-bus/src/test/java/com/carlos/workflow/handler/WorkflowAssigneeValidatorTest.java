package com.carlos.workflow.handler;

import com.carlos.workflow.exception.WorkflowException;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.dto.NodeJson;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * <p>
 * {@link WorkflowAssigneeValidator} 单元测试：保存定义时把每个节点的 {@code permissionFlag}
 * 过一遍词汇表，拦住静态词汇写错这类错误，并在错误消息里给出节点编码与表达式。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowAssigneeValidatorTest {

    @Test
    void acceptsDefinitionUsingTheWholeSpecVocabulary() {
        DefJson defJson = definition(
            node("start", "发起", null),
            node("approve", "经理审批", "role:5@@deptLeader:12:2"),
            node("finance", "财务复核", "user:1001,1002@@dept:12:rec@@position:7@@roleInDept:5:12"),
            node("auto", "系统节点", "1001"),
            node("escalate", "升级节点", "#{@workflowAssignees.upLeader(#applicantId)}"),
            node("end", "结束", null));

        assertThatCode(() -> WorkflowAssigneeValidator.validate(defJson)).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnknownTypeAndNamesTheNode() {
        DefJson defJson = definition(
            node("start", "发起", null),
            node("approve", "经理审批", "manager:5"));

        Throwable thrown = catchThrowable(() -> WorkflowAssigneeValidator.validate(defJson));

        assertThat(thrown).isInstanceOf(WorkflowException.class);
        assertThat(((WorkflowException) thrown).getErrorCode()).isEqualTo("10705");
        assertThat(thrown.getMessage())
            .as("一次列出所有出错节点，改定义不用来回试")
            .contains("approve")
            .contains("经理审批")
            .contains("manager:5");
    }

    @Test
    void listsEveryOffendingNodeAndExpression() {
        DefJson defJson = definition(
            node("approve", "经理审批", "role:abc"),
            node("finance", "财务复核", "dept:12:xxx@@user:1001"),
            node("ok", "人事备案", "user:1001"));

        Throwable thrown = catchThrowable(() -> WorkflowAssigneeValidator.validate(defJson));

        assertThat(thrown).isInstanceOf(WorkflowException.class);
        assertThat(thrown.getMessage())
            .contains("approve")
            .contains("role:abc")
            .contains("finance")
            .contains("dept:12:xxx")
            .as("合法表达式与合法节点不进错误消息")
            .doesNotContain("user:1001")
            .doesNotContain("人事备案");
    }

    @Test
    void rejectsBareLiteralThatNoUserCanMatch() {
        DefJson defJson = definition(node("approve", "经理审批", "zhangsan"));

        assertThat(catchThrowable(() -> WorkflowAssigneeValidator.validate(defJson)))
            .isInstanceOf(WorkflowException.class);
    }

    @Test
    void ignoresBlankFlagsAndEmptyDefinitions() {
        assertThatCode(() -> WorkflowAssigneeValidator.validate(null)).doesNotThrowAnyException();
        assertThatCode(() -> WorkflowAssigneeValidator.validate(new DefJson())).doesNotThrowAnyException();
        assertThatCode(() -> WorkflowAssigneeValidator.validate(definition(node("end", "结束", "  "))))
            .as("空配置是引擎的「本节点不鉴权」，校验不拦")
            .doesNotThrowAnyException();
    }

    private static DefJson definition(NodeJson... nodes) {
        DefJson defJson = new DefJson();
        defJson.setFlowCode("leave");
        defJson.setNodeList(new ArrayList<>(List.of(nodes)));
        return defJson;
    }

    private static NodeJson node(String code, String name, String permissionFlag) {
        return new NodeJson().setNodeCode(code).setNodeName(name).setPermissionFlag(permissionFlag);
    }

}
