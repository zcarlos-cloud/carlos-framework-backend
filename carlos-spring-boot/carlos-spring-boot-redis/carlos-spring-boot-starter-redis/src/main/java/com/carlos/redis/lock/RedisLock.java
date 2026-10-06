package com.carlos.redis.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁注解（增强版）
 * <p>
 * 使用 Redisson 实现分布式锁，支持多种锁类型和获取策略
 * </p>
 *
 * @author carlos
 * @date 2020/10/27 23:42
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisLock {

    /**
     * 锁名称前缀，用于区分不同业务
     */
    String name() default "";

    /**
     * 锁 key，支持 SpEL 表达式
     * <p>示例：
     * <ul>
     *   <li>{@code #userId} - 使用参数名为 userId 的值</li>
     *   <li>{@code #p0} - 使用第一个参数</li>
     *   <li>{@code #user.id} - 使用 user 参数的 id 属性</li>
     *   <li>{@code #p0 + ':' + #p1} - 组合多个参数</li>
     * </ul>
     */
    String key() default "";

    /**
     * 锁类型
     */
    LockType lockType() default LockType.REENTRANT;

    /**
     * 锁自动释放时间
     * <p>
     * 默认值 -1 表示使用看门狗自动续期（推荐），
     * 业务执行期间锁不会过期，业务完成后手动释放。
     * <p>
     * 如果设置为正数，锁将在指定时间后自动释放（无论业务是否完成）
     */
    long leaseTime() default -1;

    /**
     * 等待获取锁的最大时间
     * <p>
     * 默认值 -1 表示无限期阻塞等待，
     * 0 表示立即返回不等待，
     * 正数表示最多等待指定时间
     */
    long waitTime() default -1;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 锁获取失败时的处理策略
     */
    LockStrategy onFailure() default LockStrategy.BLOCK;

    /**
     * 失败时抛出的异常消息
     * 仅在 onFailure = FAIL_FAST 时使用
     */
    String failMessage() default "获取分布式锁失败，请稍后重试";

    /**
     * 是否自动释放锁
     * <p>默认 true，方法执行完成后自动释放
     * 特殊场景可设置为 false，需手动调用 {@link RedissonLockUtil} 释放
     */
    boolean autoUnlock() default true;
}
