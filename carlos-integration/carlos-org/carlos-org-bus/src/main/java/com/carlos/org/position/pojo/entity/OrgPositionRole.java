package com.carlos.org.position.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 岗位角色关联表（岗位默认权限配置） 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("org_position_role")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgPositionRole extends Model<OrgPositionRole> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 岗位ID
     */
    @TableField(value = "position_id")
    private Long positionId;
    /**
     * 角色ID
     */
    @TableField(value = "role_id")
    private Long roleId;
    /**
     * 是否默认角色：1是（入职自动获得），0否（可选）
     */
    @TableField(value = "is_default")
    private Boolean defaultRole;
    /**
     * 状态：0停用，1启用
     */
    @TableField(value = "position_role_status")
    private Integer positionRoleStatus;
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
     * 租户ID
     */
    @TableField(value = "tenant_id")
    private Long tenantId;

}
