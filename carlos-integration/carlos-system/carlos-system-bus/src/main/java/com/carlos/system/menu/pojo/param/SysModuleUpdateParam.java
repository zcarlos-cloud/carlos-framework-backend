package com.carlos.system.menu.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 功能模块 更新参数
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Data
@Accessors(chain = true)
@Schema(description = "功能模块更新参数")
public class SysModuleUpdateParam {

    @Schema(description = "主键ID")
    @NotNull(message = "ID不能为空")
    private Long id;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "模块名称")
    @NotBlank(message = "模块名称不能为空")
    @Size(max = 100, message = "模块名称长度不能超过100")
    private String moduleName;

    @Schema(description = "模块编码")
    @NotBlank(message = "模块编码不能为空")
    @Size(max = 100, message = "模块编码长度不能超过100")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "模块编码须以字母开头，仅允许字母、数字、下划线、点、中划线，长度2~64位")
    private String moduleCode;

    @Schema(description = "排序")
    private Integer orderNum;

    @Schema(description = "状态：true启用 false禁用")
    private Boolean moduleStatus;

}
