package com.carlos.oss.health;

import com.carlos.oss.config.OssProperties;
import com.carlos.oss.core.OssTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * OSS 健康检查指示器
 *
 * @author Carlos
 * @date 2026-05-23
 */
@Slf4j
@RequiredArgsConstructor
public class OssHealthIndicator implements HealthIndicator {

    private final OssTemplate ossTemplate;
    private final OssProperties ossProperties;

    @Override
    public Health health() {
        try {
            String bucketName = ossProperties.getBucketName();
            boolean exists = ossTemplate.bucketExists(bucketName);
            return Health.up()
                .withDetail("defaultBucket", bucketName)
                .withDetail("bucketExists", exists)
                .build();
        } catch (Exception e) {
            log.error("OSS health check failed", e);
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
