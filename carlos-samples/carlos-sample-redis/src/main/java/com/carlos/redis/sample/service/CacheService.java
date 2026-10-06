package com.carlos.redis.sample.service;

import com.carlos.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 缓存服务示例
 *
 * @author carlos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisUtil redisService;
    private final RedissonClient redissonClient;

    /**
     * 设置缓存
     */
    public void setCache(String key, String value, Long expire, TimeUnit timeUnit) {
        // redisService.set(key, value, expire, timeUnit);
        log.info("设置缓存: key={}, value={}, expire={}{}", key, value, expire, timeUnit);
    }

    /**
     * 获取缓存
     */
    public String getCache(String key) {
        // String value = redisService.get(key);
        log.info("获取缓存: key={}, value={}", key, null);
        return null;
    }

    /**
     * 删除缓存
     */
    public void deleteCache(String key) {
        redisService.delete(key);
        log.info("删除缓存: key={}", key);
    }

    /**
     * 获取或加载缓存（带缓存穿透保护）
     */
    public String getOrLoad(String key) {
        // return redisService.getOrLoad(key, k -> {
        //     // 模拟从数据库加载数据
        //     // log.info("从数据源加载数据: key={}", k);
        //     return "loaded-value-for-" + k;
        // }, 60L, TimeUnit.SECONDS);
        return null;
    }

    /**
     * 尝试获取分布式锁
     */
    public boolean tryLock(String lockName, Long waitTime, Long leaseTime, TimeUnit timeUnit) throws InterruptedException {
        RLock lock = redissonClient.getLock("framework:lock:" + lockName);
        boolean locked = lock.tryLock(waitTime, leaseTime, timeUnit);
        log.info("尝试获取锁: lockName={}, locked={}", lockName, locked);
        return locked;
    }

    /**
     * 释放分布式锁
     */
    public void unlock(String lockName) {
        RLock lock = redissonClient.getLock("framework:lock:" + lockName);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("释放锁: lockName={}", lockName);
        }
    }

    /**
     * 分布式锁执行业务（自动释放）
     */
    public String lockAndExecute(String lockName) {
        RLock lock = redissonClient.getLock("framework:lock:" + lockName);
        try {
            boolean locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                return "获取锁失败";
            }
            try {
                // 执行业务逻辑
                log.info("执行业务逻辑: lockName={}", lockName);
                Thread.sleep(1000);
                return "业务执行成功";
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "业务执行中断";
        }
    }

    /**
     * 批量设置缓存
     */
    public void setCacheBatch(Integer count) {
        for (int i = 0; i < count; i++) {
            String key = "batch:key:" + i;
            String value = "value-" + i;
            // redisService.set(key, value, 300L, TimeUnit.SECONDS);
        }
        log.info("批量设置缓存完成: count={}", count);
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        // redisService.deleteByPattern("*");
        log.info("清空所有缓存");
    }
}
