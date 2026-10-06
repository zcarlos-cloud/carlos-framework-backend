package com.carlos.redis.stream;

import java.util.Map;

/**
 * 待处理条目汇总（{@code XPENDING key group} 的返回值，不可变）
 *
 * @param group       消费组名
 * @param total       待处理总数
 * @param minId       最小 id（无待处理时为 null）
 * @param maxId       最大 id（无待处理时为 null）
 * @param perConsumer 每个消费者的待处理条数（不可变 Map）
 * @author Carlos
 * @since 3.0.0
 */
public record StreamPendingSummary(
    String group,
    long total,
    String minId,
    String maxId,
    Map<String, Long> perConsumer
) {

    /**
     * 紧凑构造器：Map 复制为不可变。
     */
    public StreamPendingSummary {
        perConsumer = perConsumer == null ? Map.of() : Map.copyOf(perConsumer);
    }

    /**
     * 只读派生：是否存在积压
     *
     * @return 有待处理条目时返回 true
     */
    public boolean hasPending() {
        return total > 0;
    }
}
