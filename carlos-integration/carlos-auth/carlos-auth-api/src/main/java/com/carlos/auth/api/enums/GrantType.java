package com.carlos.auth.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录方式<p>
 *
 * @author hemx
 * @since 2023/9/20
 */
@Getter
@AllArgsConstructor
public enum GrantType {

    /**
     * 密码模式
     */
    PASSWORD("password", "密码模式"),

    /**
     * 验证码模式
     */
    APP("APP", "验证码模式"),
    ;


    private final String code;

    private final String desc;


    public String getCode() {
        return code;
    }


    public String getDesc() {
        return desc;
    }
}
