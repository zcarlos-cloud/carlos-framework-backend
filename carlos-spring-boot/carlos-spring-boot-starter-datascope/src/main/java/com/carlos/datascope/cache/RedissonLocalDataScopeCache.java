package com.carlos.datascope.cache;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 本地缓存实现（RLocalCachedMap）
 * <p>
 * 基于 RLocalCachedMap 的自动 L1 + L2 两级缓存实现。
 * Redisson 自动处理本地缓存与 Redis 之间的一致性，支持跨实例缓存同步。
 *
 * @author Carlos
 * @version 2.0
 */
@Slf4j
public class RedissonLocalDataScopeCache implements DataScopeCache {

    private static final String CACHE_NAME = "framework:cache:datascope";

    private final RLocalCachedMap<String, Object> cache;
    private final long defaultTtlMinutes;

    public RedissonLocalDataScopeCache(RedissonClient redissonClient, long ttlMinutes) {
        this.defaultTtlMinutes = ttlMinutes;
        LocalCachedMapOptions<String, Object> options = LocalCachedMapOptions.<String, Object>defaults()
            .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU)
            .cacheSize(5000)
            .timeToLive(ttlMinutes, TimeUnit.MINUTES)
            .maxIdle(ttlMinutes / 2, TimeUnit.MINUTES)
            .storeMode(LocalCachedMapOptions.StoreMode.LOCALCACHE_REDIS)
            .syncStrategy(LocalCachedMapOptions.SyncStrategy.INVALIDATE)
            .reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.CLEAR);
        this.cache = redissonClient.getLocalCachedMap(CACHE_NAME, options);
        log.info("RedissonLocalDataScopeCache initialized with ttl={}min", ttlMinutes);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key) {
        return (T) cache.get(key);
    }

    @Override
    public void put(String key, Object value) {
        cache.fastPut(key, value);
    }

    @Override
    public void put(String key, Object value, long ttl, TimeUnit timeUnit) {
        // RLocalCachedMap 不支持单个 key 的 TTL，依赖 LocalCachedMapOptions 全局配置
        cache.fastPut(key, value);
    }

    @Override
    public void remove(String key) {
        cache.remove(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public boolean contains(String key) {
        return cache.containsKey(key);
    }

    @Override
    public CacheStats stats() {
        return CacheStats.of(0, 0, cache.size());
    }
}
