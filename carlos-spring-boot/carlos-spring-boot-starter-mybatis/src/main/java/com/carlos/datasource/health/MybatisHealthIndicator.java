package com.carlos.datasource.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据源健康检查指示器
 *
 * @author Carlos
 * @date 2026-05-23
 */
@Slf4j
@RequiredArgsConstructor
public class MybatisHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(5);
            return Health.up()
                .withDetail("connectionValid", valid)
                .build();
        } catch (SQLException e) {
            log.error("DataSource health check failed", e);
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
