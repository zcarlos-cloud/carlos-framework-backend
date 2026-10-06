package com.carlos.audit.api.pojo.param;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 审计日志查询 API 请求参数
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Data
@Accessors(chain = true)
public class ApiAuditLogQueryParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户ID */
    private String tenantId;

    /** 页码 */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = 1;

    /** 每页大小 */
    @Min(value = 1, message = "每页大小必须大于等于1")
    @Max(value = 1000, message = "每页大小不能超过1000")
    private Integer pageSize = 20;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 日志类型 */
    private String logType;

    /** 日志状态 */
    private String logStatus;

    /** 用户标识（用户轨迹查询） */
    private String principalId;

    /** 用户名（模糊匹配，登录日志等按账号筛选场景） */
    private String principalName;

    /** 客户端IP（等值匹配，登录日志等按来源筛选场景） */
    private String clientIp;

    /** 认证方式（等值匹配：PASSWORD/SMS/OAUTH2 等，登录日志按认证方式筛选场景） */
    private String authType;

    /** 最小风险等级（风险事件查询） */
    @Min(value = 0, message = "风险等级不能小于0")
    @Max(value = 100, message = "风险等级不能大于100")
    private Integer minRiskLevel = 50;

    /** 统计日期 */
    private LocalDate statsDate;

    /** 日志主键（详情查询） */
    private Long id;
}
