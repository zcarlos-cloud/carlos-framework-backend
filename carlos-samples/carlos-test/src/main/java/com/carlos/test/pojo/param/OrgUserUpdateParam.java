package com.carlos.test.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 系统用户 更新参数封装
 * </p>
 *
 * @author Carlos
 * @date 2023-8-12 11:16:18
 */
@Data
@Accessors(chain = true)
@Schema(description = "系统用户修改参数")
public class OrgUserUpdateParam {

    @NotBlank(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "用户")
    private String account;
    @Schema(description = "真实姓名")
    private String realname;
    @Schema(description = "密码")
    private String pwd;
    @Schema(description = "证件号码")
    private String identify;
    @Schema(description = "手机号码")
    private String phone;
    @Schema(description = "详细地址")
    private String address;
    @Schema(description = "性别：保 1：男：女，默")
    private Integer gender;
    @Schema(description = "头像文件的id")
    private String head;
    @Schema(description = "排序")
    private Integer sort;
    @Schema(description = "备注")
    private String description;
    @Schema(description = "钉钉")
    private String dingding;
    @Schema(description = "政治面貌")
    private String politicalOutlook;
    @Schema(description = "学历")
    private String educationBackground;
    @Schema(description = "最后登录时")
    private LocalDateTime lastLogin;
    @Schema(description = "登录次数")
    private Integer loginCount;
}
