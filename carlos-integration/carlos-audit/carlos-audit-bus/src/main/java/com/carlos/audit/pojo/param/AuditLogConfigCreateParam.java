package com.carlos.audit.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;


/**
 * <p>
 * 审计日志配置（动态TTL与采样策略） 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Data
@Accessors(chain = true)
@Schema(description = "审计日志配置（动态TTL与采样策略）新增参数")
public class AuditLogConfigCreateParam {
    @NotBlank(message = "日志类型，如：USER_LOGIN不能为空")
    @Schema(description = "日志类型，如：USER_LOGIN")
    private String logType;
    @NotNull(message = "保留天数不能为空")
    @Schema(description = "保留天数")
    private Integer retentionDays;
    @NotNull(message = "采样率 0.00-1.00，1.00为全量不能为空")
    @Schema(description = "采样率 0.00-1.00，1.00为全量")
    private BigDecimal samplingRate;
    @NotNull(message = "是否异步写入：0-同步/1-异步不能为空")
    @Schema(description = "是否异步写入：0-同步/1-异步")
    private Boolean asyncWrite;
    @NotNull(message = "是否存储数据变更：0-否/1-是不能为空")
    @Schema(description = "是否存储数据变更：0-否/1-是")
    private Boolean storeDataChange;
    @NotNull(message = "是否存储技术上下文：0-否/1-是不能为空")
    @Schema(description = "是否存储技术上下文：0-否/1-是")
    private Boolean storeTechnical;
    @NotBlank(message = "租户ID，0表示系统级配置不能为空")
    @Schema(description = "租户ID，0表示系统级配置")
    private String tenantId;
}
