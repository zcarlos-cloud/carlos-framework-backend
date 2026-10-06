package com.carlos.gateway.ratelimit.listener;

import com.carlos.gateway.ratelimit.event.RateLimitExceededEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * 限流告警监听器
 * 实现简单的限流告警机制，可用于对接告警系统（钉钉/企业微信/邮件等）
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "carlos.gateway.rate-limiter.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAlertListener {

    /**
     * 限流计数窗口（5分钟）
     */
    private static final Duration ALERT_WINDOW = Duration.ofMinutes(5);

    /**
     * 触发告警的阈值（5分钟内超过100次）
     */
    private static final int ALERT_THRESHOLD = 100;

    /**
     * 限流计数器：key -> (count, lastExceededTime)
     */
    private final Map<String, RateLimitCounter> counters = new ConcurrentHashMap<>();

    /**
     * 监听限流超限事件
     */
    @EventListener
    @Async("virtualTaskExecutor")
    public void onRateLimitExceeded(RateLimitExceededEvent event) {
        String key = event.getRouteId() + ":" + event.getKey();
        Instant now = event.getEventTime();

        RateLimitCounter counter = counters.computeIfAbsent(key, k -> new RateLimitCounter());

        // 检查是否需要重置计数器（窗口过期）
        if (counter.isWindowExpired(now)) {
            counter.reset(now);
        }

        // 增加计数
        int count = counter.increment();

        // 检查是否触发告警
        if (count == ALERT_THRESHOLD) {
            triggerAlert(event, count);
        }

        // 清理过期数据
        cleanupExpiredCounters(now);
    }

    /**
     * 触发告警
     */
    private void triggerAlert(RateLimitExceededEvent event, int count) {
        log.warn("[RATE-LIMIT-ALERT] Route '{}' key '{}' exceeded rate limit {} times in {} minutes",
            event.getRouteId(),
            maskKey(event.getKey()),
            count,
            ALERT_WINDOW.toMinutes()
        );

        // TODO: 对接实际告警系统
        // - 钉钉/企业微信机器人
        // - 邮件通知
        // - SMS 告警
        // - 集成 Prometheus Alertmanager
    }

    /**
     * 清理过期计数器
     */
    private void cleanupExpiredCounters(Instant now) {
        counters.entrySet().removeIf(entry -> entry.getValue().isWindowExpired(now));
    }

    /**
     * 对 Key 进行脱敏
     */
    private String maskKey(String key) {
        if (key == null || key.length() < 8) {
            return "***";
        }
        return key.substring(0, 3) + "***" + key.substring(key.length() - 3);
    }

    /**
     * 限流计数器
     */
    private static class RateLimitCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile Instant windowStart;

        public RateLimitCounter() {
            this.windowStart = Instant.now();
        }

        public int increment() {
            return count.incrementAndGet();
        }

        public boolean isWindowExpired(Instant now) {
            return Duration.between(windowStart, now).compareTo(ALERT_WINDOW) > 0;
        }

        public void reset(Instant now) {
            count.set(0);
            windowStart = now;
        }
    }
}
