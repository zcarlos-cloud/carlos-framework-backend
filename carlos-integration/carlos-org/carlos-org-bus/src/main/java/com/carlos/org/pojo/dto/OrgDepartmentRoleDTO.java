package com.carlos.org.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 部门角色 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
public class OrgDepartmentRoleDTO {
    /** 主键 */
    private Long id;
    /** 部门id */
    private Long deptId;
    /** 角色id */
    private Long roleId;
    /** 版本 */
    private Integer version;
    /** 是否为默认角色 */
    private Boolean defaultRole;
    /** 创建者 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 修改时间 */
    private LocalDateTime updateTime;
    /** 租户id */
    private Long tenantId;
}
