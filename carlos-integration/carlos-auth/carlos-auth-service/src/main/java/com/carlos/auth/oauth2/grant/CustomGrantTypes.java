package com.carlos.auth.oauth2.grant;

import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * <p>
 * 自定义 OAuth2 授权类型
 * </p>
 *
 * <p>本类定义项目内部使用的自定义授权类型常量，用于扩展认证方式。</p>
 *
 * <h3>说明：</h3>
 * <ul>
 *   <li><strong>注意：</strong>密码授权类型（password）已被移除，账号密码登录应使用表单登录（/login）端点</li>
 *   <li>这些授权类型用于内部服务间调用和特定业务场景</li>
 *   <li>对外暴露的标准 OAuth2 端点仍应使用 AUTHORIZATION_CODE、CLIENT_CREDENTIALS 等标准类型</li>
 * </ul>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
public final class CustomGrantTypes {

    private CustomGrantTypes() {
        // 工具类，禁止实例化
    }

    /**
     * 密码授权类型（内部使用，供统一身份源架构兼容）
     */
    public static final AuthorizationGrantType PASSWORD = new AuthorizationGrantType("password");

    /**
     * 短信验证码授权类型
     */
    public static final AuthorizationGrantType SMS_CODE = new AuthorizationGrantType("sms_code");

    /**
     * 邮箱验证码授权类型
     */
    public static final AuthorizationGrantType EMAIL_CODE = new AuthorizationGrantType("email_code");

    /**
     * 扫码登录授权类型
     */
    public static final AuthorizationGrantType QR_CODE = new AuthorizationGrantType("qr_code");

    /**
     * 第三方登录授权类型（通用）
     */
    public static final AuthorizationGrantType SOCIAL = new AuthorizationGrantType("social");

    /**
     * 钉钉登录授权类型
     */
    public static final AuthorizationGrantType DINGTALK = new AuthorizationGrantType("dingtalk");

    /**
     * 微信登录授权类型
     */
    public static final AuthorizationGrantType WECHAT = new AuthorizationGrantType("wechat");

    /**
     * Passkey/WebAuthn 无密码登录授权类型
     */
    public static final AuthorizationGrantType PASSKEY = new AuthorizationGrantType("passkey");

    /**
     * 判断是否为自定义扩展授权类型
     *
     * @param grantType 授权类型
     * @return true-是自定义扩展类型
     */
    public static boolean isCustomGrantType(AuthorizationGrantType grantType) {
        if (grantType == null) {
            return false;
        }
        String value = grantType.getValue();
        return PASSWORD.getValue().equals(value)
            || SMS_CODE.getValue().equals(value)
            || EMAIL_CODE.getValue().equals(value)
            || QR_CODE.getValue().equals(value)
            || SOCIAL.getValue().equals(value)
            || DINGTALK.getValue().equals(value)
            || WECHAT.getValue().equals(value)
            || PASSKEY.getValue().equals(value);
    }

    /**
     * 判断是否为自定义扩展授权类型（字符串值）
     *
     * @param grantTypeValue 授权类型字符串值
     * @return true-是自定义扩展类型
     */
    public static boolean isCustomGrantType(String grantTypeValue) {
        if (grantTypeValue == null) {
            return false;
        }
        return PASSWORD.getValue().equals(grantTypeValue)
            || SMS_CODE.getValue().equals(grantTypeValue)
            || EMAIL_CODE.getValue().equals(grantTypeValue)
            || QR_CODE.getValue().equals(grantTypeValue)
            || SOCIAL.getValue().equals(grantTypeValue)
            || DINGTALK.getValue().equals(grantTypeValue)
            || WECHAT.getValue().equals(grantTypeValue)
            || PASSKEY.getValue().equals(grantTypeValue);
    }
}
