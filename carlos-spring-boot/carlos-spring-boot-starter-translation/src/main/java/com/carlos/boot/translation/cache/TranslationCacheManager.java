package com.carlos.boot.translation.cache;

import com.carlos.boot.translation.config.TranslationProperties;
import com.carlos.redis.util.RedisUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 翻译缓存管理器（基于 Caffeine + Redis）
 * </p>
 * <p>
 * 提供本地缓存（Caffeine）与 Redis 二级缓存的协同工作。
 * 当 Redisson 可用时，{@link RedissonTranslationCacheManager} 会覆盖此实现以提供更高效的多级缓存。
 * </p>
 *
 * @author carlos
 * @date 2025/01/20
 */
@Slf4j
public class TranslationCacheManager implements TranslationCache {

    protected static final String CACHE_NAME = "framework:cache:translation";
    protected final Cache<String, Object> caffeineCache;
    protected final TranslationProperties properties;

    public TranslationCacheManager(TranslationProperties properties) {
        this.properties = properties;
        this.caffeineCache = Caffeine.newBuilder()
            .maximumSize(properties.getCache().getLocalSize())
            .expireAfterWrite(properties.getCache().getLocalExpireMinutes(), TimeUnit.MINUTES)
            .recordStats()
            .build();
        log.info("TranslationCacheManager initialized with Caffeine");
    }

    /**
     * 获取缓存
     *
     * @param key  缓存Key
     * @param type 类型
     * @param <T>  泛型
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object local = caffeineCache.getIfPresent(key);
        if (local != null) {
            return (T) local;
        }
        try {
            T value = RedisUtil.getValue(key);
            caffeineCache.put(key, value);
            return value;
        } catch (Exception e) {
            log.warn("Failed to deserialize cache value: {}", key, e);
        }
        return null;
    }

    /**
     * 写入缓存
     *
     * @param key   缓存Key
     * @param value 缓存值
     */
    public void put(String key, Object value) {
        put(key, value, properties.getCache().getRedisExpireMinutes(), TimeUnit.MINUTES);
    }

    /**
     * 写入缓存（指定过期时间）
     *
     * @param key     缓存Key
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void put(String key, Object value, long timeout, TimeUnit unit) {
        caffeineCache.put(key, value);
        try {
            RedisUtil.setValue(key, value, timeout, unit);
        } catch (Exception e) {
            log.warn("Failed to serialize cache value: {}", key, e);
        }
    }

    /**
     * 批量获取（优化 Redis 网络往返）
     *
     * @param keys 缓存Key集合
     * @param type 类型
     * @param <T>  泛型
     * @return 缓存值Map
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> batchGet(Collection<String> keys, Class<T> type) {
        Map<String, T> result = new HashMap<>();
        List<String> missKeys = new ArrayList<>();

        // L1 批量查询
        for (String key : keys) {
            Object local = caffeineCache.getIfPresent(key);
            if (local != null) {
                result.put(key, (T) local);
            } else {
                missKeys.add(key);
            }
        }

        // L2 批量查询（使用 Redis Pipeline）
        if (!missKeys.isEmpty()) {
            Map<String, T> valueList = RedisUtil.getValueMap(missKeys, 300);
            result.putAll(valueList);
            for (Map.Entry<String, T> entry : valueList.entrySet()) {
                // 回填 L1
                caffeineCache.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 使缓存失效
     *
     * @param key 缓存Key
     */
    public void evict(String key) {
        caffeineCache.invalidate(key);
        RedisUtil.delete(key);
    }

    /**
     * 批量使缓存失效
     *
     * @param keys 缓存Key集合
     */
    public void batchEvict(Collection<String> keys) {
        keys.forEach(caffeineCache::invalidate);
        RedisUtil.delete(keys);
    }

    /**
     * 清空所有本地缓存
     */
    public void clearLocal() {
        caffeineCache.invalidateAll();
    }

    /**
     * 获取本地缓存统计信息
     *
     * @return 统计信息
     */
    public String getStats() {
        return caffeineCache.stats().toString();
    }
}
