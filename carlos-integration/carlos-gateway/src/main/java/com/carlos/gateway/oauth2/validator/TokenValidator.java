package com.carlos.gateway.oauth2.validator;

import com.carlos.core.auth.UserContext;
import com.carlos.gateway.oauth2.OAuth2GatewayProperties;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Token 验证器接口
 * 支持 JWT 和 Opaque Token 双模式验证
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 */
public interface TokenValidator {

    /**
     * 验证 Token 并返回用户上下文
     *
     * @param token Token 字符串
     * @return 用户上下文（验证失败返回空 Mono）
     */
    Mono<UserContext> validate(String token);

    /**
     * 获取验证器类型
     *
     * @return Token 类型
     */
    OAuth2GatewayProperties.TokenType getType();
}
