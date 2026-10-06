package com.carlos.datascope.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.*;

/**
 * 数据权限配置属性
 * <p>
 * 支持YAML配置数据权限规则
 *
 * @author Carlos
 * @version 2.0
 */
@Data
@ConfigurationProperties(prefix = "carlos.datascope")
public class DataScopeProperties {

    /**
     * 是否启用数据权限
     */
    private boolean enabled = true;

    /**
     * 默认权限维度
     */
    private String defaultDimension = "CURRENT_USER";

    /**
     * 默认权限字段映射
     */
    private FieldMapping fieldMapping = new FieldMapping();

    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * 审计配置
     */
    private AuditConfig audit = new AuditConfig();

    /**
     * 全局规则列表
     */
    private List<RuleConfig> globalRules = new ArrayList<>();

    /**
     * 表级规则配置
     */
    private Map<String, RuleConfig> tableRules = new HashMap<>();

    /**
     * 字段映射配置
     */
    @Data
    public static class FieldMapping {
        private String user = "create_by";
        private String dept = "dept_id";
        private String role = "role_id";
        private String region = "region_code";
    }

    /**
     * 缓存配置
     */
    @Data
    public static class CacheConfig {
        private boolean enabled = true;
        private LocalCacheConfig local = new LocalCacheConfig();
        private RedisCacheConfig redis = new RedisCacheConfig();
    }

    /**
     * 本地缓存配置
     */
    @Data
    public static class LocalCacheConfig {
        private boolean enabled = true;
        private long maxSize = 1000;
        private Duration ttl = Duration.ofMinutes(5);
    }

    /**
     * Redis缓存配置
     */
    @Data
    public static class RedisCacheConfig {
        private boolean enabled = false;
        private Duration ttl = Duration.ofMinutes(30);
    }

    /**
     * 审计配置
     */
    @Data
    public static class AuditConfig {
        private boolean enabled = true;
        private String storage = "LOGGER";
        private int sampleRate = 100;
        private boolean async = true;
    }

    /**
     * 规则配置
     */
    @Data
    public static class RuleConfig {
        private String dimension;
        private String field;
        private List<String> tables;
        private int priority = 100;
        private boolean enabled = true;
    }
}
