package com.carlos.core.auth;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * Oauth2获取Token返回信息封装
 * </p>
 *
 * @author carlos
 * @date 2021/11/5 10:24
 */
@Data
@Accessors(chain = true)
public class Oauth2TokenDTO implements Serializable {

    /**
     * 访问令牌
     */
    private String token;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * token类型
     */
    private String tokenType;

    /**
     * 有效时间（秒）
     */
    private long expiresIn;
}
