package com.carlos.core.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * oauth2授权模式
 * </p>
 *
 * @author carlos
 * @date 2020/4/11 23:26
 */
@Getter
@AllArgsConstructor
public enum GrantTypeEnum {

    /**
     *
     */
    AUTHORIZATION_CODE("authorization_code", "授权码模式"),
    IMPLICIT("implicit", "简化模式"),
    REFRESH_TOKEN("refresh_token", "刷新令牌模式"),
    PASSWORD("password", "密码模式"),
    ;

    private final String value;

    private final String desc;
}
