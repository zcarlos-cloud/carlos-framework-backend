package com.carlos.monitor.alarm.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 告警记录 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@TableName("monitor_alarm_record")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitorAlarmRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 规则ID
     */
    @TableField("rule_id")
    private Long ruleId;

    /**
     * 规则名称（冗余，防规则改名失联）
     */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 实例ID
     */
    @TableField("instance_id")
    private Long instanceId;

    /**
     * 实例名称（冗余）
     */
    @TableField("instance_name")
    private String instanceName;

    /**
     * 指标名
     */
    @TableField("metric_name")
    private String metricName;

    /**
     * 触发时指标值
     */
    @TableField("metric_value")
    private BigDecimal metricValue;

    /**
     * 阈值
     */
    @TableField("threshold_value")
    private BigDecimal thresholdValue;

    /**
     * 告警级别
     */
    @TableField("alarm_level")
    private String alarmLevel;

    /**
     * 状态：FIRING=触发中，CONFIRMED=已确认，RECOVERED=已恢复
     */
    @TableField("alarm_status")
    private String alarmStatus;

    /**
     * 告警内容（通知文案）
     */
    @TableField("alarm_content")
    private String alarmContent;

    /**
     * 首次触发时间
     */
    @TableField("first_trigger_time")
    private LocalDateTime firstTriggerTime;

    /**
     * 最近触发时间（未恢复重复触发时刷新）
     */
    @TableField("last_trigger_time")
    private LocalDateTime lastTriggerTime;

    /**
     * 恢复时间
     */
    @TableField("recover_time")
    private LocalDateTime recoverTime;

    /**
     * 通知状态：PENDING/SENT/FAILED/SILENCED
     */
    @TableField("notify_status")
    private String notifyStatus;

    /**
     * 确认人
     */
    @TableField("confirm_by")
    private Long confirmBy;

    /**
     * 确认时间
     */
    @TableField("confirm_time")
    private LocalDateTime confirmTime;

    /**
     * 处理备注
     */
    @TableField("handle_remark")
    private String handleRemark;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 修改人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
