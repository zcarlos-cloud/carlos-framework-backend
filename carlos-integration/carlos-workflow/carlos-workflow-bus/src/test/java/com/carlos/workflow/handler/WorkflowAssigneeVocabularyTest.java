package com.carlos.workflow.handler;

import com.carlos.workflow.handler.WorkflowAssigneeVocabulary.Expression;
import com.carlos.workflow.handler.WorkflowAssigneeVocabulary.Kind;
import com.carlos.workflow.handler.WorkflowAssigneeVocabulary.Type;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * {@link WorkflowAssigneeVocabulary} 单元测试：词汇表是运行期展开（{@link OrgPermissionHandler}）
 * 与保存期校验（{@link WorkflowAssigneeValidator}）共用的唯一实现，这里逐条钉住它的判定口径。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowAssigneeVocabularyTest {

    @Test
    void parsesEveryStaticVocabularyEntryInTheSpec() {
        List<Expression> expressions = WorkflowAssigneeVocabulary.parse(
            "user:1001@@role:5@@dept:12@@dept:12:rec@@deptLeader:12@@deptLeader:12:2@@position:7@@roleInDept:5:12");

        assertThat(expressions).hasSize(8);
        assertThat(expressions).allSatisfy(expression -> {
            assertThat(expression.kind()).isEqualTo(Kind.KNOWN);
            assertThat(expression.malformed()).as("规范 §3 的写法必须全部合法：%s", expression.raw()).isFalse();
        });
        assertThat(expressions).extracting(Expression::type).containsExactly(
            Type.USER, Type.ROLE, Type.DEPT, Type.DEPT, Type.DEPT_LEADER, Type.DEPT_LEADER, Type.POSITION, Type.ROLE_IN_DEPT);
    }

    @Test
    void splitsOnExpressionSeparatorAndDropsBlankTokens() {
        assertThat(WorkflowAssigneeVocabulary.parse("  role:5 @@ user:1001 , nobody @@  "))
            .as("引擎通常已按 @@ 拆好，词汇表仍要容忍单串里带分隔符与空白")
            .extracting(Expression::raw)
            .containsExactly("role:5", "user:1001 , nobody");
        assertThat(WorkflowAssigneeVocabulary.parse("@@")).isEmpty();
        assertThat(WorkflowAssigneeVocabulary.parse(null)).isEmpty();
        assertThat(WorkflowAssigneeVocabulary.parse("   ")).isEmpty();
    }

    @Test
    void flagsUnknownTypeSeparatelyFromLiteral() {
        Expression unknown = WorkflowAssigneeVocabulary.parse("manager:5").get(0);
        assertThat(unknown.kind()).isEqualTo(Kind.UNKNOWN_TYPE);
        assertThat(unknown.type()).isNull();

        Expression literal = WorkflowAssigneeVocabulary.parse("zhangsan").get(0);
        assertThat(literal.kind())
            .as("无冒号的 token 是发动机的字面量策略：SpEL/流程变量求值结果或裸用户 id")
            .isEqualTo(Kind.LITERAL);
        assertThat(literal.kind()).isNotEqualTo(Kind.UNKNOWN_TYPE);
    }

    @Test
    void marksMalformedParamsButKeepsTheType() {
        assertThat(WorkflowAssigneeVocabulary.parse("role:abc").get(0))
            .satisfies(expression -> {
                assertThat(expression.kind()).isEqualTo(Kind.KNOWN);
                assertThat(expression.type()).isEqualTo(Type.ROLE);
                assertThat(expression.malformed()).isTrue();
            });
    }

    @Test
    void acceptsOnlySpecShapedParameters() {
        assertThat(invalid("user:1001,1002")).isEmpty();
        assertThat(invalid("user:1001,abc")).containsExactly("user:1001,abc");
        assertThat(invalid("dept:12:rec")).isEmpty();
        assertThat(invalid("dept:12:xxx")).containsExactly("dept:12:xxx");
        assertThat(invalid("deptLeader:12:1")).isEmpty();
        assertThat(invalid("deptLeader:12:20")).isEmpty();
        assertThat(invalid("deptLeader:12:21")).as("层级上限防御 parentId 成环").containsExactly("deptLeader:12:21");
        assertThat(invalid("deptLeader:12:0")).containsExactly("deptLeader:12:0");
        assertThat(invalid("roleInDept:5")).containsExactly("roleInDept:5");
        assertThat(invalid("roleInDept:5:12")).isEmpty();
        assertThat(invalid("position:abc")).containsExactly("position:abc");
    }

    @Test
    void acceptsDynamicExpressionsAndPlainUserIdsAsLiterals() {
        assertThat(invalid("#{@workflowAssignees.upLeader(#applicantId)}")).isEmpty();
        assertThat(invalid("${approvers}")).isEmpty();
        assertThat(invalid("1001")).isEmpty();
        assertThat(invalid("zhangsan"))
            .as("裸非数字字面量参与交集永远不会命中办理人，保存期就该拦下")
            .containsExactly("zhangsan");
    }

    @Test
    void reportsOnlyTheOffendingExpressions() {
        assertThat(invalid("role:5@@manager:5@@#{@workflowAssignees.subordinates(#applicantId)}@@dept:12:rec"))
            .containsExactly("manager:5");
    }

    @Test
    void reportsFlagThatParsesToNothing() {
        assertThat(invalid("@@")).as("配了非空内容却一个表达式都解析不出，同样是配置错误").containsExactly("@@");
        assertThat(invalid("  ")).as("空串是引擎的「本节点不鉴权」，不是错误").isEmpty();
        assertThat(invalid(null)).isEmpty();
    }

    @Test
    void identifiesDynamicExpressionsByEnginePrefix() {
        assertThat(WorkflowAssigneeVocabulary.isDynamicExpression("#{@workflowAssignees.upLeader(#applicantId)}")).isTrue();
        assertThat(WorkflowAssigneeVocabulary.isDynamicExpression("${approvers}")).isTrue();
        assertThat(WorkflowAssigneeVocabulary.isDynamicExpression("1001")).isFalse();
        assertThat(WorkflowAssigneeVocabulary.isDynamicExpression("role:5")).isFalse();
        assertThat(WorkflowAssigneeVocabulary.isDynamicExpression(null)).isFalse();
    }

    @Test
    void parsesUserIdsAndLevels() {
        assertThat(WorkflowAssigneeVocabulary.userIds("1001,1002")).containsExactly(1001L, 1002L);
        assertThat(WorkflowAssigneeVocabulary.userIds("1001,abc")).isNull();
        assertThat(WorkflowAssigneeVocabulary.userIds("")).isNull();
        assertThat(WorkflowAssigneeVocabulary.level("3")).isEqualTo(3);
        assertThat(WorkflowAssigneeVocabulary.level("0")).isNull();
        assertThat(WorkflowAssigneeVocabulary.level("abc")).isNull();
    }

    @Test
    void parseAllFlattensConcatenatedTokens() {
        assertThat(WorkflowAssigneeVocabulary.parseAll(List.of("user:11@@role:5", " ", "dept:12")))
            .extracting(Expression::raw)
            .containsExactly("user:11", "role:5", "dept:12");
        assertThat(WorkflowAssigneeVocabulary.parseAll(null)).isEmpty();
    }

    private static List<String> invalid(String permissionFlag) {
        return WorkflowAssigneeVocabulary.invalidExpressions(permissionFlag);
    }

}
