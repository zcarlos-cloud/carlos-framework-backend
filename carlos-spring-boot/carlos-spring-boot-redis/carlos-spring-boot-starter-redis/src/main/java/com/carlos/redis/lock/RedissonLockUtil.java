package com.carlos.redis.lock;

import com.carlos.core.response.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 分布式锁工具类（增强版）
 * <p>
 * 基于 Redisson 实现，支持多种锁类型和编程式锁操作
 *
 * @author carlos
 * @date 2022/4/21 17:44
 */
@Slf4j
public class RedissonLockUtil {

    private static RedissonClient staticRedissonClient;

    /**
     * 初始化 RedissonLockUtil 静态字段
     */
    public static void init(RedissonClient redissonClient) {
        staticRedissonClient = redissonClient;
        log.info("RedissonLockUtil initialized");
    }

    // ========================= 基础锁操作 =========================

    /**
     * 获取普通可重入锁
     */
    public static RLock getLock(String name) {
        checkRedissonClient();
        return staticRedissonClient.getLock(name);
    }

    /**
     * 获取公平锁
     */
    public static RLock getFairLock(String name) {
        checkRedissonClient();
        return staticRedissonClient.getFairLock(name);
    }

    /**
     * 获取读写锁
     */
    public static RReadWriteLock getReadWriteLock(String name) {
        checkRedissonClient();
        return staticRedissonClient.getReadWriteLock(name);
    }

    /**
     * 获取读锁
     */
    public static RLock getReadLock(String name) {
        return getReadWriteLock(name).readLock();
    }

    /**
     * 获取写锁
     */
    public static RLock getWriteLock(String name) {
        return getReadWriteLock(name).writeLock();
    }

    // ========================= 加锁操作 =========================

    /**
     * 加锁（阻塞，使用看门狗自动续期）
     *
     * @param name 锁名称
     * @return true-成功, false-失败
     */
    public static boolean lock(String name) {
        return lock(name, LockType.REENTRANT);
    }

    /**
     * 加锁（阻塞，指定租约时间）
     *
     * @param name     锁名称
     * @param leaseTime 租约时间（自动释放时间）
     * @param timeUnit 时间单位
     * @return true-成功, false-失败
     */
    public static boolean lock(String name, long leaseTime, TimeUnit timeUnit) {
        return lock(name, LockType.REENTRANT, leaseTime, timeUnit);
    }

    /**
     * 加锁（阻塞，指定锁类型）
     *
     * @param name     锁名称
     * @param lockType 锁类型
     * @return true-成功, false-失败
     */
    public static boolean lock(String name, LockType lockType) {
        try {
            RLock lock = getLockByType(name, lockType);
            lock.lock();
            log.debug("[RedissonLock] Acquired {} lock: {}", lockType, name);
            return true;
        } catch (Exception e) {
            log.error("[RedissonLock] Failed to acquire {} lock: {}", lockType, name, e);
            return false;
        }
    }

    /**
     * 加锁（阻塞，指定锁类型和租约时间）
     *
     * @param name      锁名称
     * @param lockType  锁类型
     * @param leaseTime 租约时间
     * @param timeUnit  时间单位
     * @return true-成功, false-失败
     */
    public static boolean lock(String name, LockType lockType, long leaseTime, TimeUnit timeUnit) {
        try {
            RLock lock = getLockByType(name, lockType);
            lock.lock(leaseTime, timeUnit);
            log.debug("[RedissonLock] Acquired {} lock: {}, leaseTime: {} {}",
                lockType, name, leaseTime, timeUnit);
            return true;
        } catch (Exception e) {
            log.error("[RedissonLock] Failed to acquire {} lock: {}", lockType, name, e);
            return false;
        }
    }

    // ========================= 尝试加锁 =========================

    /**
     * 尝试加锁（非阻塞）
     *
     * @param name 锁名称
     * @return true-成功, false-失败
     */
    public static boolean tryLock(String name) {
        return tryLock(name, LockType.REENTRANT, 0, -1, TimeUnit.MILLISECONDS);
    }

    /**
     * 尝试加锁（带等待时间，使用看门狗）
     *
     * @param name     锁名称
     * @param waitTime 最大等待时间
     * @param timeUnit 时间单位
     * @return true-成功, false-失败
     */
    public static boolean tryLock(String name, long waitTime, TimeUnit timeUnit) {
        return tryLock(name, LockType.REENTRANT, waitTime, -1, timeUnit);
    }

    /**
     * 尝试加锁（带等待时间和租约时间）
     *
     * @param name      锁名称
     * @param waitTime  最大等待时间
     * @param leaseTime 租约时间（-1 使用看门狗）
     * @param timeUnit  时间单位
     * @return true-成功, false-失败
     */
    public static boolean tryLock(String name, long waitTime, long leaseTime, TimeUnit timeUnit) {
        return tryLock(name, LockType.REENTRANT, waitTime, leaseTime, timeUnit);
    }

    /**
     * 尝试加锁（完整参数）
     *
     * @param name      锁名称
     * @param lockType  锁类型
     * @param waitTime  最大等待时间
     * @param leaseTime 租约时间（-1 使用看门狗）
     * @param timeUnit  时间单位
     * @return true-成功, false-失败
     */
    public static boolean tryLock(String name, LockType lockType, long waitTime, long leaseTime, TimeUnit timeUnit) {
        try {
            checkRedissonClient();
            RLock lock = getLockByType(name, lockType);
            boolean acquired;
            if (leaseTime < 0) {
                acquired = lock.tryLock(waitTime, timeUnit);
            } else {
                acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            }
            log.debug("[RedissonLock] Try {} lock: {}, waitTime: {} {}, result: {}",
                lockType, name, waitTime, timeUnit, acquired);
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[RedissonLock] Interrupted while trying to acquire {} lock: {}", lockType, name);
            return false;
        } catch (Exception e) {
            log.error("[RedissonLock] Failed to try {} lock: {}", lockType, name, e);
            return false;
        }
    }

    // ========================= 释放锁 =========================

    /**
     * 释放锁
     *
     * @param name 锁名称
     * @return true-成功, false-失败
     */
    public static boolean unlock(String name) {
        return unlock(name, LockType.REENTRANT);
    }

    /**
     * 释放指定类型的锁
     *
     * @param name     锁名称
     * @param lockType 锁类型
     * @return true-成功, false-失败
     */
    public static boolean unlock(String name, LockType lockType) {
        try {
            checkRedissonClient();
            RLock lock = getLockByType(name, lockType);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("[RedissonLock] Released {} lock: {}", lockType, name);
                return true;
            } else {
                log.warn("[RedissonLock] {} lock {} is not held by current thread", lockType, name);
                return false;
            }
        } catch (Exception e) {
            log.error("[RedissonLock] Failed to release lock: {}", name, e);
            return false;
        }
    }

    /**
     * 强制释放锁（不检查持有者）
     * <p>慎用！可能导致锁被错误释放</p>
     *
     * @param name 锁名称
     */
    public static void forceUnlock(String name) {
        try {
            checkRedissonClient();
            RLock lock = staticRedissonClient.getLock(name);
            lock.forceUnlock();
            log.warn("[RedissonLock] Force unlocked: {}", name);
        } catch (Exception e) {
            log.error("[RedissonLock] Failed to force unlock: {}", name, e);
        }
    }

    // ========================= 锁状态查询 =========================

    /**
     * 判断锁是否被持有
     */
    public static boolean isLocked(String name) {
        try {
            checkRedissonClient();
            return staticRedissonClient.getLock(name).isLocked();
        } catch (Exception e) {
            log.error("[RedissonLock] Failed to check lock status: {}", name, e);
            return false;
        }
    }

    /**
     * 判断当前线程是否持有锁
     */
    public static boolean isHeldByCurrentThread(String name) {
        try {
            checkRedissonClient();
            return staticRedissonClient.getLock(name).isHeldByCurrentThread();
        } catch (Exception e) {
            log.error("[RedissonLock] Failed to check lock holder: {}", name, e);
            return false;
        }
    }

    /**
     * 获取锁剩余持有时间
     *
     * @return 剩余时间（毫秒），-1 表示未设置过期时间，-2 表示锁不存在
     */
    public static long getRemainTime(String name) {
        try {
            checkRedissonClient();
            return staticRedissonClient.getLock(name).remainTimeToLive();
        } catch (Exception e) {
            log.error("[RedissonLock] Failed to get remain time: {}", name, e);
            return -2;
        }
    }

    // ========================= 模板方法 =========================

    /**
     * 在锁内执行操作（自动获取和释放）
     *
     * @param name     锁名称
     * @param supplier 业务逻辑
     * @param <T>      返回类型
     * @return 业务逻辑返回值
     * @throws RuntimeException 获取锁失败时抛出
     */
    public static <T> T executeWithLock(String name, Supplier<T> supplier) {
        return executeWithLock(name, LockType.REENTRANT, supplier);
    }

    /**
     * 在锁内执行操作（自动获取和释放，带类型）
     *
     * @param name     锁名称
     * @param lockType 锁类型
     * @param supplier 业务逻辑
     * @param <T>      返回类型
     * @return 业务逻辑返回值
     */
    public static <T> T executeWithLock(String name, LockType lockType, Supplier<T> supplier) {
        if (!lock(name, lockType)) {
            throw CommonErrorCode.SERVICE_UNAVAILABLE.exception("获取分布式锁失败: " + name);
        }
        try {
            return supplier.get();
        } finally {
            unlock(name, lockType);
        }
    }

    /**
     * 在锁内执行操作（自动获取和释放，带返回值默认值）
     *
     * @param name         锁名称
     * @param lockType     锁类型
     * @param waitTime     等待时间
     * @param timeUnit     时间单位
     * @param defaultValue 获取锁失败时的默认值
     * @param supplier     业务逻辑
     * @param <T>          返回类型
     * @return 业务逻辑返回值或默认值
     */
    public static <T> T executeWithLock(String name, LockType lockType, long waitTime, TimeUnit timeUnit,
                                        T defaultValue, Supplier<T> supplier) {
        if (!tryLock(name, lockType, waitTime, -1, timeUnit)) {
            return defaultValue;
        }
        try {
            return supplier.get();
        } finally {
            unlock(name, lockType);
        }
    }

    // ========================= 私有方法 =========================

    private static RLock getLockByType(String name, LockType lockType) {
        return switch (lockType) {
            case FAIR -> staticRedissonClient.getFairLock(name);
            case READ -> staticRedissonClient.getReadWriteLock(name).readLock();
            case WRITE -> staticRedissonClient.getReadWriteLock(name).writeLock();
            case REENTRANT, MULTI -> staticRedissonClient.getLock(name);
        };
    }

    private static void checkRedissonClient() {
        if (staticRedissonClient == null) {
            throw new IllegalStateException("RedissonClient is not initialized");
        }
    }
}
