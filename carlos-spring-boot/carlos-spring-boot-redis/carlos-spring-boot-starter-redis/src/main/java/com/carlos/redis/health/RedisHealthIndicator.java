package com.carlos.redis.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Redis 健康检查指示器
 *
 * @author Carlos
 * @date 2026-05-23
 */
@Slf4j
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory connectionFactory;

    @Override
    public Health health() {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            String pong = connection.ping();
            return Health.up()
                .withDetail("ping", pong)
                .build();
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
