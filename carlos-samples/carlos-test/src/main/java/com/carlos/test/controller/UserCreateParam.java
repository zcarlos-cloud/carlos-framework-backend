package com.carlos.test.controller;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;


/**
 * <p>
 * 系统用户 新增参数封装
 * </p>
 *
 * @author carlos
 * @date 2022-11-11 18:19:17
 */
@Data
@Accessors(chain = true)
public class UserCreateParam {

    @Schema(description = "用户")
    private String account;
    @Schema(description = "真实姓名")
    private String realname;
    // @NotBlank(message = "密码不能为空")
    @Schema(description = "密码")
    private String pwd;
    @Schema(description = "证件号码")
    private String identify;
    @Schema(description = "手机号码")
    private String phone;
    @Schema(description = "管理")
    private Boolean admin;
    @Schema(description = "行政区域")
    private String regionCode;
    @Schema(description = "角色id列表")
    private Set<String> roleIds;
    @Schema(description = "部门id列表")
    private Set<String> departmentIds;
}
