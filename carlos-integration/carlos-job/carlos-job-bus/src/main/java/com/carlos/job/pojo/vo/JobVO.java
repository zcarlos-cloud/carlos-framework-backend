package com.carlos.job.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 定时任务 显示对象
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@Schema(description = "定时任务")
public class JobVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务分组")
    private String jobGroup;

    @Schema(description = "目标服务名")
    private String serviceName;

    @Schema(description = "任务处理器名称")
    private String handlerName;

    @Schema(description = "任务参数（JSON 字符串）")
    private String params;

    @Schema(description = "cron 表达式（scheduleType=1 时有值）")
    private String cronExpression;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "任务状态: NORMAL-正常 PAUSED-暂停 ERROR-异常")
    private String status;

    @Schema(description = "调度类型: 1-CRON 2-固定间隔 3-一次性")
    private Integer scheduleType;

    @Schema(description = "调度配置: cron 表达式/固定间隔毫秒/触发时间(ISO)")
    private String scheduleConf;

    @Schema(description = "调度过期策略: 0-智能策略 1-忽略过期 2-立即补跑一次 3-不补跑")
    private Integer misfireStrategy;

    @Schema(description = "并发模式: 1-串行 2-并行")
    private Integer concurrentMode;

    @Schema(description = "任务超时(秒)，0-不限制")
    private Integer timeoutSeconds;

    @Schema(description = "失败重试次数，0-不重试")
    private Integer failRetryCount;

    @Schema(description = "负责人")
    private String author;

    @Schema(description = "报警接收人，多个逗号分隔")
    private String alarmReceivers;

    @Schema(description = "上次触发时间")
    private LocalDateTime prevFireTime;

    @Schema(description = "下次触发时间")
    private LocalDateTime nextFireTime;

}
