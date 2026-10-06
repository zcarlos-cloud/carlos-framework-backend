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
 * 告警规则 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@TableName("monitor_alarm_rule")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitorAlarmRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 规则名称
     */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 指标来源：REDIS/DB/SERVICE
     */
    @TableField("metric_source")
    private String metricSource;

    /**
     * 作用范围：INSTANCE=单实例，GROUP=分组，ALL=全部
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 作用对象ID（target_type=ALL 时为 NULL）
     */
    @TableField("target_id")
    private Long targetId;

    /**
     * 指标名（如 redis.memory.used_percent）
     */
    @TableField("metric_name")
    private String metricName;

    /**
     * 比较符：GT/GTE/LT/LTE/EQ/NE/CHANGE_RATE_GT/CHANGE_RATE_LT
     */
    @TableField("condition_op")
    private String conditionOp;

    /**
     * 阈值
     */
    @TableField("threshold_value")
    private BigDecimal thresholdValue;

    /**
     * 连续触发周期数（防抖动）
     */
    @TableField("duration_count")
    private Integer durationCount;

    /**
     * 告警级别（字典 monitor_alarm_level）：INFO/WARN/CRITICAL
     */
    @TableField("alarm_level")
    private String alarmLevel;

    /**
     * 通知渠道逗号分隔：SITE/MAIL/DINGTALK
     */
    @TableField("notify_channels")
    private String notifyChannels;

    /**
     * 通知目标JSON：{"userIds":[],"roleIds":[]}
     */
    @TableField("notify_targets")
    private String notifyTargets;

    /**
     * 启用开关
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

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
