package com.carlos.monitor.alarm.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 告警规则 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@Schema(description = "告警规则新增参数")
public class AlarmRuleCreateParam {

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称长度不能超过100")
    @Schema(description = "规则名称")
    private String ruleName;

    @NotBlank(message = "指标来源不能为空")
    @Schema(description = "指标来源：REDIS/DB/SERVICE")
    private String metricSource;

    @NotBlank(message = "作用范围不能为空")
    @Schema(description = "作用范围：INSTANCE/GROUP/ALL")
    private String targetType = "INSTANCE";

    @Schema(description = "作用对象ID（INSTANCE=实例ID，GROUP=分组ID，ALL 为空）")
    private Long targetId;

    @NotBlank(message = "指标名不能为空")
    @Size(max = 100, message = "指标名长度不能超过100")
    @Schema(description = "指标名（如 redis.memory.used_percent）")
    private String metricName;

    @NotBlank(message = "比较符不能为空")
    @Schema(description = "比较符：GT/GTE/LT/LTE/EQ/NE/CHANGE_RATE_GT/CHANGE_RATE_LT")
    private String conditionOp;

    @NotNull(message = "阈值不能为空")
    @Schema(description = "阈值")
    private BigDecimal thresholdValue;

    @Min(value = 1, message = "连续触发周期数不能小于1")
    @Max(value = 100, message = "连续触发周期数不能大于100")
    @Schema(description = "连续触发周期数（防抖动），默认 1")
    private Integer durationCount = 1;

    @Schema(description = "告警级别：INFO/WARN/CRITICAL，默认 WARN")
    private String alarmLevel = "WARN";

    @Size(max = 200, message = "通知渠道长度不能超过200")
    @Schema(description = "通知渠道逗号分隔：SITE/MAIL/DINGTALK，默认 SITE")
    private String notifyChannels = "SITE";

    @Schema(description = "通知用户ID列表")
    private List<Long> userIds;

    @Schema(description = "通知角色ID列表")
    private List<Long> roleIds;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;
}
