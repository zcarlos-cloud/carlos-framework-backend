package com.carlos.redis.stream;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * Redis Stream 健康指示器
 * <p>
 * <b>与 {@code RedisHealthIndicator} 的区别</b>：后者只探测连接可用性（{@code PING}），
 * 属于缓存语义。本指示器面向 Stream，做一次真实的 {@code INFO memory} 往返，
 * 并把关键流的长度与实例内存作为明细暴露出来。
 * </p>
 * <p>
 * <b>刻意不把积压判为 DOWN</b>：积压是容量信号而非故障信号，把整个应用判为 DOWN
 * 会触发无意义的实例重启（还会打断正在进行的消费）。积压只作为 {@code warn} 明细呈现，
 * 由告警规则消费。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@RequiredArgsConstructor
public class RedisStreamHealthIndicator implements HealthIndicator {

    private final RedisStreamTemplate streamTemplate;

    @Override
    public Health health() {
        long usedMemory;
        try {
            usedMemory = streamTemplate.usedMemoryBytes();
        } catch (Exception e) {
            return Health.down()
                .withDetail("reason", "Redis Stream 不可达")
                .withException(e)
                .build();
        }
        if (usedMemory < 0) {
            return Health.down()
                .withDetail("reason", "INFO memory 往返失败")
                .build();
        }
        return Health.up()
            .withDetail("usedMemoryBytes", usedMemory)
            .build();
    }
}
