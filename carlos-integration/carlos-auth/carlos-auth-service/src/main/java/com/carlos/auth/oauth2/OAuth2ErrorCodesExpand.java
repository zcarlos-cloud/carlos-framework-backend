package com.carlos.auth.oauth2;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OAuth2 错误码扩展枚举
 *
 * <p>定义了认证和授权过程中可能遇到的扩展错误码，用于提供更详细的错误信息。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 在异常处理中使用
 * throw new OAuth2AuthenticationException(
 *     new OAuth2Error(
 *         OAuth2ErrorCodesExpand.USERNAME_NOT_FOUND.getErrorCode(),
 *         OAuth2ErrorCodesExpand.USERNAME_NOT_FOUND.getErrorDescription(),
 *         ""
 *     )
 * );
 *
 * // 或使用简写方式
 * throw new OAuth2AuthenticationException(
 *     OAuth2ErrorCodesExpand.USERNAME_NOT_FOUND.toOAuth2Error()
 * );
 * }</pre>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see org.springframework.security.oauth2.core.OAuth2Error
 * @see org.springframework.security.oauth2.core.OAuth2ErrorCodes
 */
@Getter
@RequiredArgsConstructor
public enum OAuth2ErrorCodesExpand {

    /**
     * 用户名未找到
     */
    USERNAME_NOT_FOUND("username_not_found", "用户名不存在"),

    /**
     * 错误凭证（用户名或密码错误）
     */
    BAD_CREDENTIALS("bad_credentials", "用户名或密码错误"),

    /**
     * 用户被锁定
     */
    USER_LOCKED("user_locked", "用户已锁定"),

    /**
     * 用户被禁用
     */
    USER_DISABLE("user_disable", "用户已禁用"),

    /**
     * 用户账号过期
     */
    USER_EXPIRED("user_expired", "用户已过期"),

    /**
     * 用户凭证（密码）过期
     */
    CREDENTIALS_EXPIRED("credentials_expired", "用户密钥已过期"),

    /**
     * Scope 为空异常
     */
    SCOPE_IS_EMPTY("scope_is_empty", "授权范围不能为空"),

    /**
     * 令牌不存在或已失效
     */
    TOKEN_MISSING("token_missing", "令牌不存在"),

    /**
     * 未知的登录异常
     */
    UN_KNOW_LOGIN_ERROR("un_know_login_error", "未知登录错误"),

    /**
     * 验证码错误
     */
    VERIFICATION_CODE_ERROR("verification_code_error", "验证码错误"),

    /**
     * 用户未找到（适用于其他标识如手机号、邮箱）
     */
    USER_NOT_FOUND("user_not_found", "用户不存在");

    /**
     * 错误码（用于 OAuth2Error 的 errorCode 参数）
     */
    private final String errorCode;

    /**
     * 错误描述（用于 OAuth2Error 的 description 参数）
     */
    private final String errorDescription;

    /**
     * 转换为 OAuth2Error 对象
     *
     * @return OAuth2Error 实例
     */
    public org.springframework.security.oauth2.core.OAuth2Error toOAuth2Error() {
        return new org.springframework.security.oauth2.core.OAuth2Error(this.errorCode, this.errorDescription, "");
    }

    /**
     * 转换为 OAuth2Error 对象，指定自定义错误描述
     *
     * @param customDescription 自定义错误描述
     * @return OAuth2Error 实例
     */
    public org.springframework.security.oauth2.core.OAuth2Error toOAuth2Error(String customDescription) {
        return new org.springframework.security.oauth2.core.OAuth2Error(this.errorCode, customDescription, "");
    }

    /**
     * 转换为 OAuth2Error 对象，指定自定义错误描述和错误 URI
     *
     * @param customDescription 自定义错误描述
     * @param errorUri 错误 URI
     * @return OAuth2Error 实例
     */
    public org.springframework.security.oauth2.core.OAuth2Error toOAuth2Error(String customDescription, String errorUri) {
        return new org.springframework.security.oauth2.core.OAuth2Error(this.errorCode, customDescription, errorUri);
    }

    /**
     * 快速创建 OAuth2AuthenticationException
     *
     * @return OAuth2AuthenticationException 实例
     */
    public org.springframework.security.oauth2.core.OAuth2AuthenticationException exception() {
        return new org.springframework.security.oauth2.core.OAuth2AuthenticationException(toOAuth2Error());
    }

    /**
     * 快速创建 OAuth2AuthenticationException，指定自定义错误描述
     *
     * @param customDescription 自定义错误描述
     * @return OAuth2AuthenticationException 实例
     */
    public org.springframework.security.oauth2.core.OAuth2AuthenticationException exception(String customDescription) {
        return new org.springframework.security.oauth2.core.OAuth2AuthenticationException(toOAuth2Error(customDescription));
    }

    /**
     * 根据错误码查找枚举
     *
     * @param errorCode 错误码
     * @return 对应的枚举值，如果未找到返回 null
     */
    public static OAuth2ErrorCodesExpand fromErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isEmpty()) {
            return null;
        }
        for (OAuth2ErrorCodesExpand value : values()) {
            if (value.getErrorCode().equals(errorCode)) {
                return value;
            }
        }
        return null;
    }

}
