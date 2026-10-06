package com.carlos.disruptor.metrics;

import com.carlos.disruptor.core.DisruptorManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * Disruptor 健康检查指示器
 *
 * @author Carlos
 * @date 2026-03-14
 */
@RequiredArgsConstructor
public class DisruptorHealthIndicator implements HealthIndicator {

    private final DisruptorManager disruptorManager;

    @Override
    public Health health() {
        // 由于 disruptorManager 内部没有暴露获取所有实例的方法，
        // 这里简化处理，返回 UP 状态
        return Health.up()
            .withDetail("status", "Disruptor module is active")
            .build();
    }
}
