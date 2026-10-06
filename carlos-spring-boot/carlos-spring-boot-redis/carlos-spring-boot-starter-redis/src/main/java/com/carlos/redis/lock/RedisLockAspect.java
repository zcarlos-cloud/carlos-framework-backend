package com.carlos.redis.lock;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.util.SpelUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁 AOP 切面（增强版）
 * <p>
 * 支持多种锁类型、等待策略和失败处理
 *
 * @author carlos
 * @date 2020/10/27 23:46
 */
@Slf4j
@Aspect
@Order(-100) // 确保在事务注解之前执行
@AllArgsConstructor
public class RedisLockAspect {

    private final RedissonClient redissonClient;
    private final RedisLockProperties lockProperties;

    @Around("@annotation(redisLock)")
    public Object around(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        String lockKey = buildLockKey(joinPoint, redisLock);
        RLock lock = getLock(lockKey, redisLock.lockType());

        boolean acquired = acquireLock(lock, redisLock);

        if (!acquired) {
            return handleFailure(joinPoint, redisLock, lockKey);
        }

        log.debug("[RedisLock] Acquired {} lock: {}", redisLock.lockType(), lockKey);

        try {
            return joinPoint.proceed();
        } finally {
            if (redisLock.autoUnlock()) {
                releaseLock(lock, lockKey, redisLock.lockType());
            }
        }
    }

    /**
     * 获取锁对象
     */
    private RLock getLock(String lockKey, LockType lockType) {
        return switch (lockType) {
            case FAIR -> redissonClient.getFairLock(lockKey);
            case READ -> {
                RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
                yield rwLock.readLock();
            }
            case WRITE -> {
                RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
                yield rwLock.writeLock();
            }
            case REENTRANT, MULTI -> redissonClient.getLock(lockKey);
        };
    }

    /**
     * 获取锁
     *
     * @return true-获取成功, false-获取失败
     */
    private boolean acquireLock(RLock lock, RedisLock redisLock) throws InterruptedException {
        long waitTime = redisLock.waitTime();
        long leaseTime = redisLock.leaseTime();
        TimeUnit timeUnit = redisLock.timeUnit();

        // 策略为 BLOCK 且 waitTime < 0: 无限期阻塞，使用 Redisson 的 lock()
        if (redisLock.onFailure() == LockStrategy.BLOCK && waitTime < 0) {
            if (leaseTime < 0) {
                // 使用看门狗自动续期
                lock.lock();
            } else {
                lock.lock(leaseTime, timeUnit);
            }
            return true;
        }

        // 带超时的尝试获取
        long effectiveWaitTime = waitTime < 0 ? 0 : waitTime;
        if (leaseTime < 0) {
            // leaseTime = -1 时，tryLock 也会使用看门狗
            return lock.tryLock(effectiveWaitTime, timeUnit);
        } else {
            return lock.tryLock(effectiveWaitTime, leaseTime, timeUnit);
        }
    }

    /**
     * 释放锁
     */
    private void releaseLock(RLock lock, String lockKey, LockType lockType) {
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("[RedisLock] Released {} lock: {}", lockType, lockKey);
            } else {
                log.warn("[RedisLock] Lock {} is not held by current thread, skip unlock", lockKey);
            }
        } catch (Exception e) {
            log.error("[RedisLock] Failed to release lock: {}", lockKey, e);
        }
    }

    /**
     * 处理获取锁失败的情况
     *
     * @param joinPoint 切点，用于 CONTINUE 策略时继续执行
     */
    private Object handleFailure(ProceedingJoinPoint joinPoint, RedisLock redisLock, String lockKey) throws Throwable {
        LockStrategy strategy = redisLock.onFailure();
        String failMsg = redisLock.failMessage();

        log.warn("[RedisLock] Failed to acquire lock: {}, strategy: {}", lockKey, strategy);

        return switch (strategy) {
            case FAIL_FAST -> throw new BusinessException(failMsg);
            case SKIP -> {
                log.info("[RedisLock] Skip method execution due to lock acquire failure: {}", lockKey);
                yield null;
            }
            case CONTINUE -> {
                log.warn("[RedisLock] Continue without lock (risky!): {}", lockKey);
                // 无锁继续执行业务方法
                yield joinPoint.proceed();
            }
            case BLOCK -> throw new IllegalStateException("Should not reach here for BLOCK strategy");
        };
    }

    /**
     * 构建锁的完整 key
     */
    private String buildLockKey(ProceedingJoinPoint joinPoint, RedisLock redisLock) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method targetMethod = methodSignature.getMethod();
        Object target = joinPoint.getTarget();
        Object[] arguments = joinPoint.getArgs();

        String prefix = lockProperties.getPrefix();
        String name = redisLock.name();
        String spel = redisLock.key();

        // 解析 SpEL 表达式
        String parsedKey = SpelUtil.parse(target, spel, targetMethod, arguments);

        // 构建完整 key
        StringBuilder lockKey = new StringBuilder(prefix);
        if (StrUtil.isNotBlank(name)) {
            lockKey.append(name).append(StrUtil.COLON);
        }
        lockKey.append(parsedKey);

        return lockKey.toString();
    }
}
