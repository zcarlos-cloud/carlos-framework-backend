package com.carlos.job.pojo.ao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 任务信息 API 对象
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@Schema(description = "任务信息")
public class JobInfoAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务名称
     */
    @Schema(description = "任务名称")
    private String jobName;

    /**
     * 任务分组
     */
    @Schema(description = "任务分组")
    private String jobGroup;

    /**
     * 目标服务名
     */
    @Schema(description = "目标服务名")
    private String serviceName;

    /**
     * 任务处理器名称
     */
    @Schema(description = "任务处理器名称")
    private String handlerName;

    /**
     * cron 表达式
     */
    @Schema(description = "cron 表达式")
    private String cronExpression;

    /**
     * 任务状态（Quartz Trigger 状态：NORMAL/PAUSED/ERROR 等）
     */
    @Schema(description = "任务状态")
    private String status;

    /**
     * 上次触发时间
     */
    @Schema(description = "上次触发时间")
    private LocalDateTime prevFireTime;

    /**
     * 下次触发时间
     */
    @Schema(description = "下次触发时间")
    private LocalDateTime nextFireTime;

}
