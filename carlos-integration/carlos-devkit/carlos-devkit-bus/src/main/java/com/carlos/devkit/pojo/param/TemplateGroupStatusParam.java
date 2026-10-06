package com.carlos.devkit.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 模板组状态切换参数
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "模板组状态切换参数")
public class TemplateGroupStatusParam {

    @NotNull(message = "模板组ID不能为空")
    @Schema(description = "模板组ID")
    private Long id;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    @Schema(description = "状态：1=启用 0=停用")
    private Integer status;
}
