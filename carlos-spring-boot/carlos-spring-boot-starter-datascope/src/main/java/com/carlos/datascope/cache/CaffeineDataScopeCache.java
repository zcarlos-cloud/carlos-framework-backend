package com.carlos.datascope.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存实现
 * <p>
 * 基于Caffeine的高性能本地缓存
 *
 * @author Carlos
 * @version 2.0
 */
@Slf4j
public class CaffeineDataScopeCache implements DataScopeCache {

    private final Cache<String, Object> cache;
    private final long defaultTtlMinutes;

    public CaffeineDataScopeCache(long maxSize, long ttlMinutes) {
        this.defaultTtlMinutes = ttlMinutes;
        this.cache = Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
            .recordStats()
            .removalListener((key, value, cause) -> {
                log.debug("Cache entry removed: key={}, cause={}", key, cause);
            })
            .build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key) {
        return (T) cache.getIfPresent(key);
    }

    @Override
    public void put(String key, Object value) {
        cache.put(key, value);
    }

    @Override
    public void put(String key, Object value, long ttl, TimeUnit timeUnit) {
        // Caffeine不支持单个条目的TTL，使用统一配置
        cache.put(key, value);
    }

    @Override
    public void remove(String key) {
        cache.invalidate(key);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public boolean contains(String key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    public DataScopeCache.CacheStats stats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = cache.stats();
        return DataScopeCache.CacheStats.of(stats.hitCount(), stats.missCount(), cache.estimatedSize());
    }
}
