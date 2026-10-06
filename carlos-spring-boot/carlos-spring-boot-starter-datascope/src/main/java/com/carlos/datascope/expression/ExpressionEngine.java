package com.carlos.datascope.expression;

import com.carlos.datascope.core.engine.ExecutionContext;

/**
 * 表达式引擎接口
 * <p>
 * 支持多种表达式类型的解析和执行
 *
 * @author Carlos
 * @version 2.0
 */
public interface ExpressionEngine {

    /**
     * 编译表达式
     *
     * @param expression 表达式字符串
     * @return 编译后的表达式对象
     */
    Object compile(String expression);

    /**
     * 执行表达式
     *
     * @param compiledExpression 编译后的表达式
     * @param context            执行上下文
     * @return 执行结果
     */
    Object execute(Object compiledExpression, ExecutionContext context);

    /**
     * 评估表达式（返回布尔值）
     *
     * @param compiledExpression 编译后的表达式
     * @param context            执行上下文
     * @return true 表达式为真
     */
    boolean evaluate(Object compiledExpression, ExecutionContext context);

    /**
     * 解析并执行表达式（一次性）
     *
     * @param expression 表达式字符串
     * @param context    执行上下文
     * @return 执行结果
     */
    default Object parseAndExecute(String expression, ExecutionContext context) {
        Object compiled = compile(expression);
        return execute(compiled, context);
    }
}
