package com.carlos.datascope.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 数据权限规则定义
 * <p>
 * 定义一条完整的数据权限规则，包含规则类型、作用范围、表达式等
 *
 * @author Carlos
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataScopeRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 规则唯一标识
     */
    private String ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型
     */
    private RuleType type;

    /**
     * 适用的表名集合
     * <p>支持通配符 *，空集合表示所有表
     */
    @Builder.Default
    private Set<String> tableNames = new HashSet<>();

    /**
     * 适用的Mapper方法名集合
     * <p>支持通配符 *，空集合表示所有方法
     */
    @Builder.Default
    private Set<String> mapperMethods = new HashSet<>();

    /**
     * 权限维度
     */
    private ScopeDimension dimension;

    /**
     * 权限控制字段
     */
    private String field;

    /**
     * 规则表达式
     */
    private RuleExpression expression;

    /**
     * 优先级，数字越小优先级越高
     */
    @Builder.Default
    private int priority = 100;

    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 规则来源
     */
    private RuleSource source;

    /**
     * 扩展属性
     */
    @Builder.Default
    private Map<String, Object> extensions = new HashMap<>();

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 规则类型枚举
     */
    public enum RuleType {
        /**
         * 行权限 - 控制可见的数据行
         */
        ROW,

        /**
         * 列权限 - 控制可见的数据列（脱敏）
         */
        COLUMN,

        /**
         * 范围权限 - 控制数据范围（如时间范围、数值范围）
         */
        RANGE
    }

    /**
     * 规则来源枚举
     */
    public enum RuleSource {
        /**
         * 注解方式
         */
        ANNOTATION,

        /**
         * 数据库配置
         */
        DATABASE,

        /**
         * YAML配置文件
         */
        YAML,

        /**
         * 编程式配置
         */
        PROGRAMMATIC
    }

    /**
     * 检查规则是否匹配指定表
     *
     * @param tableName 表名
     * @return true 匹配
     */
    public boolean matchesTable(String tableName) {
        if (tableNames == null || tableNames.isEmpty()) {
            return true;
        }
        // 检查精确匹配
        if (tableNames.contains(tableName)) {
            return true;
        }
        // 检查通配符匹配
        for (String pattern : tableNames) {
            if ("*".equals(pattern)) {
                return true;
            }
            if (pattern.endsWith("*") && tableName.startsWith(pattern.substring(0, pattern.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查规则是否匹配指定Mapper方法
     *
     * @param methodName 方法名
     * @return true 匹配
     */
    public boolean matchesMethod(String methodName) {
        if (mapperMethods == null || mapperMethods.isEmpty()) {
            return true;
        }
        // 检查精确匹配
        if (mapperMethods.contains(methodName)) {
            return true;
        }
        // 检查通配符匹配
        for (String pattern : mapperMethods) {
            if ("*".equals(pattern)) {
                return true;
            }
            // 支持方法名前缀匹配，如 select* 匹配所有以 select 开头的方法
            if (pattern.endsWith("*") && methodName.startsWith(pattern.substring(0, pattern.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取缓存键
     *
     * @return 缓存键
     */
    public String getCacheKey() {
        return "rule:" + ruleId;
    }
}
