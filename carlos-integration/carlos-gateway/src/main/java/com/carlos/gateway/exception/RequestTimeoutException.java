package com.carlos.gateway.exception;

import com.carlos.core.response.CommonErrorCode;

/**
 * <p>
 * 请求超时异常
 * 用于处理后端服务响应超时等场景
 * 返回 HTTP 504 Gateway Timeout
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
public class RequestTimeoutException extends GatewayException {

    private static final long serialVersionUID = 1L;

    /**
     * 超时时间（毫秒）
     */
    private final long timeoutMs;

    /**
     * 目标服务
     */
    private final String targetService;

    public RequestTimeoutException(String message) {
        super(CommonErrorCode.SERVER_TIMEOUT, message);
        this.timeoutMs = 0;
        this.targetService = null;
    }

    public RequestTimeoutException(String message, long timeoutMs, String targetService) {
        super(CommonErrorCode.SERVER_TIMEOUT, message);
        this.timeoutMs = timeoutMs;
        this.targetService = targetService;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public String getTargetService() {
        return targetService;
    }
}
