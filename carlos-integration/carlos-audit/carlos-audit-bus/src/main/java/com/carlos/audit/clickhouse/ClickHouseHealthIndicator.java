package com.carlos.audit.clickhouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * <p>
 * ClickHouse 健康检查指示器
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@Slf4j
@RequiredArgsConstructor
public class ClickHouseHealthIndicator implements HealthIndicator {

    private final ClickHouseBatchWriter batchWriter;

    @Override
    public Health health() {
        try {
            // 检查缓冲区状态
            int bufferSize = batchWriter.getActiveBufferSize();
            long totalWritten = batchWriter.getTotalWritten();
            long totalFailed = batchWriter.getTotalFailed();
            long bufferOverflow = batchWriter.getBufferOverflow();

            // 如果失败数过多或缓冲区溢出过多，标记为降级
            if (bufferOverflow > 10 || totalFailed > 100) {
                return Health.down()
                    .withDetail("clickHouse", "degraded")
                    .withDetail("bufferSize", bufferSize)
                    .withDetail("totalWritten", totalWritten)
                    .withDetail("totalFailed", totalFailed)
                    .withDetail("bufferOverflow", bufferOverflow)
                    .build();
            }

            return Health.up()
                .withDetail("clickHouse", "up")
                .withDetail("bufferSize", bufferSize)
                .withDetail("totalWritten", totalWritten)
                .withDetail("totalFailed", totalFailed)
                .build();

        } catch (Exception e) {
            log.error("ClickHouse 健康检查失败", e);
            return Health.down()
                .withDetail("clickHouse", "down")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
