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
import java.time.LocalDateTime;

/**
 * <p>
 * 告警静默 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@TableName("monitor_alarm_silence")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitorAlarmSilence implements Serializable {

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
     * 实例ID，NULL 表示该规则下全部实例静默
     */
    @TableField("instance_id")
    private Long instanceId;

    /**
     * 静默开始时间
     */
    @TableField("silence_start")
    private LocalDateTime silenceStart;

    /**
     * 静默结束时间
     */
    @TableField("silence_end")
    private LocalDateTime silenceEnd;

    /**
     * 静默原因
     */
    @TableField("reason")
    private String reason;

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
