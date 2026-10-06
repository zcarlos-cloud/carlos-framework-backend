package com.carlos.org.position.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 岗位角色关联表（岗位默认权限配置） 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
public class OrgPositionRoleDTO {
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
