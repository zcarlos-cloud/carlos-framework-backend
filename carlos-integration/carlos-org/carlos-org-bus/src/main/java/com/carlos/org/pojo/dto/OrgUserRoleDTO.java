package com.carlos.org.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户角色DTO
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
@Data
@Accessors(chain = true)
public class OrgUserRoleDTO {
    /** 主键 */
    private Long id;
    /** 用户id */
    private Long userId;
    /** 角色id */
    private Long roleId;
    /** 角色名称（查询时补全，关联表本身没有这两个字段） */
    private String roleName;
    /** 角色编码（查询时补全） */
    private String roleCode;
    /** 角色生效的部门id */
    private Long deptId;
    /** 失效时间 */
    private LocalDateTime expireTime;
    /** 创建者 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 租户id */
    private Long tenantId;
}
