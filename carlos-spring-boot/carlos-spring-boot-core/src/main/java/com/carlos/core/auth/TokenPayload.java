package com.carlos.core.auth;

import cn.hutool.core.annotation.Alias;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * <p>
 * token payload信息
 * </p>
 *
 * @author carlos
 * @date 2021/12/27 11:25
 */
@Data
@NoArgsConstructor
public class TokenPayload {

    /**
     * 用户id
     */
    @Alias("user_id")
    private Serializable userId;
    /**
     * 用户名
     */
    @Alias("user_name")
    private String userName;
    /**
     * 域
     */
    private Set<String> scope;
    /**
     * 过期时间
     */
    @Alias("exp")
    private Long expire;
    /**
     * jti
     */
    private String jti;
    /**
     * 客户端id
     */
    @Alias("client_id")
    private String clientId;
    /**
     * 权限信息
     */
    private Set<String> authorities;

}
