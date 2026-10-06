package com.carlos.devkit.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 模板组复制参数
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "模板组复制参数")
public class TemplateGroupCopyParam {

    @NotNull(message = "源模板组ID不能为空")
    @Schema(description = "源模板组ID")
    private Long id;

    @NotBlank(message = "新模板组编码不能为空")
    @Size(max = 64, message = "新模板组编码长度不能超过64")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "模板组编码只能包含字母、数字、下划线和中划线")
    @Schema(description = "新模板组编码")
    private String newGroupCode;

    @NotBlank(message = "新模板组名称不能为空")
    @Size(max = 128, message = "新模板组名称长度不能超过128")
    @Schema(description = "新模板组名称")
    private String newGroupName;

    @Schema(description = "是否复制模板文件内容，默认 true")
    private Boolean copyFiles = Boolean.TRUE;
}
