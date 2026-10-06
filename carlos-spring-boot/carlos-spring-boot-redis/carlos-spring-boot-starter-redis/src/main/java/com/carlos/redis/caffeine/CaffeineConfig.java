package com.carlos.redis.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Caffeine本地缓存配置
 *
 * @author carlos
 * @date 2026-02-01
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(Caffeine.class)
@ConditionalOnProperty(prefix = "carlos.redis.caffeine", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CaffeineProperties.class)
public class CaffeineConfig {

    /**
     * 创建Caffeine缓存实例
     */
    @Bean
    public Cache<String, Object> caffeineCache(CaffeineProperties properties,
                                                 ObjectProvider<MeterRegistry> meterRegistryProvider) {
        log.info("Initializing Caffeine local cache with properties: {}", properties);

        Caffeine<Object, Object> builder = Caffeine.newBuilder();

        // 初始容量
        if (properties.getInitialCapacity() != null) {
            builder.initialCapacity(properties.getInitialCapacity());
        }

        // 最大容量
        if (properties.getMaximumSize() != null) {
            builder.maximumSize(properties.getMaximumSize());
        }

        // 写入后过期
        if (properties.getExpireAfterWriteDuration() != null) {
            builder.expireAfterWrite(properties.getExpireAfterWriteDuration());
        }

        // 访问后过期
        if (properties.getExpireAfterAccessDuration() != null) {
            builder.expireAfterAccess(properties.getExpireAfterAccessDuration());
        }

        // 刷新时间
        if (properties.getRefreshAfterWriteDuration() != null) {
            builder.refreshAfterWrite(properties.getRefreshAfterWriteDuration());
        }

        // 记录统计信息
        if (Boolean.TRUE.equals(properties.getRecordStats())) {
            builder.recordStats();
        }

        // 移除监听器：记录缓存淘汰事件的日志和指标
        if (Boolean.TRUE.equals(properties.getRemovalListenerEnabled())) {
            builder.removalListener((key, value, cause) -> {
                if (cause == RemovalCause.SIZE || cause == RemovalCause.EXPIRED) {
                    log.debug("Caffeine cache eviction: key={}, cause={}", key, cause);
                } else if (cause == RemovalCause.EXPLICIT) {
                    log.trace("Caffeine cache explicit removal: key={}", key);
                }
            });
        }

        Cache<String, Object> cache = builder.build();
        log.info("Caffeine local cache initialized successfully");

        // 注册 Micrometer 指标
        meterRegistryProvider.ifAvailable(registry -> {
            if (Boolean.TRUE.equals(properties.getRecordStats())) {
                registerCaffeineMetrics(cache, registry);
                log.info("Caffeine metrics registered with Micrometer");
            }
        });

        // 初始化 CaffeineUtil
        CaffeineUtil.init(cache);

        return cache;
    }

    /**
     * 将 Caffeine 缓存统计注册为 Micrometer Gauge
     */
    private void registerCaffeineMetrics(Cache<String, Object> cache, MeterRegistry registry) {
        Tags tags = Tags.of("name", "caffeine-cache");
        registry.gauge("carlos.caffeine.hit.count", tags, cache, c -> cache.stats().hitCount());
        registry.gauge("carlos.caffeine.miss.count", tags, cache, c -> cache.stats().missCount());
        registry.gauge("carlos.caffeine.eviction.count", tags, cache, c -> cache.stats().evictionCount());
        registry.gauge("carlos.caffeine.hit.rate", tags, cache, c -> cache.stats().hitRate());
        registry.gauge("carlos.caffeine.estimated.size", tags, cache, Cache::estimatedSize);
    }
}
