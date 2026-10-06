package com.carlos.datascope.cache;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式缓存实现
 * <p>
 * 基于Redis的分布式缓存，支持集群环境
 *
 * @author Carlos
 * @version 2.0
 */
@Slf4j
@RequiredArgsConstructor
public class RedisDataScopeCache implements DataScopeCache {

    private static final String KEY_PREFIX = "framework:datascope:";

    private final StringRedisTemplate redisTemplate;
    private final long defaultTtlMinutes;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(KEY_PREFIX + key);
        } catch (Exception e) {
            log.warn("Redis get failed, fallback to null: key={}", key, e);
            return null;
        }
    }

    @Override
    public void put(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(
                KEY_PREFIX + key,
                JSONUtil.toJsonStr(value),
                defaultTtlMinutes,
                TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("Redis put failed: key={}", key, e);
        }
    }

    @Override
    public void put(String key, Object value, long ttl, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + key, JSONUtil.toJsonStr(value), ttl, timeUnit);
        } catch (Exception e) {
            log.warn("Redis put failed: key={}", key, e);
        }
    }

    @Override
    public void remove(String key) {
        try {
            redisTemplate.delete(KEY_PREFIX + key);
        } catch (Exception e) {
            log.warn("Redis remove failed: key={}", key, e);
        }
    }

    @Override
    public void clear() {
        try {
            // 只删除 framework:datascope: 前缀的key
            // 实际实现中应该使用Redis的scan命令
            log.info("Clearing Redis cache with prefix: {}", KEY_PREFIX);
        } catch (Exception e) {
            log.warn("Redis clear failed", e);
        }
    }

    @Override
    public boolean contains(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + key));
        } catch (Exception e) {
            log.warn("Redis contains failed: key={}", key, e);
            return false;
        }
    }
}
