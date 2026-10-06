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
 * 告警记录 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "告警记录视图对象")
public class AlarmRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "实例ID")
    private Long instanceId;

    @Schema(description = "实例名称")
    private String instanceName;

    @Schema(description = "指标名")
    private String metricName;

    @Schema(description = "触发时指标值")
    private BigDecimal metricValue;

    @Schema(description = "阈值")
    private BigDecimal thresholdValue;

    @Schema(description = "告警级别")
    private String alarmLevel;

    @Schema(description = "状态：FIRING/CONFIRMED/RECOVERED")
    private String alarmStatus;

    @Schema(description = "告警内容")
    private String alarmContent;

    @Schema(description = "首次触发时间")
    private LocalDateTime firstTriggerTime;

    @Schema(description = "最近触发时间")
    private LocalDateTime lastTriggerTime;

    @Schema(description = "恢复时间")
    private LocalDateTime recoverTime;

    @Schema(description = "通知状态：PENDING/SENT/FAILED/SILENCED")
    private String notifyStatus;

    @Schema(description = "确认人")
    private Long confirmBy;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "处理备注")
    private String handleRemark;
}
