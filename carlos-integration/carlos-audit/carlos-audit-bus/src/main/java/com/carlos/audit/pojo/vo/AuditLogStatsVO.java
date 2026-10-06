package com.carlos.audit.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <p>
 * 审计日志统计 VO
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@Data
@Schema(description = "审计日志统计信息")
public class AuditLogStatsVO {

    @Schema(description = "统计日期")
    private LocalDate date;

    @Schema(description = "小时")
    private Integer hour;

    @Schema(description = "日志总数")
    private Long totalCount;

    @Schema(description = "成功数")
    private Long successCount;

    @Schema(description = "失败数")
    private Long failCount;

    @Schema(description = "独立用户数")
    private Long uniqueUsers;

    @Schema(description = "独立目标数")
    private Long uniqueTargets;

    @Schema(description = "总耗时(ms)")
    private Long totalDuration;

    @Schema(description = "平均耗时(ms)")
    private BigDecimal avgDuration;

    @Schema(description = "最高风险等级")
    private Integer maxRiskLevel;

    @Schema(description = "附件总数")
    private Long totalAttachments;

    @Schema(description = "涉及金额")
    private BigDecimal totalAmount;
}
