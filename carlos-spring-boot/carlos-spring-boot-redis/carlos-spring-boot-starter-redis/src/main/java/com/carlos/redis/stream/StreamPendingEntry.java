package com.carlos.redis.stream;

import java.time.Duration;

/**
 * 单条待处理条目（{@code XPENDING key group IDLE ...} 的明细，不可变）
 * <p>
 * <b>投递次数的权威来源</b>：{@code deliveryCount} 取自 Redis 待处理队列自身的计数器，
 * 跨实例、跨重启持久，因此它不应被应用自己再维护一份（否则会引入双写不一致）。
 * </p>
 *
 * @param recordId      物理 id
 * @param consumerName  当前归属消费者
 * @param idle          距上次投递的空闲时长
 * @param deliveryCount 累计投递次数
 * @author Carlos
 * @since 3.0.0
 */
public record StreamPendingEntry(
    String recordId,
    String consumerName,
    Duration idle,
    long deliveryCount
) {

    /**
     * 只读派生：空闲时长是否已达到阈值
     *
     * @param threshold 阈值
     * @return 达到或超过时返回 true
     */
    public boolean isIdleBeyond(Duration threshold) {
        return idle != null && threshold != null && idle.compareTo(threshold) >= 0;
    }
}
