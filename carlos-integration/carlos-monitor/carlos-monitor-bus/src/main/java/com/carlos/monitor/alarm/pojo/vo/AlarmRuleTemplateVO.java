package com.carlos.monitor.alarm.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 内置告警规则模板 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@Schema(description = "内置告警规则模板视图对象")
public class AlarmRuleTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "指标来源：REDIS/DB/SERVICE")
    private String metricSource;

    @Schema(description = "指标名")
    private String metricName;

    @Schema(description = "比较符")
    private String conditionOp;

    @Schema(description = "阈值")
    private BigDecimal thresholdValue;

    @Schema(description = "连续触发周期数")
    private Integer durationCount;

    @Schema(description = "告警级别")
    private String alarmLevel;

    @Schema(description = "模板说明")
    private String description;
}
