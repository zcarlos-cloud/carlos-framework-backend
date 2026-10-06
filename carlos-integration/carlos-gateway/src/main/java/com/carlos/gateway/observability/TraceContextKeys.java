package com.carlos.gateway.observability;

/**
 * Reactor Context 与 SLF4J MDC 共用的链路追踪键名
 * <p>
 * 键名必须保持一致，否则桥接会静默失效：
 * <ul>
 *   <li>{@code RequestTracingFilter} 按这些键写入 Reactor Context</li>
 *   <li>{@link MdcContextLifter} 按这些键从 Context 读出并写入 MDC</li>
 *   <li>logback 通过 {@code %X{traceId}} 消费 MDC</li>
 * </ul>
 *
 * @author Carlos
 * @date 2026-09-18
 */
public interface TraceContextKeys {

    /**
     * Trace ID 键名
     */
    String TRACE_ID = "traceId";

    /**
     * Span ID 键名
     */
    String SPAN_ID = "spanId";
}
