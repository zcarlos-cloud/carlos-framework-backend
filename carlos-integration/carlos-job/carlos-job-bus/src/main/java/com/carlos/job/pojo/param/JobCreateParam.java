package com.carlos.job.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 定时任务 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@Schema(description = "定时任务新增参数")
public class JobCreateParam {

    @NotBlank(message = "任务名称不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{1,100}$", message = "任务名称仅允许字母、数字、下划线、中划线")
    @Schema(description = "任务名称（分组内唯一）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobName;

    @Pattern(regexp = "^[a-zA-Z0-9_-]{1,64}$", message = "任务分组仅允许字母、数字、下划线、中划线")
    @Schema(description = "任务分组，默认 DEFAULT")
    private String jobGroup;

    @NotBlank(message = "目标服务名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9-]{1,64}$", message = "目标服务名仅允许字母、数字、中划线")
    @Schema(description = "目标服务名（Nacos 注册名；boot 单体模式填本应用名）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceName;

    @NotBlank(message = "任务处理器名称不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{1,128}$", message = "任务处理器名称仅允许字母、数字、下划线")
    @Schema(description = "任务处理器名称（JobHandler Bean 名称）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String handlerName;

    @Min(value = 1, message = "调度类型取值 1-CRON 2-固定间隔 3-一次性")
    @Max(value = 3, message = "调度类型取值 1-CRON 2-固定间隔 3-一次性")
    @Schema(description = "调度类型: 1-CRON 2-固定间隔 3-一次性，默认 1")
    private Integer scheduleType;

    @Size(max = 120, message = "调度配置长度不能超过 120")
    @Schema(description = "调度配置：CRON 为 cron 表达式，固定间隔为毫秒数，一次性为触发时间(ISO)")
    private String scheduleConf;

    @Size(max = 120, message = "cron 表达式长度不能超过 120")
    @Schema(description = "cron 表达式（scheduleType=1 时必填；为兼容旧版保留，等价于 scheduleConf）")
    private String cronExpression;

    @Size(max = 4000, message = "任务参数长度不能超过 4000")
    @Schema(description = "任务参数（JSON 字符串）")
    private String params;

    @Size(max = 250, message = "任务描述长度不能超过 250")
    @Schema(description = "任务描述")
    private String description;

    @Min(value = 0, message = "调度过期策略取值 0-3")
    @Max(value = 3, message = "调度过期策略取值 0-3")
    @Schema(description = "调度过期策略: 0-智能策略 1-忽略过期 2-立即补跑一次 3-不补跑，默认 0")
    private Integer misfireStrategy;

    @Min(value = 1, message = "并发模式取值 1-串行 2-并行")
    @Max(value = 2, message = "并发模式取值 1-串行 2-并行")
    @Schema(description = "并发模式: 1-串行(集群互斥) 2-并行，默认 1")
    private Integer concurrentMode;

    @Min(value = 0, message = "任务超时不能为负")
    @Max(value = 86400, message = "任务超时不能超过 86400 秒")
    @Schema(description = "任务超时(秒)，0-不限制，默认 0")
    private Integer timeoutSeconds;

    @Min(value = 0, message = "失败重试次数不能为负")
    @Max(value = 10, message = "失败重试次数不能超过 10")
    @Schema(description = "失败重试次数，0-不重试，默认 0")
    private Integer failRetryCount;

    @Size(max = 64, message = "负责人长度不能超过 64")
    @Schema(description = "负责人")
    private String author;

    @Size(max = 500, message = "报警接收人长度不能超过 500")
    @Schema(description = "报警接收人，多个逗号分隔")
    private String alarmReceivers;

    /**
     * 解析生效的调度配置（优先 scheduleConf，兼容旧的 cronExpression）
     *
     * @return 调度配置
     */
    public String resolveScheduleConf() {
        return scheduleConf != null && !scheduleConf.isBlank() ? scheduleConf : cronExpression;
    }

}
