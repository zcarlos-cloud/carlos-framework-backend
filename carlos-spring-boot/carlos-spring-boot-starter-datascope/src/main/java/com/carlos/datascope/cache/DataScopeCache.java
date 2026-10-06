package com.carlos.datascope.cache;

import lombok.Getter;

import java.util.concurrent.TimeUnit;

/**
 * 数据权限缓存接口
 * <p>
 * 定义缓存的基本操作，支持多种缓存实现
 *
 * @author Carlos
 * @version 2.0
 */
public interface DataScopeCache {

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @param <T> 值类型
     * @return 缓存值，不存在返回null
     */
    <T> T get(String key);

    /**
     * 设置缓存值（默认过期时间）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void put(String key, Object value);

    /**
     * 设置缓存值（指定过期时间）
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param ttl      过期时间
     * @param timeUnit 时间单位
     */
    void put(String key, Object value, long ttl, TimeUnit timeUnit);

    /**
     * 移除缓存
     *
     * @param key 缓存键
     */
    void remove(String key);

    /**
     * 清除所有缓存
     */
    void clear();

    /**
     * 检查是否包含键
     *
     * @param key 缓存键
     * @return true 包含
     */
    boolean contains(String key);

    /**
     * 获取缓存统计信息
     *
     * @return CacheStats
     */
    default CacheStats stats() {
        return CacheStats.empty();
    }

    /**
     * 缓存统计信息
     */
    @Getter
    class CacheStats {
        private long hitCount;
        private long missCount;
        private long size;

        public static CacheStats empty() {
            return new CacheStats(0, 0, 0);
        }

        public static CacheStats of(long hitCount, long missCount, long size) {
            return new CacheStats(hitCount, missCount, size);
        }

        public CacheStats(long hitCount, long missCount, long size) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.size = size;
        }

        public double getHitRate() {
            long total = hitCount + missCount;
            return total == 0 ? 0 : (double) hitCount / total;
        }
    }
}
