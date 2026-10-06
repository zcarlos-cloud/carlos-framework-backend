package com.carlos.boot.translation.cache;

import com.carlos.boot.translation.config.TranslationProperties;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RedissonClient;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Redisson 版翻译缓存管理器（基于 RLocalCachedMap）
 * </p>
 * <p>
 * 使用 Redisson 的 RLocalCachedMap 实现自动 L1 + L2 两级缓存，
 * Redisson 自动处理本地缓存与 Redis 的一致性。
 * 当 RedissonClient 不可用时，回退到 {@link TranslationCacheManager} 模式。
 * </p>
 *
 * @author carlos
 * @date 2026/06/06
 */
@Slf4j
public class RedissonTranslationCacheManager implements TranslationCache {

    private static final String CACHE_NAME = "framework:cache:translation";
    private final TranslationProperties properties;
    private final RLocalCachedMap<String, Object> localCachedMap;

    public RedissonTranslationCacheManager(TranslationProperties properties, RedissonClient redissonClient) {
        this.properties = properties;
        LocalCachedMapOptions<String, Object> options = LocalCachedMapOptions.<String, Object>defaults()
            .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU)
            .cacheSize((int) properties.getCache().getLocalSize())
            .timeToLive(properties.getCache().getLocalExpireMinutes(), TimeUnit.MINUTES)
            .maxIdle(properties.getCache().getLocalExpireMinutes() / 2, TimeUnit.MINUTES)
            .storeMode(LocalCachedMapOptions.StoreMode.LOCALCACHE_REDIS)
            .syncStrategy(LocalCachedMapOptions.SyncStrategy.INVALIDATE)
            .reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.CLEAR);
        this.localCachedMap = redissonClient.getLocalCachedMap(CACHE_NAME, options);
        log.info("RedissonTranslationCacheManager initialized with RLocalCachedMap");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) localCachedMap.get(key);
    }

    @Override
    public void put(String key, Object value) {
        localCachedMap.fastPut(key, value);
    }

    @Override
    public void put(String key, Object value, long timeout, TimeUnit unit) {
        // RLocalCachedMap 不支持单个 key 的 TTL，依赖 LocalCachedMapOptions 全局配置
        localCachedMap.fastPut(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> batchGet(Collection<String> keys, Class<T> type) {
        Map<String, Object> result = localCachedMap.getAll(new HashSet<>(keys));
        Map<String, T> typedResult = new HashMap<>();
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            typedResult.put(entry.getKey(), (T) entry.getValue());
        }
        return typedResult;
    }

    @Override
    public void evict(String key) {
        localCachedMap.remove(key);
    }

    @Override
    public void batchEvict(Collection<String> keys) {
        localCachedMap.fastRemove(keys.toArray(new String[0]));
    }

    @Override
    public void clearLocal() {
        log.warn("RLocalCachedMap clearLocal() clears both local and Redis cache");
        localCachedMap.clear();
    }

    @Override
    public String getStats() {
        return "RLocalCachedMap size=" + localCachedMap.size();
    }
}
