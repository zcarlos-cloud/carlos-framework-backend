package com.carlos.redis.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.Function;

/**
 * Caffeine缓存工具类
 *
 * @author carlos
 * @date 2026-02-01
 */
@Slf4j
public class CaffeineUtil {

    private static Cache<String, Object> cache;

    /**
     * 初始化 CaffeineUtil 静态字段
     */
    public static void init(Cache<String, Object> caffeineCache) {
        CaffeineUtil.cache = caffeineCache;
        log.info("CaffeineUtil initialized");
    }

    /**
     * 获取缓存
     *
     * @param key 键
     * @return 值
     */
    public static Object get(String key) {
        return cache != null ? cache.getIfPresent(key) : null;
    }

    /**
     * 获取缓存，如果不存在则加载
     *
     * @param key    键
     * @param loader 加载函数
     * @return 值
     */
    public static Object get(String key, Function<String, Object> loader) {
        return cache != null ? cache.get(key, loader) : loader.apply(key);
    }

    /**
     * 设置缓存
     *
     * @param key   键
     * @param value 值
     */
    public static void put(String key, Object value) {
        if (cache != null) {
            cache.put(key, value);
        }
    }

    /**
     * 批量设置缓存
     *
     * @param map 键值对
     */
    public static void putAll(Map<String, Object> map) {
        if (cache != null) {
            cache.putAll(map);
        }
    }

    /**
     * 删除缓存
     *
     * @param key 键
     */
    public static void evict(String key) {
        if (cache != null) {
            cache.invalidate(key);
        }
    }

    /**
     * 批量删除缓存
     *
     * @param keys 键集合
     */
    public static void evictAll(Iterable<String> keys) {
        if (cache != null) {
            cache.invalidateAll(keys);
        }
    }

    /**
     * 清空所有缓存
     */
    public static void clear() {
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存大小
     */
    public static long size() {
        return cache != null ? cache.estimatedSize() : 0;
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    public static String stats() {
        return cache != null ? cache.stats().toString() : "Cache not available";
    }

    /**
     * 清理过期缓存
     */
    public static void cleanUp() {
        if (cache != null) {
            cache.cleanUp();
        }
    }
}
