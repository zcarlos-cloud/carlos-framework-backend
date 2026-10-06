package com.carlos.boot.translation.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 翻译缓存接口
 * </p>
 *
 * @author carlos
 * @date 2025/01/20
 */
public interface TranslationCache {

    /**
     * 获取缓存
     *
     * @param key  缓存Key
     * @param type 类型
     * @param <T>  泛型
     * @return 缓存值
     */
    <T> T get(String key, Class<T> type);

    /**
     * 写入缓存
     *
     * @param key   缓存Key
     * @param value 缓存值
     */
    void put(String key, Object value);

    /**
     * 写入缓存（指定过期时间）
     *
     * @param key     缓存Key
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    void put(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 批量获取
     *
     * @param keys 缓存Key集合
     * @param type 类型
     * @param <T>  泛型
     * @return 缓存值Map
     */
    <T> Map<String, T> batchGet(Collection<String> keys, Class<T> type);

    /**
     * 使缓存失效
     *
     * @param key 缓存Key
     */
    void evict(String key);

    /**
     * 批量使缓存失效
     *
     * @param keys 缓存Key集合
     */
    void batchEvict(Collection<String> keys);

    /**
     * 清空所有本地缓存
     */
    void clearLocal();

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    String getStats();
}
