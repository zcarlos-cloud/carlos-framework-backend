package com.carlos.gateway.ratelimit.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * <p>
 * 限流指标事件
 * 用于收集限流统计数据，可对接 Micrometer/Prometheus
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
@Getter
public class RateLimitMetricsEvent extends ApplicationEvent {

    /**
     * 路由 ID
     */
    private final String routeId;

    /**
     * 限流键
     */
    private final String key;

    /**
     * 是否被允许
     */
    private final boolean allowed;

    /**
     * 剩余令牌数
     */
    private final long tokensRemaining;

    /**
     * 响应时间（毫秒）
     */
    private final long responseTimeMs;

    /**
     * 事件发生时间
     */
    private final Instant eventTime;

    /**
     * 创建指标事件
     *
     * @param source          事件源
     * @param routeId         路由 ID
     * @param key             限流键
     * @param allowed         是否被允许
     * @param tokensRemaining 剩余令牌数
     * @param responseTimeMs  响应时间
     * @param eventTime       事件发生时间
     */
    public RateLimitMetricsEvent(Object source, String routeId, String key,
                                 boolean allowed, long tokensRemaining,
                                 long responseTimeMs, Instant eventTime) {
        super(source);
        this.routeId = routeId;
        this.key = key;
        this.allowed = allowed;
        this.tokensRemaining = tokensRemaining;
        this.responseTimeMs = responseTimeMs;
        this.eventTime = eventTime;
    }

    /**
     * 获取事件发生时间
     *
     * @return 事件发生时间
     */
    public Instant getEventTime() {
        return eventTime;
    }

    @Override
    public String toString() {
        return "RateLimitMetricsEvent{" +
            "routeId='" + routeId + '\'' +
            ", key='" + key + '\'' +
            ", allowed=" + allowed +
            ", tokensRemaining=" + tokensRemaining +
            ", responseTimeMs=" + responseTimeMs +
            ", eventTime=" + eventTime +
            '}';
    }
}
