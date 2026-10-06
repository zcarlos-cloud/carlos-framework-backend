package com.carlos.audit.clickhouse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ClickHouse 按天聚合统计查询结果行
 *
 * @author Carlos
 * @date 2026-09-17
 */
@Data
public class AuditLogDailyStatsRow {

    @JsonProperty("date")
    private String date;

    @JsonProperty("total_count")
    private Long totalCount;

    @JsonProperty("success_count")
    private Long successCount;

    @JsonProperty("fail_count")
    private Long failCount;

    @JsonProperty("unique_users")
    private Long uniqueUsers;

    @JsonProperty("avg_duration")
    private BigDecimal avgDuration;

    @JsonProperty("max_risk_level")
    private Integer maxRiskLevel;
}
