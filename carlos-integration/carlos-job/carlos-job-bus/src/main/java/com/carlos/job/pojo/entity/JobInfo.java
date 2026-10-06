package com.carlos.job.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 定时任务元数据 数据源对象（控制面）
 * </p>
 * <p>
 * Quartz(QRTZ_* 表)是执行面，本表是任务元数据与策略配置的唯一事实源
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("job_info")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobInfo extends Model<JobInfo> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 任务名称（分组内唯一）
     */
    @TableField(value = "job_name")
    private String jobName;

    /**
     * 任务分组
     */
    @TableField(value = "job_group")
    private String jobGroup;

    /**
     * 目标服务名（Nacos 注册名）
     */
    @TableField(value = "service_name")
    private String serviceName;

    /**
     * 任务处理器名称（JobHandler Bean 名称）
     */
    @TableField(value = "handler_name")
    private String handlerName;

    /**
     * 调度类型: 1-CRON 2-固定间隔 3-一次性
     */
    @TableField(value = "schedule_type")
    private Integer scheduleType;

    /**
     * 调度配置: cron 表达式/固定间隔毫秒/一次性触发时间
     */
    @TableField(value = "schedule_conf")
    private String scheduleConf;

    /**
     * 任务参数（JSON 字符串）
     */
    @TableField(value = "params")
    private String params;

    /**
     * 任务描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 调度过期策略: 0-智能策略 1-忽略过期 2-立即补跑一次 3-不补跑
     */
    @TableField(value = "misfire_strategy")
    private Integer misfireStrategy;

    /**
     * 并发模式: 1-串行(集群互斥) 2-并行
     */
    @TableField(value = "concurrent_mode")
    private Integer concurrentMode;

    /**
     * 任务超时（秒），0-不限制
     */
    @TableField(value = "timeout_seconds")
    private Integer timeoutSeconds;

    /**
     * 失败重试次数，0-不重试
     */
    @TableField(value = "fail_retry_count")
    private Integer failRetryCount;

    /**
     * 负责人
     */
    @TableField(value = "author")
    private String author;

    /**
     * 报警接收人，多个逗号分隔
     */
    @TableField(value = "alarm_receivers")
    private String alarmReceivers;

    /**
     * 任务状态（Quartz 触发器状态）: NORMAL-正常 PAUSED-暂停
     */
    @TableField(value = "status")
    private String status;

    /**
     * 创建者编号
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者编号
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除: 0-否 1-是
     */
    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;

}
