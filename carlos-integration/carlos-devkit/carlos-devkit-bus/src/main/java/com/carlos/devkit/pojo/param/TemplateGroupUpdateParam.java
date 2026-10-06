package com.carlos.devkit.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 模板组编辑参数
 * </p>
 * <p>
 * 编码 {@code groupCode} 创建后不可改（模板物化与生成记录都依赖它做追溯），故不在此参数中。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "模板组编辑参数")
public class TemplateGroupUpdateParam {

    @NotNull(message = "模板组ID不能为空")
    @Schema(description = "模板组ID")
    private Long id;

    @NotNull(message = "模板组名称不能为空")
    @Size(max = 128, message = "模板组名称长度不能超过128")
    @Schema(description = "模板组名称")
    private String groupName;

    @Size(max = 512, message = "描述长度不能超过512")
    @Schema(description = "描述")
    private String description;

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

    @Schema(description = "状态：1=启用 0=停用")
    private Integer status;

    @Schema(description = "排序值")
    private Integer sortNum;

    @Size(max = 512, message = "备注长度不能超过512")
    @Schema(description = "备注")
    private String remark;
}
