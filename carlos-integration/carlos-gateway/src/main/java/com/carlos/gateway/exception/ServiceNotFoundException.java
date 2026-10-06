package com.carlos.gateway.exception;

import com.carlos.core.response.CommonErrorCode;

/**
 * <p>
 * 服务未找到异常
 * 用于处理路由不存在、服务未注册等场景
 * 返回 HTTP 404 Not Found
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
public class ServiceNotFoundException extends GatewayException {

    private static final long serialVersionUID = 1L;

    /**
     * 请求的服务名
     */
    private final String serviceName;

    /**
     * 请求的路径
     */
    private final String requestPath;

    public ServiceNotFoundException(String message) {
        super(message, 404, CommonErrorCode.NOT_FOUND.getCode());
        this.serviceName = null;
        this.requestPath = null;
    }

    public ServiceNotFoundException(String serviceName, String requestPath) {
        super(String.format("服务 [%s] 未找到，请求路径: %s", serviceName, requestPath),
            404, CommonErrorCode.NOT_FOUND.getCode());
        this.serviceName = serviceName;
        this.requestPath = requestPath;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getRequestPath() {
        return requestPath;
    }
}
