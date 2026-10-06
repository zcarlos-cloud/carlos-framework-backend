package com.carlos.gateway.exception;

import com.carlos.core.response.CommonErrorCode;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 请求参数校验异常
 * 用于处理请求参数不合法、缺失等场景
 * 返回 HTTP 400 Bad Request
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
@Getter
public class RequestValidationException extends GatewayException {

    private static final long serialVersionUID = 1L;

    /**
     * 字段错误详情
     */
    private final Map<String, String> fieldErrors;

    public RequestValidationException(String message) {
        super(message, 400, CommonErrorCode.BAD_REQUEST.getCode());
        this.fieldErrors = new HashMap<>();
    }

    public RequestValidationException(String message, Map<String, String> fieldErrors) {
        super(message, 400, CommonErrorCode.BAD_REQUEST.getCode());
        this.fieldErrors = fieldErrors != null ? fieldErrors : new HashMap<>();
    }

    public RequestValidationException addFieldError(String field, String error) {
        this.fieldErrors.put(field, error);
        return this;
    }
}
