package com.carlos.apm.config;

import com.carlos.core.constant.HttpHeadersConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * APM 模块配置属性
 * <p>
 * 支持渐进式可观测性配置：
 * <ul>
 *   <li><b>mdc</b>: 日志关联（始终启用）</li>
 *   <li><b>tracing</b>: 链路追踪（内置 Brave）</li>
 *   <li><b>skywalking</b>: SkyWalking 增强（自动检测）</li>
 * </ul>
 *
 * @author Carlos
 * @date 2024-12-09
 */
@Data
@ConfigurationProperties(prefix = "carlos.apm")
public class ApmProperties {

    /**
     * 是否启用 APM 模块（需显式设置为 true）
     */
    private boolean enabled = false;

    /**
     * 链路追踪配置（内置 Brave）
     */
    private Tracing tracing = new Tracing();

    /**
     * SkyWalking 配置（自动检测）
     */
    private SkyWalking skywalking = new SkyWalking();

    /**
     * MDC 日志上下文配置
     */
    private Mdc mdc = new Mdc();

    /**
     * Feign 传播配置
     */
    private Feign feign = new Feign();

    // ==================== 子配置类 ====================

    /**
     * 链路追踪配置
     */
    @Data
    public static class Tracing {

        /**
         * 是否启用链路追踪（Layer 2）
         */
        private boolean enabled = true;

        /**
         * 采样率（0.0 - 1.0），1.0 表示全量采样
         */
        private double sampleRate = 1.0;

        /**
         * 传播协议类型
         * <ul>
         *   <li>{@code B3_SINGLE}: 单头格式 b3={traceId}-{spanId}-{sampled}（默认，推荐）</li>
         *   <li>{@code B3_MULTI}: 多头部格式 X-B3-TraceId, X-B3-SpanId</li>
         * </ul>
         */
        private String propagation = "B3_SINGLE";
    }

    /**
     * SkyWalking 增强配置
     */
    @Data
    public static class SkyWalking {

        /**
         * 是否启用 SkyWalking 增强
         * <ul>
         *   <li>{@code AUTO}: 自动检测 Agent（默认）</li>
         *   <li>{@code true}: 强制启用（Agent 不存在时使用 Toolkit 模式）</li>
         *   <li>{@code false}: 禁用</li>
         * </ul>
         */
        private String enabled = "AUTO";

        /**
         * Agent 存在时是否接管 Trace ID 生成
         */
        private boolean takeover = true;

        /**
         * 日志中是否打印 SkyWalking 特有的字段
         */
        private boolean enrichLog = true;
    }

    /**
     * MDC 配置
     */
    @Data
    public static class Mdc {

        /**
         * 是否启用 MDC 上下文
         */
        private boolean enabled = true;

        /**
         * Trace ID 在 MDC 中的 key
         */
        private String traceIdKey = "traceId";

        /**
         * Span ID 在 MDC 中的 key
         */
        private String spanIdKey = "spanId";

        /**
         * 是否将追踪 ID 添加到响应头
         */
        private boolean addToResponse = true;

        /**
         * Trace ID 响应头名称
         */
        private String responseHeaderName = HttpHeadersConstant.X_TRACE_ID;
    }

    /**
     * Feign 传播配置
     */
    @Data
    public static class Feign {

        /**
         * 是否启用 Feign 追踪传播
         */
        private boolean enabled = true;
    }
}
