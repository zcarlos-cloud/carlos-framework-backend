package com.carlos.devkit.pojo.param;

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
 * 模板文件新增参数
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "模板文件新增参数")
public class TemplateFileCreateParam {

    @NotNull(message = "模板组ID不能为空")
    @Schema(description = "模板组ID")
    private Long groupId;

    @NotBlank(message = "文件路径不能为空")
    @Size(max = 512, message = "文件路径长度不能超过512")
    @Schema(description = "组内相对路径，如 src/main/java/pojo/entity/[base][for].java.ftl")
    private String filePath;

    @Schema(description = "文件正文")
    private String content;

    @NotNull(message = "文件类型不能为空")
    @Min(value = 1, message = "文件类型只能是 1 或 2")
    @Max(value = 2, message = "文件类型只能是 1 或 2")
    @Schema(description = "文件类型：1=模板(.ftl) 2=静态资源")
    private Integer fileType;

    @Size(max = 512, message = "备注长度不能超过512")
    @Schema(description = "备注")
    private String remark;
}
