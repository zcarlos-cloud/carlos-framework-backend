package com.carlos.org.pojo.dto;


import com.carlos.org.pojo.enums.OrgUserGenderEnum;
import com.carlos.org.pojo.enums.OrgUserStateEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 系统用户 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
public class OrgUserDTO {
    /** 主键 */
    private Long id;
    /** 用户名 */
    private String account;
    /** 昵称 */
    private String nickname;
    /** 密码 */
    private String pwd;
    /** 证件号码 */
    private String identify;
    /** 手机号码 */
    private String phone;
    /** 详细地址 */
    private String address;
    /** 性别，0：保密, 1：男，2：女，默认0 */
    private OrgUserGenderEnum gender;
    /** 邮箱 */
    private String email;
    /** 头像 */
    private String avatar;
    /** 备注 */
    private String description;
    /** 状态，0：禁用，1：启用，2：锁定 */
    private OrgUserStateEnum userStatus;
    /** 主部门id */
    private Long mainDeptId;
    /** 最后登录时间 */
    private LocalDateTime loginTime;
    /** 最后登录ip */
    private String loginIp;
    /** 登录次数 */
    private Integer loginCount;
    /** 版本 */
    private Integer version;
    /** 密码最后修改时间 */
    private LocalDateTime pwdLastModify;
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
}
