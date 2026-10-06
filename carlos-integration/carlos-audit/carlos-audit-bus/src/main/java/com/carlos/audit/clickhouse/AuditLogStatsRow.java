package com.carlos.audit.clickhouse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ClickHouse 统计查询结果行
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Data
public class AuditLogStatsRow {

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

    @JsonProperty("unique_targets")
    private Long uniqueTargets;

    @JsonProperty("total_duration")
    private Long totalDuration;

    @JsonProperty("avg_duration")
    private BigDecimal avgDuration;

    @JsonProperty("max_risk_level")
    private Integer maxRiskLevel;

    @JsonProperty("total_attachments")
    private Long totalAttachments;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;
}
