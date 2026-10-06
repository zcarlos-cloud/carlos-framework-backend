package com.carlos.mongodb.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * MongoDB 健康检查指示器
 *
 * @author Carlos
 * @date 2026-05-23
 */
@Slf4j
@RequiredArgsConstructor
public class MongoHealthIndicator implements HealthIndicator {

    private final MongoTemplate mongoTemplate;

    @Override
    public Health health() {
        try {
            org.bson.Document result = mongoTemplate.executeCommand("{ ping: 1 }");
            return Health.up()
                .withDetail("ping", result.getDouble("ok"))
                .build();
        } catch (Exception e) {
            log.error("MongoDB health check failed", e);
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
