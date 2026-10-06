package com.carlos.system.menu.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 菜单按钮权限参数
 * </p>
 *
 * <p>对应前端菜单编辑中的 buttons 项</p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Data
@Accessors(chain = true)
@Schema(description = "菜单按钮权限参数")
public class SysMenuButtonParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "按钮权限码不能为空")
    @Size(max = 100, message = "按钮权限码长度不能超过100")
    @Schema(description = "权限码（对应 sys_operation.perm_code）")
    private String code;

    @Size(max = 100, message = "按钮描述长度不能超过100")
    @Schema(description = "按钮描述（对应 sys_operation.op_name）")
    private String desc;

}
