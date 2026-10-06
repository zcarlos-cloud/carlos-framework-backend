package com.carlos.job.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 任务执行日志 显示对象
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@Schema(description = "任务执行日志")
public class JobExecutionLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务分组")
    private String jobGroup;

    @Schema(description = "目标服务名")
    private String serviceName;

    @Schema(description = "任务处理器名称")
    private String handlerName;

    @Schema(description = "触发类型: 1-cron定时 2-手动触发")
    private Integer triggerType;

    @Schema(description = "cron 表达式")
    private String cronExpression;

    @Schema(description = "执行状态: 1-成功 2-失败 3-执行中")
    private Integer status;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "耗时（毫秒）")
    private Long durationMs;

    @Schema(description = "失败原因/结果说明")
    private String resultMessage;

    @Schema(description = "执行节点")
    private String serverNode;

}
