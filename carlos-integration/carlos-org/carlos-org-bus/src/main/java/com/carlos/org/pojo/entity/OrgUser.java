package com.carlos.org.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.carlos.org.pojo.enums.OrgUserGenderEnum;
import com.carlos.org.pojo.enums.OrgUserStateEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统用户 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@TableName("org_user")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgUser extends Model<OrgUser> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 用户名
     */
    @TableField(value = "account")
    private String account;
    /**
     * 昵称
     */
    @TableField(value = "nickname")
    private String nickname;
    /**
     * 密码
     */
    @TableField(value = "pwd")
    private String pwd;
    /**
     * 证件号码
     */
    @TableField(value = "identify")
    private String identify;
    /**
     * 手机号码
     */
    @TableField(value = "phone")
    private String phone;
    /**
     * 详细地址
     */
    @TableField(value = "address")
    private String address;
    /**
     * 性别，0：保密, 1：男，2：女，默认0
     */
    @TableField(value = "gender")
    private OrgUserGenderEnum gender;
    /**
     * 邮箱
     */
    @TableField(value = "email")
    private String email;
    /**
     * 头像
     */
    @TableField(value = "avatar")
    private String avatar;
    /**
     * 备注
     */
    @TableField(value = "description")
    private String description;
    /**
     * 状态，0：禁用，1：启用，2：锁定
     */
    @TableField(value = "user_status")
    private OrgUserStateEnum userStatus;
    /**
     * 主部门id
     */
    @TableField(value = "main_dept_id")
    private Long mainDeptId;
    /**
     * 最后登录时间
     */
    @TableField(value = "login_time")
    private LocalDateTime loginTime;
    /**
     * 最后登录ip
     */
    @TableField(value = "login_ip")
    private String loginIp;
    /**
     * 登录次数
     */
    @TableField(value = "login_count")
    private Integer loginCount;
    /**
     * 版本
     */
    @Version
    @TableField(value = "version")
    private Integer version;
    /**
     * 密码最后修改时间
     */
    @TableField(value = "pwd_last_modify")
    private LocalDateTime pwdLastModify;
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
