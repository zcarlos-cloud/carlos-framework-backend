package com.carlos.job.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 任务执行日志 数据传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
public class JobExecutionLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务分组
     */
    private String jobGroup;

    /**
     * 目标服务名
     */
    private String serviceName;

    /**
     * 任务处理器名称
     */
    private String handlerName;

    /**
     * 触发类型: 1-cron定时 2-手动触发
     */
    private Integer triggerType;

    /**
     * cron 表达式
     */
    private String cronExpression;

    /**
     * 执行状态: 1-成功 2-失败 3-执行中
     */
    private Integer status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 失败原因/结果说明
     */
    private String resultMessage;

    /**
     * 执行节点
     */
    private String serverNode;

}
