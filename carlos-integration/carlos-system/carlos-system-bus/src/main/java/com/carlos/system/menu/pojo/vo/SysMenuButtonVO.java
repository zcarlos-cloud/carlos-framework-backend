package com.carlos.system.menu.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 菜单按钮权限 VO
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SysMenuButtonVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "权限码")
    private String code;

    @Schema(description = "按钮描述")
    private String desc;

}
