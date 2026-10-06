package com.carlos.datascope.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;

/**
 * 数据权限处理结果
 * <p>
 * 包含SQL条件、参数值、脱敏规则等处理结果
 *
 * @author Carlos
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataScopeResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否允许访问
     */
    @Builder.Default
    private boolean allowed = true;

    /**
     * SQL条件片段
     * <p>用于拼接到原SQL的WHERE条件中
     */
    private String sqlCondition;

    /**
     * 参数值列表
     * <p>对应sqlCondition中的占位符参数
     */
    @Builder.Default
    private List<Object> paramValues = new ArrayList<>();

    /**
     * 需要脱敏的列集合
     */
    @Builder.Default
    private Set<String> maskingColumns = new HashSet<>();

    /**
     * 脱敏规则映射
     * <p>key: 列名, value: 脱敏规则
     */
    @Builder.Default
    private Map<String, MaskingRule> maskingRules = new HashMap<>();

    /**
     * 拒绝原因
     * <p>当 allowed = false 时有效
     */
    private String denyReason;

    /**
     * 匹配的规则的ID列表
     */
    @Builder.Default
    private List<String> matchedRuleIds = new ArrayList<>();

    // ==================== 静态工厂方法 ====================

    /**
     * 获取允许访问的结果（无条件限制）
     *
     * @return DataScopeResult
     */
    public static DataScopeResult allowed() {
        return DataScopeResult.builder()
            .allowed(true)
            .sqlCondition("1 = 1")
            .build();
    }

    /**
     * 获取拒绝访问的结果
     *
     * @param reason 拒绝原因
     * @return DataScopeResult
     */
    public static DataScopeResult denied(String reason) {
        return DataScopeResult.builder()
            .allowed(false)
            .denyReason(reason)
            .sqlCondition("1 = 0")
            .build();
    }

    /**
     * 获取条件结果
     *
     * @param condition SQL条件
     * @param params    参数值
     * @return DataScopeResult
     */
    public static DataScopeResult condition(String condition, Object... params) {
        return DataScopeResult.builder()
            .allowed(true)
            .sqlCondition(condition)
            .paramValues(Arrays.asList(params))
            .build();
    }

    // ==================== 实例方法 ====================

    /**
     * 合并两个结果
     *
     * @param other 另一个结果
     * @return 合并后的结果
     */
    public DataScopeResult merge(DataScopeResult other) {
        if (other == null) {
            return this;
        }

        DataScopeResult result = new DataScopeResult();

        // 合并允许状态
        result.setAllowed(this.allowed && other.allowed);

        // 合并SQL条件
        result.setSqlCondition(mergeCondition(this.sqlCondition, other.sqlCondition));

        // 合并参数值
        List<Object> mergedParams = new ArrayList<>(this.paramValues);
        mergedParams.addAll(other.paramValues);
        result.setParamValues(mergedParams);

        // 合并脱敏列
        Set<String> mergedColumns = new HashSet<>(this.maskingColumns);
        mergedColumns.addAll(other.maskingColumns);
        result.setMaskingColumns(mergedColumns);

        // 合并脱敏规则
        Map<String, MaskingRule> mergedRules = new HashMap<>(this.maskingRules);
        mergedRules.putAll(other.maskingRules);
        result.setMaskingRules(mergedRules);

        // 合并拒绝原因
        if (!other.allowed) {
            result.setDenyReason(other.denyReason);
        } else if (!this.allowed) {
            result.setDenyReason(this.denyReason);
        }

        // 合并规则ID
        List<String> mergedRuleIds = new ArrayList<>(this.matchedRuleIds);
        mergedRuleIds.addAll(other.matchedRuleIds);
        result.setMatchedRuleIds(mergedRuleIds);

        return result;
    }

    /**
     * 合并两个SQL条件
     *
     * @param cond1 条件1
     * @param cond2 条件2
     * @return 合并后的条件
     */
    private String mergeCondition(String cond1, String cond2) {
        if (cond1 == null || cond1.isEmpty()) {
            return cond2;
        }
        if (cond2 == null || cond2.isEmpty()) {
            return cond1;
        }
        if ("1 = 1".equals(cond1)) {
            return cond2;
        }
        if ("1 = 1".equals(cond2)) {
            return cond1;
        }
        return "(" + cond1 + ") AND (" + cond2 + ")";
    }

    /**
     * 添加脱敏规则
     *
     * @param column 列名
     * @param rule   脱敏规则
     */
    public void addMaskingRule(String column, MaskingRule rule) {
        this.maskingColumns.add(column);
        this.maskingRules.put(column, rule);
    }

    /**
     * 添加匹配的规则ID
     *
     * @param ruleId 规则ID
     */
    public void addMatchedRuleId(String ruleId) {
        this.matchedRuleIds.add(ruleId);
    }

    /**
     * 是否需要脱敏
     *
     * @return true 如果存在脱敏列
     */
    public boolean needsMasking() {
        return maskingColumns != null && !maskingColumns.isEmpty();
    }

    // ==================== 内部类 ====================

    /**
     * 脱敏规则
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaskingRule implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 脱敏类型
         */
        private MaskingType type;

        /**
         * 自定义正则表达式
         */
        private String pattern;

        /**
         * 掩码字符
         */
        @Builder.Default
        private char maskChar = '*';

        /**
         * 保留前缀字符数
         */
        private int keepPrefix;

        /**
         * 保留后缀字符数
         */
        private int keepSuffix;
    }

    /**
     * 脱敏类型枚举
     */
    public enum MaskingType {
        PHONE,      // 手机号: 138****8888
        ID_CARD,    // 身份证: 110101********1234
        EMAIL,      // 邮箱: t***@qq.com
        BANK_CARD,  // 银行卡: 6222 **** **** 8888
        NAME,       // 姓名: 张**
        ADDRESS,    // 地址: 北京市********
        CUSTOM      // 自定义
    }

    /**
     * 数据范围
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataRange implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 最小值
         */
        private Comparable<?> minValue;

        /**
         * 最大值
         */
        private Comparable<?> maxValue;

        /**
         * 允许的离散值
         */
        @Builder.Default
        private Set<Object> allowedValues = new HashSet<>();

        /**
         * 排除的值
         */
        @Builder.Default
        private Set<Object> excludedValues = new HashSet<>();
    }
}
