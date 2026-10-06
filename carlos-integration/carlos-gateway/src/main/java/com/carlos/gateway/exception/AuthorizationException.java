package com.carlos.gateway.exception;

import com.carlos.core.response.CommonErrorCode;

/**
 * <p>
 * 授权异常
 * 用于处理权限不足、访问被拒绝等场景
 * 返回 HTTP 403 Forbidden
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
public class AuthorizationException extends GatewayException {

    private static final long serialVersionUID = 1L;

    public AuthorizationException(String message) {
        super(message, 403, CommonErrorCode.FORBIDDEN.getCode());
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, 403, cause);
    }
}
