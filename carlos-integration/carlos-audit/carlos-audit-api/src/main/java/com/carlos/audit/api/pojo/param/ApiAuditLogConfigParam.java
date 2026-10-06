package com.carlos.audit.api.pojo.param;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 审计日志配置 API 请求参数
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Data
@Accessors(chain = true)
public class ApiAuditLogConfigParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 日志类型 */
    @NotBlank(message = "日志类型不能为空")
    private String logType;

    /** 保留天数 */
    @Min(value = 1, message = "保留天数至少为1")
    @Max(value = 3650, message = "保留天数不能超过3650")
    private Integer retentionDays;

    /** 采样率 0.00-1.00 */
    @NotNull(message = "采样率不能为空")
    @DecimalMin(value = "0.00", message = "采样率不能小于0")
    @DecimalMax(value = "1.00", message = "采样率不能大于1")
    private BigDecimal samplingRate;

    /** 是否异步写入 */
    @NotNull(message = "异步写入开关不能为空")
    private Boolean asyncWrite;

    /** 是否存储数据变更 */
    @NotNull(message = "数据变更存储开关不能为空")
    private Boolean storeDataChange;

    /** 是否存储技术上下文 */
    @NotNull(message = "技术上下文存储开关不能为空")
    private Boolean storeTechnical;

    /** 租户ID，0表示系统级配置 */
    @NotBlank(message = "租户ID不能为空")
    private String tenantId;
}
