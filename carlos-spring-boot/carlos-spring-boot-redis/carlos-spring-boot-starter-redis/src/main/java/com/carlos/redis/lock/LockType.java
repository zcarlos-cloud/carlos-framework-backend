package com.carlos.redis.lock;

/**
 * 分布式锁类型
 *
 * @author carlos
 * @date 2024/01/01
 */
public enum LockType {
    /**
     * 普通可重入锁（默认）
     */
    REENTRANT,

    /**
     * 公平锁 - 按请求锁的顺序获取锁
     */
    FAIR,

    /**
     * 读锁 - 共享锁，多个读线程可同时获取
     */
    READ,

    /**
     * 写锁 - 独占锁，写操作独占资源
     */
    WRITE,

    /**
     * 联锁 - 同时获取多个锁，全部获取成功才算成功
     */
    MULTI
}
