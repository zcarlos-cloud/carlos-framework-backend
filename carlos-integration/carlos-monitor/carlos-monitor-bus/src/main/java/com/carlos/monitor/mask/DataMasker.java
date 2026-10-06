package com.carlos.monitor.mask;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.carlos.monitor.mask.manager.MonitorDataMaskRuleManager;
import com.carlos.monitor.mask.pojo.entity.MonitorDataMaskRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>
 * 数据脱敏器（redis/db 数据管理域共用）
 * 规则匹配：实例规则优先、全局规则兜底（Manager 层 Caffeine 缓存 60s）；
 * REDIS 目标按 key pattern（glob）+ hash field 匹配；DB 目标按 schema/table/column 匹配（N5 复用）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataMasker {

    /**
     * 目标类型：Redis
     */
    public static final String TARGET_REDIS = "REDIS";

    /**
     * 目标类型：DB
     */
    public static final String TARGET_DB = "DB";

    private static final String MASK_FULL = "***";

    private final MonitorDataMaskRuleManager maskRuleManager;

    /**
     * 对 Redis 值脱敏
     *
     * @param instanceId 实例ID
     * @param key        Redis key
     * @param field      hash field（非 hash 类型传 null，按 column_name='*' 匹配）
     * @param value      原始值
     * @return 脱敏后的值（无匹配规则原样返回）
     */
    public String maskRedisValue(Long instanceId, String key, String field, String value) {
        if (value == null) {
            return null;
        }
        MonitorDataMaskRule rule = matchRedisRule(instanceId, key, field);
        if (rule == null) {
            return value;
        }
        return applyMask(rule, value);
    }

    /**
     * 对 DB 列值脱敏
     *
     * @param instanceId 实例ID
     * @param schemaName 模式名（可空，匹配 '*' 规则）
     * @param tableName  表名（可空）
     * @param columnName 列名
     * @param value      原始值
     * @return 脱敏后的值（无匹配规则原样返回）
     */
    public String maskDbValue(Long instanceId, String schemaName, String tableName,
                              String columnName, String value) {
        if (value == null || columnName == null) {
            return value;
        }
        MonitorDataMaskRule rule = matchDbRule(instanceId, schemaName, tableName, columnName);
        if (rule == null) {
            return value;
        }
        return applyMask(rule, value);
    }

    /**
     * 匹配 DB 脱敏规则（实例规则优先；规则字段为空或 '*' 表示通配）
     */
    private MonitorDataMaskRule matchDbRule(Long instanceId, String schemaName,
                                            String tableName, String columnName) {
        List<MonitorDataMaskRule> rules = maskRuleManager.listEffectiveRules(instanceId, TARGET_DB);
        for (MonitorDataMaskRule rule : rules) {
            if (!wildcardMatch(rule.getSchemaName(), schemaName)) {
                continue;
            }
            if (!wildcardMatch(rule.getTableName(), tableName)) {
                continue;
            }
            if (wildcardMatch(rule.getColumnName(), columnName)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * 通配匹配：规则值为空或 '*' 匹配一切；否则精确匹配（忽略大小写）
     */
    private boolean wildcardMatch(String ruleValue, String value) {
        if (StrUtil.isBlank(ruleValue) || "*".equals(ruleValue)) {
            return true;
        }
        return value != null && ruleValue.equalsIgnoreCase(value);
    }

    /**
     * 匹配 Redis 脱敏规则（实例规则优先，取第一个命中的）
     */
    private MonitorDataMaskRule matchRedisRule(Long instanceId, String key, String field) {        List<MonitorDataMaskRule> rules = maskRuleManager.listEffectiveRules(instanceId, TARGET_REDIS);
        for (MonitorDataMaskRule rule : rules) {
            // table_name 存 key pattern（glob）；column_name 存 hash field 名（'*' 匹配全部）
            if (!globMatch(rule.getTableName(), key)) {
                continue;
            }
            String column = rule.getColumnName();
            boolean fieldMatch = StrUtil.isBlank(column) || "*".equals(column)
                || (field != null && column.equals(field));
            if (fieldMatch) {
                return rule;
            }
        }
        return null;
    }

    /**
     * glob 匹配（* 任意、? 单字符）；pattern 为空不匹配
     */
    private boolean globMatch(String globPattern, String value) {
        if (StrUtil.isBlank(globPattern) || value == null) {
            return false;
        }
        String regex = globPattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
        return Pattern.matches(regex, value);
    }

    /**
     * 按 mask_type 执行脱敏
     */
    private String applyMask(MonitorDataMaskRule rule, String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        return switch (StrUtil.nullToDefault(rule.getMaskType(), "FULL")) {
            case "FULL" -> MASK_FULL;
            case "PHONE" -> maskPhone(value);
            case "EMAIL" -> maskEmail(value);
            case "NAME" -> maskName(value);
            case "CUSTOM" -> maskCustom(rule.getMaskPattern(), value);
            default -> MASK_FULL;
        };
    }

    /**
     * 手机号：保留前 3 后 2，中间脱敏
     */
    private String maskPhone(String value) {
        if (value.length() < 6) {
            return MASK_FULL;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 2);
    }

    /**
     * 邮箱：保留首字符与域名，账号其余脱敏
     */
    private String maskEmail(String value) {
        int atIdx = value.indexOf('@');
        if (atIdx <= 1) {
            return MASK_FULL;
        }
        return value.charAt(0) + "***" + value.substring(atIdx);
    }

    /**
     * 姓名：保留姓氏，其余脱敏
     */
    private String maskName(String value) {
        if (value.length() <= 1) {
            return MASK_FULL;
        }
        return value.charAt(0) + "**";
    }

    /**
     * 自定义：mask_pattern JSON {"pattern":"regex","replacement":"..."}
     */
    private String maskCustom(String maskPattern, String value) {
        if (StrUtil.isBlank(maskPattern) || !JSONUtil.isTypeJSON(maskPattern)) {
            return MASK_FULL;
        }
        try {
            JSONObject json = JSONUtil.parseObj(maskPattern);
            String regex = json.getStr("pattern");
            String replacement = json.getStr("replacement", MASK_FULL);
            if (StrUtil.isBlank(regex)) {
                return MASK_FULL;
            }
            return value.replaceAll(regex, replacement);
        } catch (Exception e) {
            log.warn("自定义脱敏规则执行失败，按全脱敏处理, error:{}", e.getMessage());
            return MASK_FULL;
        }
    }
}
