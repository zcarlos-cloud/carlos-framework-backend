package com.carlos.gateway.exception;

import com.carlos.core.response.CommonErrorCode;

/**
 * <p>
 * 熔断异常
 * 用于处理服务熔断触发时的场景
 * 返回 HTTP 503 Service Unavailable
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
public class CircuitBreakerException extends GatewayException {

    private static final long serialVersionUID = 1L;

    /**
     * 熔断器名称
     */
    private final String circuitBreakerName;

    /**
     * 熔断器状态（OPEN、HALF_OPEN、CLOSED）
     */
    private final String circuitBreakerState;

    /**
     * 失败率
     */
    private final float failureRate;

    public CircuitBreakerException(String message) {
        super(CommonErrorCode.SERVICE_CALL_ERROR, message);
        this.circuitBreakerName = "unknown";
        this.circuitBreakerState = "UNKNOWN";
        this.failureRate = 0.0f;
    }

    public CircuitBreakerException(String message, String circuitBreakerName, String circuitBreakerState, float failureRate) {
        super(CommonErrorCode.SERVICE_CALL_ERROR, message);
        this.circuitBreakerName = circuitBreakerName;
        this.circuitBreakerState = circuitBreakerState;
        this.failureRate = failureRate;
    }

    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }

    public String getCircuitBreakerState() {
        return circuitBreakerState;
    }

    public float getFailureRate() {
        return failureRate;
    }
}
