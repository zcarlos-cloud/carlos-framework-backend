package com.carlos.gateway.observability;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 访问日志配置属性
 *
 * @author Carlos
 * @date 2025-03-26
 */
@Data
@ConfigurationProperties(prefix = "carlos.gateway.access-log")
public class AccessLogProperties {

    /**
     * 是否启用访问日志
     */
    private boolean enabled = true;

    /**
     * 是否记录请求体
     */
    private boolean logRequestBody = false;

    /**
     * 是否记录响应体
     */
    private boolean logResponseBody = false;

    /**
     * 慢请求阈值（毫秒），超过此值的请求会标记为慢请求
     */
    private long slowRequestThreshold = 1000;

    /**
     * 忽略的 URL 路径模式
     */
    private String[] ignorePatterns = {"/actuator/**", "/health", "/prometheus", "/favicon.ico"};
}
