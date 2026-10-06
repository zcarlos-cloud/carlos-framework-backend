package com.carlos.redis.sample.controller;

import com.carlos.core.response.Result;
import com.carlos.redis.sample.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存与分布式锁操作接口
 *
 * @author carlos
 */
@Tag(name = "Redis 示例接口", description = "缓存操作、分布式锁演示")
@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor
public class RedisController {

    private final CacheService cacheService;

    @Operation(summary = "设置缓存")
    @PostMapping("/cache")
    public Result<Boolean> setCache(
        @Parameter(description = "缓存键") @RequestParam String key,
        @Parameter(description = "缓存值") @RequestParam String value,
        @Parameter(description = "过期时间(秒)，默认60秒") @RequestParam(required = false, defaultValue = "60") Long expire) {
        cacheService.setCache(key, value, expire, TimeUnit.SECONDS);
        return Result.success(Boolean.TRUE);
    }

    @Operation(summary = "获取缓存")
    @GetMapping("/cache")
    public Result<String> getCache(
        @Parameter(description = "缓存键") @RequestParam String key) {
        String value = cacheService.getCache(key);
        return Result.success(value);
    }

    @Operation(summary = "删除缓存")
    @DeleteMapping("/cache")
    public Result<Boolean> deleteCache(
        @Parameter(description = "缓存键") @RequestParam String key) {
        cacheService.deleteCache(key);
        return Result.success(Boolean.TRUE);
    }

    @Operation(summary = "获取或设置缓存（带缓存穿透保护）")
    @GetMapping("/cache-or-load")
    public Result<String> getOrLoad(
        @Parameter(description = "缓存键") @RequestParam String key) {
        String value = cacheService.getOrLoad(key);
        return Result.success(value);
    }

    @Operation(summary = "尝试获取分布式锁")
    @PostMapping("/lock")
    public Result<Boolean> tryLock(
        @Parameter(description = "锁名称") @RequestParam String lockName,
        @Parameter(description = "等待时间(秒)") @RequestParam(defaultValue = "5") Long waitTime,
        @Parameter(description = "租约时间(秒)") @RequestParam(defaultValue = "30") Long leaseTime) throws InterruptedException {
        boolean locked = cacheService.tryLock(lockName, waitTime, leaseTime, TimeUnit.SECONDS);
        return Result.success(locked);
    }

    @Operation(summary = "释放分布式锁")
    @DeleteMapping("/lock")
    public Result<Boolean> unlock(
        @Parameter(description = "锁名称") @RequestParam String lockName) {
        cacheService.unlock(lockName);
        return Result.success(Boolean.TRUE);
    }

    @Operation(summary = "分布式锁执行业务（自动释放）")
    @PostMapping("/lock-execute")
    public Result<String> lockAndExecute(
        @Parameter(description = "锁名称") @RequestParam String lockName) {
        String result = cacheService.lockAndExecute(lockName);
        return Result.success(result);
    }

    @Operation(summary = "批量设置缓存")
    @PostMapping("/cache-batch")
    public Result<Boolean> setCacheBatch(
        @Parameter(description = "数量") @RequestParam(defaultValue = "10") Integer count) {
        cacheService.setCacheBatch(count);
        return Result.success(Boolean.TRUE);
    }

    @Operation(summary = "清空所有缓存")
    @DeleteMapping("/cache-all")
    public Result<Boolean> clearCache() {
        cacheService.clearCache();
        return Result.success(Boolean.TRUE);
    }
}
