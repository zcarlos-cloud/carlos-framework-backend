package com.carlos.audit.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <p>
 * 审计日志按天聚合统计 VO（趋势图数据源）
 * </p>
 *
 * @author Carlos
 * @date 2026-09-17
 */
@Data
@Schema(description = "审计日志按天聚合统计信息")
public class AuditLogDailyStatsVO {

    @Schema(description = "统计日期")
    private LocalDate date;

    @Schema(description = "日志总数")
    private Long totalCount;

    @Schema(description = "成功数")
    private Long successCount;

    @Schema(description = "失败数")
    private Long failCount;

    @Schema(description = "独立用户数")
    private Long uniqueUsers;

    @Schema(description = "平均耗时(ms)")
    private BigDecimal avgDuration;

    @Schema(description = "最高风险等级")
    private Integer maxRiskLevel;
}
