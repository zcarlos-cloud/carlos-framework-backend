package com.carlos.job.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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
 * 任务执行日志 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("job_execution_log")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobExecutionLog extends Model<JobExecutionLog> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 任务名称
     */
    @TableField(value = "job_name")
    private String jobName;

    /**
     * 任务分组
     */
    @TableField(value = "job_group")
    private String jobGroup;

    /**
     * 目标服务名
     */
    @TableField(value = "service_name")
    private String serviceName;

    /**
     * 任务处理器名称
     */
    @TableField(value = "handler_name")
    private String handlerName;

    /**
     * 触发类型: 1-cron定时 2-手动触发
     */
    @TableField(value = "trigger_type")
    private Integer triggerType;

    /**
     * cron 表达式
     */
    @TableField(value = "cron_expression")
    private String cronExpression;

    /**
     * 执行状态: 1-成功 2-失败 3-执行中
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 开始时间
     */
    @TableField(value = "start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField(value = "end_time")
    private LocalDateTime endTime;

    /**
     * 耗时（毫秒）
     */
    @TableField(value = "duration_ms")
    private Long durationMs;

    /**
     * 失败原因/结果说明
     */
    @TableField(value = "result_message")
    private String resultMessage;

    /**
     * 执行节点（调度中心实例）
     */
    @TableField(value = "server_node")
    private String serverNode;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
