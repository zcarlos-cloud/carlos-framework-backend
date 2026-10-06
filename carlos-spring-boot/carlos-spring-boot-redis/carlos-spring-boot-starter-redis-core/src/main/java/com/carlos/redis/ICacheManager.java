package com.carlos.redis;

import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 标准化缓存管理接口（可分布式、可统计、可观测）
 *
 * @param <T> 实体类型
 * @author Carlos
 */
public interface ICacheManager<T> {

    /* -------------------- 生命周期 -------------------- */

    /**
     * 全量预热
     *
     * @author Carlos
     * @date 2023/2/13 13:43
     */
    default void initCache() {
    }

    /**
     *  清空缓存
     *
     * @author Carlos
     * @date 2025-12-05 18:19
     */
    default long clearCache() {
        return 0L;
    }

    ;

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息
     * @author Carlos
     * @date 2023/1/16 17:49
     */
    default CacheStats stats() {
        return null;
    }

    /* -------------------- 单点 CRUD -------------------- */

    /**
     * 新增缓存
     *
     * @param bean 缓存对象
     * @author Carlos
     * @date 2023/1/16 17:48
     */
    default void putCache(T bean) {
    }

    /**
     * 新增缓存CAS 写入
     *
     * @param bean 缓存对象
     * @return 是否成功
     * @author Carlos
     * @date 2023/1/16 17:49
     */
    default boolean putIfAbsent(T bean) {
        return false;
    }


    /**
     * 更新缓存 读-改-写原子
     *
     * @param bean 缓存对象
     * @param updater 更新函数
     * @return 是否成功
     * @author Carlos
     * @date 2023/1/16 17:49
     */
    default boolean updateCache(T bean, Function<T, T> updater) {
        return false;
    }

    /**
     * 删除缓存
     *
     * @param keys 缓存key
     * @return 是否成功
     * @author Carlos
     * @date 2023/1/16 17:47
     */
    default boolean deleteIfExists(Serializable... keys) {
        return false;
    }

    /**
     * 更新缓存
     *
     * @param bean 更新对象
     * @author Carlos
     * @date 2023/1/16 17:47
     */
    default void updateCache(T bean) {
    }

    /**
     * 删除缓存
     *
     * @param keys 缓存key
     * @author Carlos
     * @date 2023/1/16 17:46
     */
    default T getCache(Serializable... keys) {
        return null;
    }

    /**
     * 删除缓存
     *
     * @param bean 缓存对象
     * @author Carlos
     * @date 2023/1/16 17:46
     */
    default void deleteCache(T bean) {
    }

    /**
     * 获取缓存key
     *
     * @param param 参数
     * @author Carlos
     * @date 2023/1/16 17:46
     */
    default String generateKey(Serializable... param) {
        return null;
    }

    /**
     * 获取缓存key
     * @param format 格式
     * @param params 参数
     * @author Carlos
     * @date 2023/1/16 17:46
     */
    // default String generateKey(String format, Serializable... params) {
    //     return null;
    // }

    /**
     * 淘汰策略
     *
     * @return com.carlos.redis.EvictionPolicy
     * @author Carlos
     * @date 2025-12-07 17:12
     */
    default EvictionPolicy policy() {
        return EvictionPolicy.CacheAside;
    }
}
