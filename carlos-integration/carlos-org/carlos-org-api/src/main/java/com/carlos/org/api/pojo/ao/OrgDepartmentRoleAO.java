package com.carlos.org.api.pojo.ao;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 部门角色 API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
public class OrgDepartmentRoleAO implements Serializable {
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
