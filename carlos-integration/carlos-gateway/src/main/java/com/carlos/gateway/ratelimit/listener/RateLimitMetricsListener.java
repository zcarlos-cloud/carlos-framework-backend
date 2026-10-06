package com.carlos.gateway.ratelimit.listener;

import com.carlos.gateway.ratelimit.event.RateLimitExceededEvent;
import com.carlos.gateway.ratelimit.event.RateLimitMetricsEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * 限流指标监听器
 * 收集限流事件并导出到 Micrometer（Prometheus 等）
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
@Slf4j
@Component
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(name = "carlos.gateway.rate-limiter.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitMetricsListener {

    private final MeterRegistry meterRegistry;

    public RateLimitMetricsListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 监听限流超限事件
     */
    @EventListener
    @Async("virtualTaskExecutor")
    public void onRateLimitExceeded(RateLimitExceededEvent event) {
        log.debug("Rate limit exceeded: routeId={}, key={}", event.getRouteId(), event.getKey());

        // 记录限流计数
        Counter.builder("carlos.gateway.rate-limiter.exceeded")
            .description("Rate limit exceeded count")
            .tags(Tags.of(
                "route_id", event.getRouteId(),
                "key", maskKey(event.getKey())
            ))
            .register(meterRegistry)
            .increment();
    }

    /**
     * 监听限流指标事件
     */
    @EventListener
    @Async("virtualTaskExecutor")
    public void onRateLimitMetrics(RateLimitMetricsEvent event) {
        // 记录限流检查结果
        Counter.builder("carlos.gateway.rate-limiter.requests")
            .description("Rate limit check requests")
            .tags(Tags.of(
                "route_id", event.getRouteId(),
                "allowed", String.valueOf(event.isAllowed())
            ))
            .register(meterRegistry)
            .increment();

        // 记录响应时间
        Timer.builder("carlos.gateway.rate-limiter.response-time")
            .description("Rate limit check response time")
            .tags(Tags.of("route_id", event.getRouteId()))
            .register(meterRegistry)
            .record(event.getResponseTimeMs(), TimeUnit.MILLISECONDS);

        // 记录剩余令牌数（Gauge）
        meterRegistry.gauge("carlos.gateway.rate-limiter.tokens-remaining",
            Tags.of("route_id", event.getRouteId()),
            new AtomicInteger((int) event.getTokensRemaining()));
    }

    /**
     * 对 Key 进行脱敏处理（保护隐私）
     */
    private String maskKey(String key) {
        if (key == null || key.length() < 8) {
            return "***";
        }
        // 只显示前 3 位和后 3 位
        return key.substring(0, 3) + "***" + key.substring(key.length() - 3);
    }
}
