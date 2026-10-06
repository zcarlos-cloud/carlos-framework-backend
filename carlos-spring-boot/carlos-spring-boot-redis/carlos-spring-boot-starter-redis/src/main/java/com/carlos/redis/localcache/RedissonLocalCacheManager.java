package com.carlos.redis.localcache;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 本地缓存管理器
 * <p>
 * 基于 RLocalCachedMap 实现自动 L1 (本地) + L2 (Redis) 两级缓存。
 * 本地缓存与 Redis 之间通过 Pub/Sub 自动同步，支持跨实例缓存失效。
 * </p>
 *
 * @author carlos
 * @date 2026-05-23
 */
@Slf4j
public class RedissonLocalCacheManager {

    private final RedissonClient redissonClient;
    private final LocalCachedMapOptions<String, Object> defaultOptions;

    public RedissonLocalCacheManager(RedissonClient redissonClient) {
        this(redissonClient, createDefaultOptions());
    }

    public RedissonLocalCacheManager(RedissonClient redissonClient,
                                     LocalCachedMapOptions<String, Object> defaultOptions) {
        this.redissonClient = redissonClient;
        this.defaultOptions = defaultOptions;
        log.info("RedissonLocalCacheManager initialized");
    }

    /**
     * 创建默认缓存配置
     */
    public static LocalCachedMapOptions<String, Object> createDefaultOptions() {
        return LocalCachedMapOptions.<String, Object>defaults()
            .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU)
            .cacheSize(10000)
            .timeToLive(10, TimeUnit.MINUTES)
            .maxIdle(5, TimeUnit.MINUTES)
            .storeMode(LocalCachedMapOptions.StoreMode.LOCALCACHE_REDIS)
            .syncStrategy(LocalCachedMapOptions.SyncStrategy.INVALIDATE)
            .reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.CLEAR);
    }

    /**
     * 获取指定名称的本地缓存 Map
     */
    public RLocalCachedMap<String, Object> getCache(String name) {
        return redissonClient.getLocalCachedMap(name, defaultOptions);
    }

    /**
     * 创建自定义配置的本地缓存 Map
     */
    public RLocalCachedMap<String, Object> getCache(String name,
                                                    LocalCachedMapOptions<String, Object> options) {
        return redissonClient.getLocalCachedMap(name, options);
    }

    /**
     * 获取缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String cacheName, String key) {
        return (T) getCache(cacheName).get(key);
    }

    /**
     * 设置缓存值
     */
    public void put(String cacheName, String key, Object value) {
        getCache(cacheName).fastPut(key, value);
    }

    /**
     * 设置缓存值（带过期时间）
     * <p>注意：RLocalCachedMap 仅支持全局 TTL，单个 key 的 TTL 由 LocalCachedMapOptions 配置决定。
     * 此方法中的 ttl 和 unit 参数将被忽略。</p>
     */
    public void put(String cacheName, String key, Object value, long ttl, TimeUnit unit) {
        getCache(cacheName).fastPut(key, value);
    }

    /**
     * 批量设置缓存
     */
    public void putAll(String cacheName, Map<String, Object> map) {
        getCache(cacheName).putAll(map);
    }

    /**
     * 删除缓存
     */
    public void evict(String cacheName, String key) {
        getCache(cacheName).remove(key);
    }

    /**
     * 批量删除缓存
     */
    public void evictAll(String cacheName, Iterable<String> keys) {
        RLocalCachedMap<String, Object> cache = getCache(cacheName);
        for (String key : keys) {
            cache.remove(key);
        }
    }

    /**
     * 清空指定缓存
     */
    public void clear(String cacheName) {
        getCache(cacheName).clear();
    }

    /**
     * 判断缓存是否包含 key
     */
    public boolean contains(String cacheName, String key) {
        return getCache(cacheName).containsKey(key);
    }

    /**
     * 获取缓存统计信息
     */
    public long size(String cacheName) {
        return getCache(cacheName).size();
    }
}
