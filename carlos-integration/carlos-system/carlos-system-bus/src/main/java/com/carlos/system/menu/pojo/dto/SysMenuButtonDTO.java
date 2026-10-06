package com.carlos.system.menu.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 菜单按钮权限 DTO
 * </p>
 *
 * <p>对应前端菜单编辑中的 buttons 项，与 sys_operation 表关联</p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SysMenuButtonDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限码（对应 sys_operation.perm_code）
     */
    private String code;

    /**
     * 按钮描述（对应 sys_operation.op_name）
     */
    private String desc;

}
