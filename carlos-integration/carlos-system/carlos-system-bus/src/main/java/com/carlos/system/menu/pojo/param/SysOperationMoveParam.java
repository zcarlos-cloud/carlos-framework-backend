package com.carlos.system.menu.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 统一操作表 移动参数封装
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Data
@Accessors(chain = true)
@Schema(description = "统一操作移动参数")
public class SysOperationMoveParam {

    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "目标菜单ID，为空表示摘除菜单，成为独立接口（按钮类型不允许为空）")
    private Long menuId;

}
