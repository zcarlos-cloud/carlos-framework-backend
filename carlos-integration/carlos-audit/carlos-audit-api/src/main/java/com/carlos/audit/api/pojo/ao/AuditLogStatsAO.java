package com.carlos.audit.api.pojo.ao;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 审计日志实时统计 API 响应对象
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Data
public class AuditLogStatsAO implements Serializable {

    /** 统计日期 */
    private LocalDate date;

    /** 总记录数 */
    private Long totalCount;

    /** 成功数 */
    private Long successCount;

    /** 失败数 */
    private Long failCount;

    /** 独立用户数 */
    private Long uniqueUsers;

    /** 独立目标对象数 */
    private Long uniqueTargets;

    /** 总耗时（毫秒） */
    private Long totalDuration;

    /** 平均耗时（毫秒） */
    private BigDecimal avgDuration;

    /** 最高风险等级 */
    private Integer maxRiskLevel;

    /** 附件总数 */
    private Long totalAttachments;

    /** 涉及金额 */
    private BigDecimal totalAmount;
}
