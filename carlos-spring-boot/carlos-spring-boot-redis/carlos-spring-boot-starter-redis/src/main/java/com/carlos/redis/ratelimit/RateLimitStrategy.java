package com.carlos.redis.ratelimit;

/**
 * 限流策略枚举
 * <p>
 * 定义触发限流时的处理策略
 *
 * @author carlos
 * @date 2026-04-08
 */
public enum RateLimitStrategy {

    /**
     * 快速失败 - 立即抛出限流异常
     * <p>适用于需要及时响应、不允许等待的场景</p>
     */
    FAIL_FAST,

    /**
     * 阻塞等待 - 等待令牌可用后继续执行
     * <p>适用于必须完成的业务，愿意等待的场景</p>
     */
    BLOCK,

    /**
     * 降级处理 - 执行降级逻辑（调用指定方法或返回默认值）
     * <p>适用于有备用方案的业务</p>
     */
    FALLBACK
}
