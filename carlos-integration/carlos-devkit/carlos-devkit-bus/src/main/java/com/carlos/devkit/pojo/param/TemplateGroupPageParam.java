package com.carlos.devkit.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 模板组分页查询参数
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "模板组分页查询参数")
public class TemplateGroupPageParam extends ParamPage {

    @Size(max = 128, message = "模板组名称长度不能超过128")
    @Schema(description = "模板组名称（模糊）")
    private String groupName;

    @Schema(description = "模板组编码（模糊）")
    private String groupCode;

    @Schema(description = "模板类型：1=代码生成 2=工程生成")
    private Integer templateType;

    @Schema(description = "技术栈编码")
    private String techStack;

    @Schema(description = "状态：1=启用 0=停用")
    private Integer status;
}
