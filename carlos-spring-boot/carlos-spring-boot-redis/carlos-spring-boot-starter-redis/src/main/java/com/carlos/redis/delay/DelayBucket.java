package com.carlos.redis.delay;

/**
 * 延时队列「桶」（不可变）
 * <p>
 * 一个桶对应 Redis 里一组队列（Redisson 内部会派生 ZSET 与通道 key）。
 * 桶的语义（优先级、业务分组）由上层解释，<b>本层只做透传</b>。
 * </p>
 *
 * @param name      桶名（用于日志与配置）
 * @param queueName 实际队列名（含前缀）
 * @author Carlos
 * @since 3.0.0
 */
public record DelayBucket(String name, String queueName) {

    /**
     * 按前缀与桶名构造
     *
     * @param prefix 前缀（以冒号结尾）
     * @param name   桶名
     * @return 桶
     */
    public static DelayBucket of(String prefix, String name) {
        String normalized = prefix == null || prefix.isBlank() ? "" : prefix;
        return new DelayBucket(name, normalized + name);
    }
}
