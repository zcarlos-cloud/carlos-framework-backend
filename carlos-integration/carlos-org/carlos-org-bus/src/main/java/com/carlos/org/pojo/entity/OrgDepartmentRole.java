package com.carlos.org.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 部门角色 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@TableName("org_department_role")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgDepartmentRole extends Model<OrgDepartmentRole> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 部门id
     */
    @TableField(value = "dept_id")
    private Long deptId;
    /**
     * 角色id
     */
    @TableField(value = "role_id")
    private Long roleId;
    /**
     * 版本
     */
    @Version
    @TableField(value = "version")
    private Integer version;
    /**
     * 是否为默认角色
     */
    @TableField(value = "is_default")
    private Boolean defaultRole;
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
