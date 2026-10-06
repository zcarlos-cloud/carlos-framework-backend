package com.carlos.monitor.alarm.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 告警规则 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "告警规则视图对象")
public class AlarmRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "规则ID")
    private Long id;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "指标来源：REDIS/DB/SERVICE")
    private String metricSource;

    @Schema(description = "作用范围：INSTANCE/GROUP/ALL")
    private String targetType;

    @Schema(description = "作用对象ID")
    private Long targetId;

    @Schema(description = "指标名")
    private String metricName;

    @Schema(description = "比较符：GT/GTE/LT/LTE/EQ/NE/CHANGE_RATE_GT/CHANGE_RATE_LT")
    private String conditionOp;

    @Schema(description = "阈值")
    private BigDecimal thresholdValue;

    @Schema(description = "连续触发周期数")
    private Integer durationCount;

    @Schema(description = "告警级别：INFO/WARN/CRITICAL")
    private String alarmLevel;

    @Schema(description = "通知渠道逗号分隔：SITE/MAIL/DINGTALK")
    private String notifyChannels;

    @Schema(description = "通知目标（JSON 转对象）")
    private NotifyTargetsVO notifyTargets;

    @Schema(description = "启用开关")
    private Boolean enabled;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;
}
