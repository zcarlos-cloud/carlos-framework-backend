package com.carlos.monitor.instance.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * Nacos 配置自动发现 参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
@Schema(description = "Nacos 配置自动发现参数")
public class InstanceDiscoverParam {

    @NotBlank(message = "应用名不能为空")
    @Size(max = 100, message = "应用名长度不能超过100")
    @Schema(description = "目标应用名（微服务 spring.application.name）")
    private String appName;

    @Size(max = 100, message = "配置分组长度不能超过100")
    @Schema(description = "Nacos 配置分组，默认 DEFAULT_GROUP")
    private String group = "DEFAULT_GROUP";
}
