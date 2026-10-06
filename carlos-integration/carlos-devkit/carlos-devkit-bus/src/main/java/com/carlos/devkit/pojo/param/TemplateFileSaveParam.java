package com.carlos.devkit.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 模板文件保存参数（在线编辑保存）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "模板文件保存参数")
public class TemplateFileSaveParam {

    @NotNull(message = "模板文件ID不能为空")
    @Schema(description = "模板文件ID")
    private Long id;

    @Schema(description = "文件正文")
    private String content;

    @Size(max = 512, message = "变更说明长度不能超过512")
    @Schema(description = "变更说明（用于版本历史）")
    private String changeLog;
}
