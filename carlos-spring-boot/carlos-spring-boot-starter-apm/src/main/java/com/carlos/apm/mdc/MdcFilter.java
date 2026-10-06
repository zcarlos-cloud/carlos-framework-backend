package com.carlos.apm.mdc;

import com.carlos.apm.config.ApmProperties;
import com.carlos.apm.core.TraceIdGenerator;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

/**
 * MDC 过滤器（统一追踪上下文注入）
 * <p>
 * 在请求处理开始时将 Trace ID / Span ID 设置到 MDC 中，
 * 确保整个请求生命周期日志中包含追踪信息。
 * <p>
 * Trace ID 提取优先级：
 * <ol>
 *   <li>B3 单头（b3={traceId}-{spanId}-{sampled}）</li>
 *   <li>B3 多头（X-B3-TraceId）</li>
 *   <li>sw8 头（SkyWalking，预留）</li>
 *   <li>X-Trace-ID 头</li>
 *   <li>生成新的</li>
 * </ol>
 *
 * @author Carlos
 * @date 2024-12-09
 */
@Slf4j
public class MdcFilter implements Filter {

    /**
     * B3 单头名称
     */
    private static final String B3_HEADER = "b3";

    /**
     * B3 Trace ID 头（多头部格式）
     */
    private static final String X_B3_TRACE_ID = "X-B3-TraceId";

    /**
     * B3 Span ID 头（多头部格式）
     */
    private static final String X_B3_SPAN_ID = "X-B3-SpanId";

    /**
     * SkyWalking 传播头
     */
    private static final String SW8_HEADER = "sw8";

    private final ApmProperties apmProperties;

    public MdcFilter(ApmProperties apmProperties) {
        this.apmProperties = apmProperties;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.debug("[APM] MDC Filter 已初始化");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // 1. 提取或生成 Trackable 上下文
            String traceId = resolveTraceId(httpRequest);
            String spanId = resolveSpanId(httpRequest);

            // 2. 设置到 MDC
            MdcUtil.setTraceId(traceId);
            MdcUtil.setSpanId(spanId);

            // 3. 尝试从 Brave 获取当前 Span 的 Trace ID（覆盖占位值）
            MdcUtil.setTraceIdFromBrave();

            // 4. 尝试 SkyWalking 增强
            MdcUtil.setSkyWalkingTraceIdIfPresent();

            // 5. 响应头返回
            if (apmProperties.getMdc().isAddToResponse()) {
                addResponseHeaders(httpResponse);
            }

            log.debug("[APM] MDC 上下文已设置 - traceId: {}", MdcUtil.getTraceId());

            chain.doFilter(request, response);

        } finally {
            MdcUtil.clear();
        }
    }

    /**
     * 解析 Trace ID
     */
    private String resolveTraceId(HttpServletRequest request) {
        // 1. B3 单头
        String b3 = request.getHeader(B3_HEADER);
        String traceId = TraceIdGenerator.extractTraceIdFromB3(b3);
        if (traceId != null) {
            return traceId;
        }

        // 2. B3 多头
        String xB3TraceId = request.getHeader(X_B3_TRACE_ID);
        if (xB3TraceId != null && !xB3TraceId.isEmpty()) {
            return xB3TraceId;
        }

        // 3. sw8（预留）
        String sw8 = request.getHeader(SW8_HEADER);
        traceId = TraceIdGenerator.extractTraceIdFromSw8(sw8);
        if (traceId != null) {
            return traceId;
        }

        // 4. X-Trace-ID
        String xTraceId = request.getHeader(apmProperties.getMdc().getResponseHeaderName());

        if (xTraceId != null && !xTraceId.isEmpty()) {
            return xTraceId;
        }

        // 5. 生成新的
        return TraceIdGenerator.generateTraceId();
    }

    /**
     * 解析 Span ID
     */
    private String resolveSpanId(HttpServletRequest request) {
        // 1. B3 单头
        String b3 = request.getHeader(B3_HEADER);
        String spanId = TraceIdGenerator.extractSpanIdFromB3(b3);
        if (spanId != null) {
            return spanId;
        }

        // 2. B3 多头
        String xB3SpanId = request.getHeader(X_B3_SPAN_ID);
        if (xB3SpanId != null && !xB3SpanId.isEmpty()) {
            return xB3SpanId;
        }

        // 3. 生成新的
        return TraceIdGenerator.generateSpanId();
    }

    /**
     * 添加响应头
     */
    private void addResponseHeaders(HttpServletResponse response) {
        Optional.ofNullable(MdcUtil.getTraceId())
            .ifPresent(traceId ->
                response.setHeader(apmProperties.getMdc().getResponseHeaderName(), traceId));
    }

    @Override
    public void destroy() {
        log.debug("[APM] MDC Filter 已销毁");
    }
}
