package com.carlos.datascope.core.engine;

import com.carlos.datascope.core.model.DataScopeRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 编译后的规则
 * <p>
 * 包含原始规则和编译后的表达式对象，用于提高执行性能
 *
 * @author Carlos
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompiledRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 原始规则
     */
    private DataScopeRule rule;

    /**
     * 编译后的表达式对象
     */
    private transient Object compiledExpression;

    /**
     * 编译时间戳
     */
    private long compileTime;

    /**
     * 是否编译成功
     */
    private boolean compileSuccess;

    /**
     * 编译错误信息
     */
    private String compileError;

    /**
     * 创建编译成功的规则
     *
     * @param rule               原始规则
     * @param compiledExpression 编译后的表达式
     * @return CompiledRule
     */
    public static CompiledRule success(DataScopeRule rule, Object compiledExpression) {
        return CompiledRule.builder()
            .rule(rule)
            .compiledExpression(compiledExpression)
            .compileTime(System.currentTimeMillis())
            .compileSuccess(true)
            .build();
    }

    /**
     * 创建编译失败的规则
     *
     * @param rule  原始规则
     * @param error 错误信息
     * @return CompiledRule
     */
    public static CompiledRule failure(DataScopeRule rule, String error) {
        return CompiledRule.builder()
            .rule(rule)
            .compileTime(System.currentTimeMillis())
            .compileSuccess(false)
            .compileError(error)
            .build();
    }

    /**
     * 检查是否有效
     *
     * @return true 编译成功且有效
     */
    public boolean isValid() {
        return compileSuccess && compiledExpression != null;
    }
}
