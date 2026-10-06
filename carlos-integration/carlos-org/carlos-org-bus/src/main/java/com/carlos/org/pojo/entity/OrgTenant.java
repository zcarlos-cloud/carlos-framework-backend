package com.carlos.org.pojo.entity;


import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.carlos.org.pojo.enums.OrgTenantStateEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 租户 数据源对象
 * </p>
 * <p>0 号租户保留为平台租户；本表不做租户隔离（无 tenant_id 列）</p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@TableName("org_tenant")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgTenant extends Model<OrgTenant> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键（雪花 ID；0 保留为平台租户）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 租户编码（登录时输入）
     */
    @TableField(value = "tenant_code")
    private String tenantCode;
    /**
     * 租户名称
     */
    @TableField(value = "tenant_name")
    private String tenantName;
    /**
     * 联系人
     */
    @TableField(value = "contact_name")
    private String contactName;
    /**
     * 联系电话
     */
    @TableField(value = "contact_phone")
    private String contactPhone;
    /**
     * 租户状态， 1：正常, 2: 停用
     */
    @TableField(value = "tenant_status")
    private OrgTenantStateEnum tenantStatus;
    /**
     * 过期时间（NULL 表示永不过期）
     */
    @TableField(value = "expire_time")
    private LocalDateTime expireTime;
    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;
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

}
