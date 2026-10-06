package com.carlos.monitor.servicecollector.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 日志级别调整 参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "日志级别调整参数")
public class LoggerUpdateParam {

    @NotBlank(message = "实例地址不能为空")
    @Size(max = 50, message = "实例地址长度不能超过50")
    @Schema(description = "实例地址 ip:port")
    private String instanceAddr;

    @NotBlank(message = "logger 名不能为空")
    @Size(max = 200, message = "logger 名长度不能超过200")
    @Schema(description = "logger 名（如 com.carlos.monitor）")
    private String loggerName;

    @NotBlank(message = "级别不能为空")
    @Schema(description = "目标级别：TRACE/DEBUG/INFO/WARN/ERROR/OFF")
    private String level;

    @NotNull(message = "有效期不能为空")
    @Min(value = 60, message = "有效期不能小于60秒")
    @Max(value = 86400, message = "有效期不能超过86400秒")
    @Schema(description = "有效期（秒，到期自动恢复原级别）")
    private Long ttlSeconds;
}
