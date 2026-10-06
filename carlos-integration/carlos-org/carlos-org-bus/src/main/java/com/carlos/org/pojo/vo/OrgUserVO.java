package com.carlos.org.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统用户 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgUserVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "用户名")
    private String account;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "证件号码")
    private String identify;
    @Schema(description = "手机号码")
    private String phone;
    @Schema(description = "详细地址")
    private String address;
    @Schema(description = "性别，0：保密, 1：男，2：女，默认0")
    private Integer gender;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "头像")
    private String avatar;
    @Schema(description = "备注")
    private String description;
    @Schema(description = "状态，0：禁用，1：启用，2：锁定")
    private Integer userStatus;
    @Schema(description = "主部门id")
    private Long mainDeptId;
    @Schema(description = "最后登录时间")
    private LocalDateTime loginTime;
    @Schema(description = "最后登录ip")
    private String loginIp;
    @Schema(description = "登录次数")
    private Integer loginCount;
    @Schema(description = "密码最后修改时间")
    private LocalDateTime pwdLastModify;
    @Schema(description = "创建者")
    private Long createBy;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "修改者")
    private Long updateBy;
    @Schema(description = "修改时间")
    private LocalDateTime updateTime;
    @Schema(description = "租户id")
    private Long tenantId;

}
