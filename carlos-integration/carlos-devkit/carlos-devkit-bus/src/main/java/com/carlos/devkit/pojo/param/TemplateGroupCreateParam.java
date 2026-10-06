package com.carlos.devkit.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 模板组新增参数
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "模板组新增参数")
public class TemplateGroupCreateParam {

    @NotBlank(message = "模板组编码不能为空")
    @Size(max = 64, message = "模板组编码长度不能超过64")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "模板组编码只能包含字母、数字、下划线和中划线")
    @Schema(description = "模板组编码，如 cloud-mybatis")
    private String groupCode;

    @NotBlank(message = "模板组名称不能为空")
    @Size(max = 128, message = "模板组名称长度不能超过128")
    @Schema(description = "模板组名称")
    private String groupName;

    @Size(max = 512, message = "描述长度不能超过512")
    @Schema(description = "描述")
    private String description;

    @NotNull(message = "模板类型不能为空")
    @Min(value = 1, message = "模板类型只能是 1 或 2")
    @Max(value = 2, message = "模板类型只能是 1 或 2")
    @Schema(description = "模板类型：1=代码生成 2=工程生成")
    private Integer templateType;

    @Size(max = 32, message = "技术栈编码长度不能超过32")
    @Schema(description = "技术栈编码")
    private String techStack;

    @Size(max = 16, message = "数据库类型长度不能超过16")
    @Schema(description = "适用数据库：MYSQL/POSTGRESQL/ANY")
    private String dbType;

    @Size(max = 64, message = "模板根目录名长度不能超过64")
    @Schema(description = "模板根目录名，默认 template")
    private String templateRoot;

    @Size(max = 512, message = "逻辑删除字段清单长度不能超过512")
    @Schema(description = "逻辑删除字段名清单，逗号分隔")
    private String logicDeleteFields;

    @Size(max = 512, message = "版本字段清单长度不能超过512")
    @Schema(description = "乐观锁版本字段名清单，逗号分隔")
    private String versionFields;

    @Size(max = 1024, message = "通用字段清单长度不能超过1024")
    @Schema(description = "通用字段名清单，逗号分隔")
    private String commonFields;

    @Schema(description = "排序值，默认 0")
    private Integer sortNum = 0;

    @Size(max = 512, message = "备注长度不能超过512")
    @Schema(description = "备注")
    private String remark;
}
