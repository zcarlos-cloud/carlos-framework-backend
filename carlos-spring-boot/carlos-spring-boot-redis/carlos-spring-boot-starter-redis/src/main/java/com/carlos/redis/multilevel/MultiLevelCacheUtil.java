package com.carlos.redis.multilevel;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 多级缓存工具类
 * <p>
 * 基于 Redisson RLocalCachedMap 实现自动 L1 (本地) + L2 (Redis) 两级缓存。
 * Redisson 自动处理本地缓存和 Redis 之间的一致性，通过 Pub/Sub 实现跨实例缓存同步。
 * </p>
 *
 * @author carlos
 * @date 2026-02-01
 */
@Slf4j
public class MultiLevelCacheUtil {

    private static final String CACHE_NAME = "framework:cache:multilevel";
    private static RLocalCachedMap<String, Object> localCachedMap;

    private MultiLevelCacheUtil() {
    }

    /**
     * 初始化多级缓存
     *
     * @param redissonClient RedissonClient
     * @param options        本地缓存配置选项
     */
    public static void init(RedissonClient redissonClient, LocalCachedMapOptions<String, Object> options) {
        localCachedMap = redissonClient.getLocalCachedMap(CACHE_NAME, options);
        log.info("MultiLevelCacheUtil initialized with RLocalCachedMap, cacheName: {}", CACHE_NAME);
    }

    /**
     * 获取缓存，先从 L1 获取，再从 L2 获取
     *
     * @param key 键
     * @return 值
     */
    public static Object get(String key) {
        if (localCachedMap == null) {
            log.warn("MultiLevelCacheUtil not initialized, key: {}", key);
            return null;
        }
        Object value = localCachedMap.get(key);
        if (value != null) {
            log.debug("Hit local cached map for key: {}", key);
        }
        return value;
    }

    /**
     * 获取缓存，如果不存在则加载
     *
     * @param key    键
     * @param loader 加载函数
     * @return 值
     */
    public static Object get(String key, Supplier<Object> loader) {
        return get(key, loader, 0, null);
    }

    /**
     * 获取缓存，如果不存在则加载并设置过期时间
     *
     * @param key      键
     * @param loader   加载函数
     * @param timeout  L2 过期时间
     * @param timeUnit 时间单位
     * @return 值
     */
    public static Object get(String key, Supplier<Object> loader, long timeout, TimeUnit timeUnit) {
        if (localCachedMap == null) {
            log.warn("MultiLevelCacheUtil not initialized, key: {}", key);
            return loader != null ? loader.get() : null;
        }

        Object value = localCachedMap.get(key);
        if (value != null) {
            log.debug("Hit local cached map for key: {}", key);
            return value;
        }

        log.debug("Cache miss for key: {}, loading from source", key);
        if (loader == null) {
            return null;
        }

        value = loader.get();
        if (value != null) {
            if (timeout > 0 && timeUnit != null) {
                // RLocalCachedMap 不支持单个 key 的 TTL，依赖 LocalCachedMapOptions 全局配置
                localCachedMap.fastPut(key, value);
            } else {
                localCachedMap.fastPut(key, value);
            }
            log.debug("Loaded and cached key: {}", key);
        }
        return value;
    }

    /**
     * 设置缓存到两级缓存
     *
     * @param key   键
     * @param value 值
     */
    public static void put(String key, Object value) {
        put(key, value, 0, null);
    }

    /**
     * 设置缓存到两级缓存，并设置过期时间
     *
     * @param key      键
     * @param value    值
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    public static void put(String key, Object value, long timeout, TimeUnit timeUnit) {
        if (localCachedMap == null) {
            log.warn("MultiLevelCacheUtil not initialized, key: {}", key);
            return;
        }
        if (timeout > 0 && timeUnit != null) {
            // RLocalCachedMap 不支持单个 key 的 TTL，依赖 LocalCachedMapOptions 全局配置
            localCachedMap.fastPut(key, value);
        } else {
            localCachedMap.fastPut(key, value);
        }
        log.debug("Put value to local cached map for key: {}", key);
    }

    /**
     * 删除两级缓存
     *
     * @param key 键
     */
    public static void evict(String key) {
        if (localCachedMap == null) {
            return;
        }
        localCachedMap.remove(key);
        log.debug("Evicted key from local cached map: {}", key);
    }

    /**
     * 批量删除两级缓存
     *
     * @param keys 键集合
     */
    public static void evictAll(Iterable<String> keys) {
        if (localCachedMap == null) {
            return;
        }
        for (String key : keys) {
            localCachedMap.remove(key);
        }
        log.debug("Evicted keys from local cached map");
    }

    /**
     * 清空两级缓存
     */
    public static void clear() {
        if (localCachedMap == null) {
            return;
        }
        localCachedMap.clear();
        log.info("Cleared local cached map");
    }

    /**
     * 按前缀删除两级缓存
     * <p>遍历缓存 key 并删除匹配指定前缀的条目。适用于按业务前缀批量失效，
     * 避免使用 {@link #clear()} 误清全部缓存。</p>
     *
     * @param prefix 前缀（非空）
     * @return 删除的 key 数量
     */
    public static int evictByPrefix(String prefix) {
        if (localCachedMap == null || prefix == null || prefix.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String key : localCachedMap.keySet()) {
            if (key != null && key.startsWith(prefix)) {
                localCachedMap.remove(key);
                count++;
            }
        }
        log.debug("Evicted {} keys by prefix: {}", count, prefix);
        return count;
    }

    /**
     * 仅删除 L1 缓存
     * <p>注意：RLocalCachedMap 不支持单独清除本地缓存，此方法行为与 evict 一致</p>
     *
     * @param key 键
     */
    public static void evictL1(String key) {
        evict(key);
    }

    /**
     * 仅删除 L2 缓存
     * <p>注意：RLocalCachedMap 不支持单独清除 Redis 缓存，此方法行为与 evict 一致</p>
     *
     * @param key 键
     */
    public static void evictL2(String key) {
        evict(key);
    }

    /**
     * 刷新缓存（先删除再加载）
     *
     * @param key    键
     * @param loader 加载函数
     * @return 新值
     */
    public static Object refresh(String key, Supplier<Object> loader) {
        evict(key);
        return get(key, loader);
    }

    /**
     * 刷新缓存（带过期时间）
     *
     * @param key      键
     * @param loader   加载函数
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return 新值
     */
    public static Object refresh(String key, Supplier<Object> loader, long timeout, TimeUnit timeUnit) {
        evict(key);
        return get(key, loader, timeout, timeUnit);
    }
}
