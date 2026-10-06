package com.carlos.job.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 自动注册任务参数（业务服务启动时按注解向调度中心注册）
 * </p>
 *
 * <p><b>幂等语义</b>：以 {@code (jobName, jobGroup)} 为业务键，已存在则跳过、不覆盖
 * （{@code job_info} 是唯一真相源，运维在后台的修改不会被发布回滚）。字段名需与
 * {@code JobCreateParam} 保持一致，供对象转换逐字段映射。</p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Data
@Accessors(chain = true)
@Schema(description = "自动注册任务参数")
public class ApiJobRegisterParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务名称（分组内唯一）
     */
    @NotBlank(message = "任务名称不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{1,100}$", message = "任务名称仅允许字母、数字、下划线、中划线")
    @Schema(description = "任务名称（分组内唯一）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobName;

    /**
     * 任务分组，默认 DEFAULT
     */
    @Pattern(regexp = "^[a-zA-Z0-9_-]{1,64}$", message = "任务分组仅允许字母、数字、下划线、中划线")
    @Schema(description = "任务分组")
    private String jobGroup;

    /**
     * 目标服务名（Nacos 注册名）
     */
    @NotBlank(message = "目标服务名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9-]{1,64}$", message = "目标服务名仅允许字母、数字、中划线")
    @Schema(description = "目标服务名（Nacos 注册名）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceName;

    /**
     * 任务处理器名称（注解 {@code @ScheduleJob} 的取值）
     */
    @NotBlank(message = "任务处理器名称不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{1,100}$", message = "任务处理器名称仅允许字母、数字、下划线")
    @Schema(description = "任务处理器名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String handlerName;

    /**
     * 调度类型：1 Cron 2 固定间隔 3 一次性；为空按 CRON
     */
    @Min(value = 1, message = "调度类型不合法")
    @Max(value = 3, message = "调度类型不合法")
    @Schema(description = "调度类型：1 Cron 2 固定间隔 3 一次性")
    private Integer scheduleType;

    /**
     * 调度配置（CRON 表达式 / 间隔毫秒 / ISO 时间）
     */
    @Size(max = 120, message = "调度配置长度不能超过 120")
    @Schema(description = "调度配置")
    private String scheduleConf;

    /**
     * cron 表达式（兼容字段，scheduleConf 优先）
     */
    @Size(max = 120, message = "cron 表达式长度不能超过 120")
    @Schema(description = "cron 表达式（兼容字段）")
    private String cronExpression;

    /**
     * 任务参数（JSON 字符串）
     */
    @Size(max = 4000, message = "任务参数长度不能超过 4000")
    @Schema(description = "任务参数（JSON 字符串）")
    private String params;

    /**
     * 任务描述
     */
    @Size(max = 250, message = "任务描述长度不能超过 250")
    @Schema(description = "任务描述")
    private String description;

    /**
     * 过期策略：0 智能 1 忽略过期 2 立即补跑一次 3 不补跑
     */
    @Min(value = 0, message = "过期策略不合法")
    @Max(value = 3, message = "过期策略不合法")
    @Schema(description = "过期策略：0 智能 1 忽略过期 2 立即补跑一次 3 不补跑")
    private Integer misfireStrategy;

    /**
     * 并发模式：1 串行 2 并行
     */
    @Min(value = 1, message = "并发模式不合法")
    @Max(value = 2, message = "并发模式不合法")
    @Schema(description = "并发模式：1 串行 2 并行")
    private Integer concurrentMode;

    /**
     * 超时秒数，0 表示不限制
     */
    @Min(value = 0, message = "超时时间不合法")
    @Max(value = 86400, message = "超时时间不合法")
    @Schema(description = "超时秒数，0 表示不限制")
    private Integer timeoutSeconds;

    /**
     * 失败重试次数
     */
    @Min(value = 0, message = "失败重试次数不合法")
    @Max(value = 10, message = "失败重试次数不合法")
    @Schema(description = "失败重试次数")
    private Integer failRetryCount;

    /**
     * 负责人
     */
    @Size(max = 64, message = "负责人长度不能超过 64")
    @Schema(description = "负责人")
    private String author;

    /**
     * 报警接收人，多个逗号分隔
     */
    @Size(max = 500, message = "报警接收人长度不能超过 500")
    @Schema(description = "报警接收人，多个逗号分隔")
    private String alarmReceivers;

    /**
     * 初始状态：NORMAL / PAUSED，为空按 NORMAL
     */
    @Pattern(regexp = "^(NORMAL|PAUSED)?$", message = "任务状态仅允许 NORMAL 或 PAUSED")
    @Schema(description = "初始状态：NORMAL / PAUSED")
    private String status;

}
