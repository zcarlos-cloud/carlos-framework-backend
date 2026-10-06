package com.carlos.auth.security.ext.handler;

import com.carlos.auth.security.ext.ExtendAuthenticationConverter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

/**
 * 授权类型参数处理器
 *
 * <p>每种自定义授权类型（password、sms_code、email_code 等）实现此接口，
 * 负责该授权类型的参数验证、主体提取和凭证提取。</p>
 *
 * @author Carlos
 * @since 3.0.0
 * @see ExtendAuthenticationConverter
 */
public interface GrantTypeHandler {

    /**
     * 获取支持的授权类型值
     *
     * @return 授权类型字符串，如 "password"、"sms_code"
     */
    String getGrantType();

    /**
     * 验证请求参数
     *
     * @param params 请求参数
     * @throws OAuth2AuthenticationException 验证失败时抛出
     */
    void validate(Map<String, String> params) throws OAuth2AuthenticationException;

    /**
     * 提取认证主体
     *
     * @param params 请求参数
     * @return 认证主体（用户名/手机号/邮箱等）
     */
    Object extractPrincipal(Map<String, String> params);

    /**
     * 提取凭证
     *
     * @param params 请求参数
     * @return 凭证（密码/验证码等），不需要凭证时返回 null
     */
    Object extractCredentials(Map<String, String> params);
}
