package com.carlos.devkit.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 模板文件重命名（移动）参数
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "模板文件重命名参数")
public class TemplateFileRenameParam {

    @NotNull(message = "模板文件ID不能为空")
    @Schema(description = "模板文件ID")
    private Long id;

    @NotBlank(message = "新文件路径不能为空")
    @Size(max = 512, message = "新文件路径长度不能超过512")
    @Schema(description = "新的组内相对路径")
    private String newPath;
}
