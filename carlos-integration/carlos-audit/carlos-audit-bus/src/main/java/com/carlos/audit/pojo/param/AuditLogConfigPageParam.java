package com.carlos.audit.pojo.param;


import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * <p>
 * 审计日志配置（动态TTL与采样策略） 列表查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "审计日志配置（动态TTL与采样策略）列表查询参数")
public class AuditLogConfigPageParam extends ParamPage {
    @Schema(description = "日志类型，如：USER_LOGIN")
    private String logType;
    @Schema(description = "保留天数")
    private Integer retentionDays;
    @Schema(description = "采样率 0.00-1.00，1.00为全量")
    private BigDecimal samplingRate;
    @Schema(description = "是否异步写入：0-同步/1-异步")
    private Boolean asyncWrite;
    @Schema(description = "是否存储数据变更：0-否/1-是")
    private Boolean storeDataChange;
    @Schema(description = "是否存储技术上下文：0-否/1-是")
    private Boolean storeTechnical;
    @Schema(description = "租户ID，0表示系统级配置")
    private String tenantId;
    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
