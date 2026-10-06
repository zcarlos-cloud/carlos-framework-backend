package com.carlos.system.menu.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 操作权限树 VO
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Data
@Accessors(chain = true)
public class SysOperationTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单ID")
    private Long menuId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "该菜单下的操作列表")
    private List<SysOperationVO> operations;

}
