package com.carlos.job.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 编程式创建任务 API 参数（供其他微服务通过 Feign 注册任务）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@Schema(description = "编程式创建任务参数")
public class ApiJobCreateParam implements Serializable {

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
     * 目标服务名（Nacos 注册名），任务到点时回调该服务执行
     */
    @NotBlank(message = "目标服务名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9-]{1,64}$", message = "目标服务名仅允许字母、数字、中划线")
    @Schema(description = "目标服务名（Nacos 注册名）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceName;

    /**
     * 任务处理器名称（目标服务中 JobHandler 的 Bean 名称）
     */
    @NotBlank(message = "任务处理器名称不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{1,128}$", message = "任务处理器名称仅允许字母、数字、下划线")
    @Schema(description = "任务处理器名称（JobHandler Bean 名称）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String handlerName;

    /**
     * cron 表达式
     */
    @NotBlank(message = "cron 表达式不能为空")
    @Size(max = 120, message = "cron 表达式长度不能超过 120")
    @Schema(description = "cron 表达式", requiredMode = Schema.RequiredMode.REQUIRED)
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

}
