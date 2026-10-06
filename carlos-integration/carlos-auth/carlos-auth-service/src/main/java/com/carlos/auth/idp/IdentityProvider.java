package com.carlos.auth.idp;

import java.util.Map;
import java.util.Set;

/**
 * 统一身份源提供者接口
 *
 * <p>所有身份认证方式（本地密码、短信、钉钉、企微、LDAP、SAML 等）均需实现此接口。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-14
 */
public interface IdentityProvider {

    /**
     * 身份源唯一标识
     *
     * @return 如 local、dingtalk、wecom、ldap、saml 等
     */
    String getProviderId();

    /**
     * 身份源类型
     *
     * @return 如 oauth2、saml、ldap、local
     */
    String getProviderType();

    /**
     * 支持的授权类型（Grant Type）
     *
     * @return 授权类型集合，如 password、sms_code、dingtalk
     */
    Set<String> getSupportedGrantTypes();

    /**
     * 后端直接校验类登录
     *
     * <p>适用于本地密码、短信验证码、LDAP 等不需要浏览器重定向的认证方式。</p>
     *
     * @param request 认证请求上下文
     * @return 认证成功后的用户身份信息
     */
    UserIdentity authenticate(IdentityProviderRequest request);

    /**
     * 获取授权重定向 URL
     *
     * <p>仅 OAuth2/SAML 类身份源需要实现。</p>
     *
     * @param state        防 CSRF 状态值
     * @param redirectUri  回调地址
     * @return 重定向 URL
     */
    default String buildAuthorizeUrl(String state, String redirectUri) {
        throw new UnsupportedOperationException("该身份源不支持授权重定向");
    }

    /**
     * 处理回调/授权码
     *
     * <p>仅 OAuth2/SAML 类身份源需要实现。</p>
     *
     * @param callbackParams 回调参数
     * @return 认证成功后的用户身份信息
     */
    default UserIdentity handleCallback(Map<String, String> callbackParams) {
        throw new UnsupportedOperationException("该身份源不支持回调处理");
    }
}
