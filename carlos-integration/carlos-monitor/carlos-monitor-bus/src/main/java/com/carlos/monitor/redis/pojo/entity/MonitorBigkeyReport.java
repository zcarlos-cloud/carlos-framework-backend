package com.carlos.monitor.redis.pojo.entity;

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
 * Redis 大key/热key 分析报告 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@TableName("monitor_bigkey_report")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitorBigkeyReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * Redis 实例ID
     */
    @TableField("instance_id")
    private Long instanceId;

    /**
     * 报告类型：BIGKEY/HOTKEY
     */
    @TableField("report_type")
    private String reportType;

    /**
     * 触发方式：MANUAL/SCHEDULED
     */
    @TableField("trigger_type")
    private String triggerType;

    /**
     * 扫描状态：RUNNING/SUCCESS/FAILED
     */
    @TableField("scan_status")
    private String scanStatus;

    /**
     * 已扫描 key 数量
     */
    @TableField("scanned_keys")
    private Long scannedKeys;

    /**
     * 采样率（%），100=全量 SCAN
     */
    @TableField("sample_rate")
    private Integer sampleRate;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 失败原因
     */
    @TableField("error_msg")
    private String errorMsg;

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
