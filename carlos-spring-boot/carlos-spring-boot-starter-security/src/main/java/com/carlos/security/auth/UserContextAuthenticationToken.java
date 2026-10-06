package com.carlos.security.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * <p>
 * 用户上下文认证令牌
 * </p>
 *
 * <p>封装从网关传递的用户信息，作为 Spring Security 的 Authentication 实现。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
public class UserContextAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    /**
     * 用户上下文（principal）
     */
    private final UserContext userContext;

    /**
     * 创建未认证的令牌（用于过滤器中构建）
     */
    public UserContextAuthenticationToken(UserContext userContext) {
        super(userContext != null ? userContext.getAuthorities() : null);
        this.userContext = userContext;
        // 初始状态为未认证
        setAuthenticated(false);
    }

    /**
     * 创建已认证的令牌
     */
    public UserContextAuthenticationToken(UserContext userContext,
                                          Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userContext = userContext;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        // 无凭证（网关已完成认证）
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userContext;
    }

    /**
     * 获取用户上下文
     */
    public UserContext getUserContext() {
        return userContext;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException(
                "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }
        super.setAuthenticated(false);
    }
}
