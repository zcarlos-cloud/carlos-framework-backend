package com.carlos.gateway.observability;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * 链路追踪配置属性
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 */
@Data
@ConfigurationProperties(prefix = "carlos.gateway.tracing")
public class TracingProperties {

    /**
     * 是否启用链路追踪
     */
    private boolean enabled = true;

    /**
     * 是否启用 Reactor Context → MDC 桥接
     * <p>
     * 桥接是全局钩子（{@code Hooks.onEachOperator}），会给每个算子包一层订阅者。
     * 出现性能问题时可单独关闭，关闭后网关自身日志的 traceId 将为空。
     */
    private boolean mdcEnabled = true;

    /**
     * 是否记录请求体
     */
    private boolean recordRequestBody = false;

    /**
     * 是否记录响应体
     */
    private boolean recordResponseBody = false;

    /**
     * 采样率（0.0 - 1.0）
     */
    private double samplingRate = 1.0;

    /**
     * 传播协议类型：B3_SINGLE（默认）、B3_MULTI、ALL
     */
    private String propagation = "B3_SINGLE";

    /**
     * 是否启用 SkyWalking sw8 头传播（当请求包含 sw8 时透传）
     */
    private boolean sw8Propagation = true;

    /**
     * 忽略的 URL 前缀
     */
    private String[] ignorePatterns = {"/actuator/**", "/health", "/prometheus"};
}
