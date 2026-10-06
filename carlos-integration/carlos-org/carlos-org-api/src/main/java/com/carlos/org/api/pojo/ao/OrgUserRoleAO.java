package com.carlos.org.api.pojo.ao;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户角色 API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
public class OrgUserRoleAO implements Serializable {
    /** 主键 */
    private Long id;
    /** 用户id */
    private Long userId;
    /** 角色id */
    private Long roleId;
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
