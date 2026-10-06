package com.carlos.org.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.carlos.org.pojo.enums.OrgDataScopeEnum;
import com.carlos.org.pojo.enums.OrgRoleStateEnum;
import com.carlos.org.pojo.enums.OrgRoleTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统角色 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@TableName("org_role")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgRole extends Model<OrgRole> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 角色名称
     */
    @TableField(value = "role_name")
    private String roleName;
    /**
     * 角色唯一编码
     */
    @TableField(value = "role_code")
    private String roleCode;
    /**
     * 角色类型， 1：系统角色, 2: 自定义角色
     */
    @TableField(value = "role_type")
    private OrgRoleTypeEnum roleType;
    /**
     * 数据范围， 1：全部, 2: 本部及子部门, 3:仅本部门, 4:仅本人, 5:自定义规则
     */
    @TableField(value = "data_scope")
    private OrgDataScopeEnum dataScope;
    /**
     * 角色状态， 0：禁用, 1: 启用
     */
    @TableField(value = "role_status")
    private OrgRoleStateEnum roleStatus;
    /**
     * 备注
     */
    @TableField(value = "description")
    private String description;
    /**
     * 版本
     */
    @Version
    @TableField(value = "version")
    private Integer version;
    /**
     * 逻辑删除，0：未删除，1：已删除
     */
    @TableLogic
    @TableField(value = "is_deleted")
    private Boolean deleted;
    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 修改者
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;
    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    /**
     * 租户id
     */
    @TableField(value = "tenant_id")
    private Long tenantId;

}
