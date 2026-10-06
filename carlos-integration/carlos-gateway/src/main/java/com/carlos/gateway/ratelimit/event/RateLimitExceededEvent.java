package com.carlos.gateway.ratelimit.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.Map;

/**
 * <p>
 * 限流超限事件
 * 当请求触发限流时发布，用于监控和告警
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
@Getter
public class RateLimitExceededEvent extends ApplicationEvent {

    /**
     * 路由 ID
     */
    private final String routeId;

    /**
     * 限流键（IP/用户ID/路径等）
     */
    private final String key;

    /**
     * 限流事件发生时间
     */
    private final Instant eventTime;

    /**
     * 限流响应头信息
     */
    private final Map<String, String> headers;

    /**
     * 创建限流超限事件
     *
     * @param source    事件源
     * @param routeId   路由 ID
     * @param key       限流键
     * @param eventTime 事件发生时间
     * @param headers   响应头信息
     */
    public RateLimitExceededEvent(Object source, String routeId, String key,
                                  Instant eventTime, Map<String, String> headers) {
        super(source);
        this.routeId = routeId;
        this.key = key;
        this.eventTime = eventTime;
        this.headers = headers;
    }

    /**
     * 获取限流事件发生时间
     *
     * @return 事件发生时间
     */
    public Instant getEventTime() {
        return eventTime;
    }

    @Override
    public String toString() {
        return "RateLimitExceededEvent{" +
            "routeId='" + routeId + '\'' +
            ", key='" + key + '\'' +
            ", eventTime=" + eventTime +
            ", headers=" + headers +
            '}';
    }
}
