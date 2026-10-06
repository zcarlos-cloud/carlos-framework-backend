package com.carlos.datascope.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 规则表达式
 * <p>
 * 定义规则的表达式内容和类型，支持多种表达式引擎
 *
 * @author Carlos
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleExpression implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 表达式类型
     */
    private ExpressionType type;

    /**
     * 表达式内容
     */
    private String content;

    /**
     * 表达式参数
     */
    @Builder.Default
    private Map<String, Object> params = new HashMap<>();

    /**
     * 编译后的表达式（缓存，不序列化）
     */
    private transient Object compiledExpression;

    /**
     * 表达式类型枚举
     */
    public enum ExpressionType {
        /**
         * SQL片段 - 直接拼接到WHERE条件
         * <p>示例: dept_id IN (1, 2, 3)
         */
        SQL,

        /**
         * SpringEL表达式 - 支持复杂逻辑
         * <p>示例: @ds.hasRole('admin') and #userId == authentication.principal.id
         */
        SPEL,

        /**
         * 简单表达式 - field operator value
         * <p>示例: dept_id = ${deptId}
         */
        SIMPLE,

        /**
         * 脚本表达式 - Groovy/JavaScript
         */
        SCRIPT
    }

    /**
     * 创建SQL类型的表达式
     *
     * @param sql SQL片段
     * @return RuleExpression
     */
    public static RuleExpression ofSql(String sql) {
        return RuleExpression.builder()
            .type(ExpressionType.SQL)
            .content(sql)
            .build();
    }

    /**
     * 创建SpEL类型的表达式
     *
     * @param spel SpEL表达式
     * @return RuleExpression
     */
    public static RuleExpression ofSpel(String spel) {
        return RuleExpression.builder()
            .type(ExpressionType.SPEL)
            .content(spel)
            .build();
    }

    /**
     * 创建简单类型的表达式
     *
     * @param expression 简单表达式
     * @return RuleExpression
     */
    public static RuleExpression ofSimple(String expression) {
        return RuleExpression.builder()
            .type(ExpressionType.SIMPLE)
            .content(expression)
            .build();
    }

    /**
     * 检查表达式是否有效
     *
     * @return true 有效
     */
    public boolean isValid() {
        return content != null && !content.trim().isEmpty();
    }

    /**
     * 获取缓存键
     *
     * @return 缓存键
     */
    public String getCacheKey() {
        return "expr:" + type + ":" + Math.abs(content.hashCode());
    }
}
