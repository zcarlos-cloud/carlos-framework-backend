package com.carlos.redis.lock;

/**
 * 锁获取失败时的处理策略
 *
 * @author carlos
 * @date 2024/01/01
 */
public enum LockStrategy {
    /**
     * 快速失败 - 立即抛出异常
     */
    FAIL_FAST,

    /**
     * 跳过执行 - 静默跳过方法执行，返回 null
     */
    SKIP,

    /**
     * 继续执行 - 无锁执行（带警告日志）
     */
    CONTINUE,

    /**
     * 阻塞等待 - 持续等待直到获取锁（默认）
     */
    BLOCK
}
