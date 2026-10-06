package com.carlos.gateway.exception;

import com.carlos.core.response.CommonErrorCode;
import lombok.Getter;

/**
 * <p>
 * 认证异常
 * 用于处理登录认证失败、Token 无效等场景
 * 返回 HTTP 401 Unauthorized
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
@Getter
public class AuthenticationException extends GatewayException {

    private static final long serialVersionUID = 1L;

    /**
     * 认证失败类型
     */
    private final AuthFailureType failureType;

    public AuthenticationException(String message) {
        super(message, 401, CommonErrorCode.UNAUTHORIZED.getCode());
        this.failureType = AuthFailureType.OTHER;
    }

    public AuthenticationException(String message, AuthFailureType failureType) {
        super(message, 401, CommonErrorCode.UNAUTHORIZED.getCode());
        this.failureType = failureType;
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, 401, cause);
        this.failureType = AuthFailureType.OTHER;
    }

    /**
     * 认证失败类型枚举
     */
    public enum AuthFailureType {
        /**
         * Token 缺失
         */
        MISSING_TOKEN,
        /**
         * Token 过期
         */
        TOKEN_EXPIRED,
        /**
         * Token 无效
         */
        INVALID_TOKEN,
        /**
         * Token 被吊销
         */
        TOKEN_REVOKED,
        /**
         * 其他原因
         */
        OTHER
    }
}
