package com.carlos.json.config;

import cn.hutool.core.date.DatePattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * JSON 配置属性类
 * </p>
 *
 * @author carlos
 * @date 2025/01/15
 */
@Data
@ConfigurationProperties(prefix = "carlos.json")
public class JsonProperties {

    /**
     * 是否启用 JSON 自动配置
     */
    private boolean enabled = true;

    /**
     * JSON 引擎类型: jackson, fastjson2, gson
     */
    private JsonEngineType engine = JsonEngineType.JACKSON;

    /**
     * 日期格式
     */
    private String dateFormat = DatePattern.NORM_DATETIME_PATTERN;

    /**
     * 时区
     */
    private String timeZone = "GMT+8";

    /**
     * 是否忽略 null 值字段
     */
    private boolean ignoreNull = true;

    /**
     * 是否忽略空字符串（序列化时跳过空字符串字段）
     */
    private boolean ignoreEmptyString = false;

    /**
     * 序列化配置
     */
    private Serialization serialization = new Serialization();

    /**
     * 反序列化配置
     */
    private Deserialization deserialization = new Deserialization();

    /**
     * 字段映射配置
     */
    private FieldMapping fieldMapping = new FieldMapping();

    /**
     * JSON 引擎类型枚举
     */
    public enum JsonEngineType {
        /**
         * Jackson 引擎（Spring Boot 默认）
         */
        JACKSON,
        /**
         * Fastjson2 引擎（阿里巴巴，性能优先）
         */
        FASTJSON2,
        /**
         * Gson 引擎（Google）
         */
        GSON
    }

    /**
     * 序列化配置
     */
    @Data
    public static class Serialization {
        /**
         * 是否输出缩进（美化）
         */
        private boolean indentOutput = false;

        /**
         * 遇到空 Bean 是否失败
         */
        private boolean failOnEmptyBeans = false;

        /**
         * 是否将 Long/BigInteger 转为 String（解决精度丢失）
         */
        private boolean longToString = true;
    }

    /**
     * 反序列化配置
     */
    @Data
    public static class Deserialization {
        /**
         * 遇到未知属性是否失败
         */
        private boolean failOnUnknownProperties = false;

        /**
         * 是否允许单引号
         */
        private boolean allowSingleQuotes = true;

        /**
         * 是否允许注释
         */
        private boolean allowComments = false;

        /**
         * 是否允许未转义的控制字符
         */
        private boolean allowUnescapedControlChars = false;

        /**
         * 是否允许反斜杠转义任何字符
         */
        private boolean allowBackslashEscapingAnyChar = false;
    }

    /**
     * 字段映射配置
     */
    @Data
    public static class FieldMapping {
        /**
         * 属性命名策略：SNAKE_CASE, LOWER_CAMEL_CASE, KEBAB_CASE, LOWER_CASE, UPPER_CAMEL_CASE
         */
        private String propertyNamingStrategy = "LOWER_CAMEL_CASE";
    }
}
