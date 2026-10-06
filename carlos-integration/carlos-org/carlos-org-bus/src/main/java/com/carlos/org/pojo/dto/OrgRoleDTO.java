package com.carlos.org.pojo.dto;


import com.carlos.org.pojo.enums.OrgDataScopeEnum;
import com.carlos.org.pojo.enums.OrgRoleStateEnum;
import com.carlos.org.pojo.enums.OrgRoleTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 系统角色 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
public class OrgRoleDTO {
    /** 主键 */
    private Long id;
    /** 角色名称 */
    private String roleName;
    /** 角色唯一编码 */
    private String roleCode;
    /** 角色类型， 1：系统角色, 2: 自定义角色 */
    private OrgRoleTypeEnum roleType;
    /** 数据范围， 1：全部, 2: 本部及子部门, 3:仅本部门, 4:仅本人, 5:自定义规则 */
    private OrgDataScopeEnum dataScope;
    /** 角色状态， 0：禁用, 1: 启用 */
    private OrgRoleStateEnum roleStatus;
    /** 备注 */
    private String description;
    /** 版本 */
    private Integer version;
    /** 创建者 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 修改者 */
    private Long updateBy;
    /** 修改时间 */
    private LocalDateTime updateTime;
    /** 租户id */
    private Long tenantId;
    /** 操作ID列表（用于详情查询，来源 org_role_operation） */
    private java.util.List<Long> operationIds;
    /** 用户数量（用于列表展示） */
    private Long userCount;
}
