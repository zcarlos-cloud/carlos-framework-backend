package com.carlos.system.menu.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 菜单树形数据
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysMenuTreeVO {

    @Schema(description = "菜单id")
    private Long id;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "路由name")
    private String routeName;

    @Schema(description = "子菜单")
    private List<SysMenuTreeVO> children;

}
