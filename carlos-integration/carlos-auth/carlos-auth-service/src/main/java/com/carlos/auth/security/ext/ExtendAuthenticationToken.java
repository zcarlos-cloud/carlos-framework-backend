package com.carlos.auth.security.ext;

import com.carlos.auth.security.SecurityUser;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 扩展认证令牌
 *
 * <p>用于支持多种认证方式的统一令牌，可以携带额外的认证参数。</p>
 * <p><strong>注意：</strong>密码认证已通过表单登录（/login）端点实现，不再通过扩展授权流程处理。</p>
 *
 * <h3>支持的认证方式：</h3>
 * <ul>
 *   <li>短信验证码认证（SMS_CODE）</li>
 *   <li>邮箱验证码认证（EMAIL_CODE）</li>
 *   <li>扫码认证（QR_CODE）</li>
 *   <li>第三方登录（SOCIAL）</li>
 *   <li>自定义扩展认证方式</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 创建短信认证令牌
 * Map<String, String> params = new HashMap<>();
 * params.put("phone", "13800138000");
 * params.put("sms_code", "123456");
 *
 * ExtendAuthenticationToken token = new ExtendAuthenticationToken(
 *     "13800138000",           // principal
 *     "123456",                // credentials
 *     new AuthorizationGrantType("sms_code"),
 *     clientAuthentication,
 *     Set.of("read", "write"),
 *     params
 * );
 * }</pre>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see ExtendAuthenticationProvider
 * @see ExtendAuthenticationConverter
 */
@Getter
@Setter
public class ExtendAuthenticationToken extends AbstractAuthenticationToken {

    /**
     * 授权类型
     */
    private final AuthorizationGrantType grantType;

    /**
     * 客户端认证信息
     */
    private final OAuth2ClientAuthenticationToken client;

    /**
     * 授权范围
     */
    private final Set<String> scopes;

    /**
     * 认证主体（用户名/手机号/邮箱/第三方ID等）
     */
    private final Object principal;

    /**
     * 凭证（密码/验证码/Token等）
     */
    private final Object credentials;

    /**
     * 扩展参数
     */
    private final Map<String, String> additionalParameters;

    /**
     * 认证后的用户信息
     */
    private SecurityUser securityUser;

    /**
     * 认证方式标识
     */
    private String authMethod;

    /**
     * 创建未认证的令牌
     *
     * @param principal 认证主体
     * @param credentials 凭证
     * @param grantType 授权类型
     * @param client 客户端认证信息
     * @param scopes 授权范围
     * @param additionalParameters 扩展参数
     */
    public ExtendAuthenticationToken(Object principal,
                                     Object credentials,
                                     AuthorizationGrantType grantType,
                                     OAuth2ClientAuthenticationToken client,
                                     Set<String> scopes,
                                     Map<String, String> additionalParameters) {
        super(Collections.emptyList());
        this.principal = principal;
        this.credentials = credentials;
        this.grantType = grantType;
        this.client = client;
        this.scopes = scopes != null ? Set.copyOf(scopes) : Collections.emptySet();
        this.additionalParameters = additionalParameters != null
            ? Map.copyOf(additionalParameters)
            : Collections.emptyMap();
        this.authMethod = grantType != null ? grantType.getValue() : "unknown";
        setAuthenticated(false);
    }

    /**
     * 创建已认证的令牌
     *
     * @param principal 认证主体
     * @param credentials 凭证
     * @param grantType 授权类型
     * @param client 客户端认证信息
     * @param scopes 授权范围
     * @param additionalParameters 扩展参数
     * @param authorities 权限列表
     * @param securityUser 认证后的用户信息
     */
    public ExtendAuthenticationToken(Object principal,
                                     Object credentials,
                                     AuthorizationGrantType grantType,
                                     OAuth2ClientAuthenticationToken client,
                                     Set<String> scopes,
                                     Map<String, String> additionalParameters,
                                     Collection<? extends GrantedAuthority> authorities,
                                     SecurityUser securityUser) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        this.grantType = grantType;
        this.client = client;
        this.scopes = scopes != null ? Set.copyOf(scopes) : Collections.emptySet();
        this.additionalParameters = additionalParameters != null
            ? Map.copyOf(additionalParameters)
            : Collections.emptyMap();
        this.securityUser = securityUser;
        this.authMethod = grantType != null ? grantType.getValue() : "unknown";
        super.setAuthenticated(true);
    }

    /**
     * 创建简化版未认证令牌
     *
     * @param principal 认证主体
     * @param credentials 凭证
     * @param grantType 授权类型
     * @param client 客户端认证信息
     */
    public ExtendAuthenticationToken(Object principal,
                                     Object credentials,
                                     AuthorizationGrantType grantType,
                                     OAuth2ClientAuthenticationToken client) {
        this(principal, credentials, grantType, client, null, null);
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    /**
     * 获取指定名称的扩展参数
     *
     * @param name 参数名
     * @return 参数值，不存在返回 null
     */
    public String getParameter(String name) {
        return additionalParameters.get(name);
    }

    /**
     * 检查是否包含指定参数
     *
     * @param name 参数名
     * @return true-存在，false-不存在
     */
    public boolean hasParameter(String name) {
        return additionalParameters.containsKey(name);
    }

    /**
     * 获取客户端ID
     *
     * @return 客户端ID
     */
    public String getClientId() {
        return client != null ? client.getRegisteredClient().getClientId() : null;
    }

    /**
     * 判断是否为指定认证方式
     *
     * @param method 认证方式
     * @return true-匹配
     */
    public boolean isAuthMethod(String method) {
        return authMethod != null && authMethod.equals(method);
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException(
                "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }
        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        // 清除敏感信息
        if (credentials instanceof String) {
            // 无法直接清除 String，但可以通过其他方式处理
        }
    }

    @Override
    public String toString() {
        return "ExtendAuthenticationToken{" +
            "grantType=" + grantType +
            ", principal=" + principal +
            ", authMethod='" + authMethod + '\'' +
            ", scopes=" + scopes +
            ", authenticated=" + isAuthenticated() +
            '}';
    }

}
