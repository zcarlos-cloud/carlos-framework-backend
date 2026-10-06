package com.carlos.redis.config;

import com.carlos.redis.serialize.SerializerType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * 缓存配置项
 * </p>
 *
 * <p>
 * 只保留仍然生效的两个字段：value 的序列化方式，以及响应式 Redis 的开关。
 * 原先的 {@code use-prefix} / {@code key-prefix}（统一前缀）与 {@code key-max-length} /
 * {@code key-overflow-strategy}（注解缓存 key 生成器）已随 Spring Cache 集成一并删除 —— 那套机制
 * 只服务 {@code @Cacheable} 系列注解，本框架不使用注解缓存，key 前缀一律由各模块常量显式书写。
 * </p>
 *
 * @author carlos
 * @date 2021/6/10 13:21
 */
@Data
@ConfigurationProperties(prefix = "carlos.cache")
public class CacheProperties {

    /**
     * 序列化类型：jackson、fastjson、kryo、jdk
     * 默认 jackson
     */
    private SerializerType serializer = SerializerType.JACKSON;

    /**
     * Reactive Redis 配置
     */
    private Reactive reactive = new Reactive();

    @Data
    public static class Reactive {
        /**
         * 是否启用 Reactive Redis 支持
         * 默认启用（当类路径存在 reactor 时）
         */
        private boolean enabled = true;
    }
}
