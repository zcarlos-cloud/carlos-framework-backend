package com.carlos.org.position.api.pojo.param;


import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 岗位角色关联表（岗位默认权限配置） API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
public class ApiOrgPositionRoleParam implements Serializable {
    /** 主键 */
    private Long id;
    /** 岗位ID */
    private Long positionId;
    /** 角色ID */
    private Long roleId;
    /** 是否默认角色：1是（入职自动获得），0否（可选） */
    private Boolean defaultRole;
    /** 状态：0停用，1启用 */
    private Integer positionRoleStatus;
    /** 创建者 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 修改者 */
    private Long updateBy;
    /** 修改时间 */
    private LocalDateTime updateTime;
    /** 租户ID */
    private Long tenantId;
}
