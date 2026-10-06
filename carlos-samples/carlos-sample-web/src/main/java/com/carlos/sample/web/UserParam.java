package com.carlos.sample.web;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 用户参数校验示例类
 *
 * @author carlos
 */
@Data
public class UserParam {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20个字符之间")
    private String username;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 年龄
     */
    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄最小值为1")
    @Max(value = 150, message = "年龄最大值为150")
    private Integer age;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
