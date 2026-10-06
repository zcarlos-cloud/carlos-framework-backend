package com.carlos.gateway.observability;

import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.carlos.core.constant.HttpHeadersConstant;
import com.carlos.gateway.config.GlobalFilterOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * 统一链路追踪过滤器（渐进式可观测性）
 * <p>
 * 功能：
 * <ol>
 *   <li>从入站请求提取或生成 Trace ID</li>
 *   <li>创建或继续 Brave Span</li>
 *   <li>向下游服务注入多层传播头：
 *     <ul>
 *       <li>B3（Brave/Zipkin）</li>
 *       <li>sw8（SkyWalking，预留）</li>
 *       <li>X-Trace-ID（日志追踪）</li>
 *     </ul>
 *   </li>
 *   <li>在响应头中返回追踪信息</li>
 * </ol>
 *
 * @author Carlos
 * @date 2025-03-26
 */
@Slf4j
public class RequestTracingFilter implements GlobalFilter, Ordered {

    private final Tracer tracer;
    private final TracingProperties properties;

    /**
     * sw8 传播头名称（SkyWalking）
     */
    static final String SW8_HEADER = "sw8";

    public RequestTracingFilter(Tracer tracer, TracingProperties properties) {
        this.tracer = tracer;
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();

        // 1. 创建或继续 Span
        Span span = createOrContinueSpan(request);

        // 2. 构建传播头
        ServerHttpRequest mutatedRequest = injectPropagationHeaders(
            request, span);

        // 3. 设置响应头
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().set(HttpHeadersConstant.X_TRACE_ID,
            span.context().traceIdString());
        response.getHeaders().set(HttpHeadersConstant.X_SPAN_ID,
            span.context().spanIdString());

        // 4. 构建新的 Exchange
        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(mutatedRequest)
            .build();

        // 5. 将追踪信息放入 Reactor Context
        return chain.filter(mutatedExchange)
            .contextWrite(Context.of(
                "traceId", span.context().traceIdString(),
                "spanId", span.context().spanIdString()
            ))
            .doFinally(signalType -> {
                tagResponse(span, exchange.getResponse());
                span.finish();
                log.debug("[Gateway] 请求完成 - traceId: {}",
                    span.context().traceIdString());
            })
            .onErrorResume(throwable -> {
                span.error(throwable);
                span.finish();
                return Mono.error(throwable);
            });
    }

    // ==================== Span 管理 ====================

    private Span createOrContinueSpan(ServerHttpRequest request) {
        // 尝试从入站请求提取 B3 上下文
        TraceContextOrSamplingFlags extracted = extractB3Context(request);

        Span span = (extracted != null)
            ? tracer.nextSpan(extracted)
            : tracer.nextSpan();

        // 设置 Span 名称和标签
        span.name("gateway." + request.getMethod().name().toLowerCase()
                + request.getURI().getPath())
            .tag("http.method", request.getMethod().name())
            .tag("http.url", request.getURI().toString())
            .tag("http.path", request.getURI().getPath())
            .start();

        // 客户端 IP
        if (request.getRemoteAddress() != null) {
            span.tag("http.client_ip",
                request.getRemoteAddress().getAddress().getHostAddress());
        }

        // 用户 ID（如果已认证）
        String userId = request.getHeaders().getFirst(HttpHeadersConstant.X_USER_ID);
        if (StringUtils.hasText(userId)) {
            span.tag("user.id", userId);
        }

        return span;
    }

    // ==================== B3 提取 ====================

    private TraceContextOrSamplingFlags extractB3Context(ServerHttpRequest request) {
        try {
            // 单头格式: b3={traceId}-{spanId}-{sampled}
            String b3 = request.getHeaders().getFirst(HttpHeadersConstant.B3);
            if (StringUtils.hasText(b3)) {
                String[] parts = b3.split("-");
                if (parts.length >= 2) {
                    TraceContext context = buildTraceContext(parts[0], parts[1],
                        parts.length > 2 ? parts[2] : null);
                    if (context != null) {
                        return TraceContextOrSamplingFlags.create(context);
                    }
                }
            }

            // 多头格式: X-B3-TraceId, X-B3-SpanId
            String traceId = request.getHeaders().getFirst(
                HttpHeadersConstant.X_B3_TRACE_ID);
            String spanId = request.getHeaders().getFirst(
                HttpHeadersConstant.X_B3_SPAN_ID);
            String sampled = request.getHeaders().getFirst(
                HttpHeadersConstant.X_B3_SAMPLED);

            if (StringUtils.hasText(traceId) && StringUtils.hasText(spanId)) {
                TraceContext context = buildTraceContext(traceId, spanId, sampled);
                if (context != null) {
                    return TraceContextOrSamplingFlags.create(context);
                }
            }
        } catch (Exception e) {
            log.warn("[Gateway] 解析 B3 头失败: {}", e.getMessage());
        }

        return null;
    }

    private TraceContext buildTraceContext(String traceIdHex, String spanIdHex,
                                           String sampled) {
        try {
            long traceIdHigh = 0L;
            long traceIdLow;

            if (traceIdHex.length() == 32) {
                traceIdHigh = Long.parseUnsignedLong(traceIdHex.substring(0, 16), 16);
                traceIdLow = Long.parseUnsignedLong(traceIdHex.substring(16), 16);
            } else {
                traceIdLow = Long.parseUnsignedLong(traceIdHex, 16);
            }

            long spanId = Long.parseUnsignedLong(spanIdHex, 16);
            Boolean sampledFlag = sampled != null ? "1".equals(sampled) : null;

            TraceContext.Builder builder = TraceContext.newBuilder()
                .traceIdHigh(traceIdHigh)
                .traceId(traceIdLow)
                .spanId(spanId);

            if (sampledFlag != null) {
                builder.sampled(sampledFlag);
            }

            return builder.build();
        } catch (NumberFormatException e) {
            log.warn("[Gateway] 解析追踪 ID 失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 传播头注入 ====================

    private ServerHttpRequest injectPropagationHeaders(
        ServerHttpRequest request, Span span) {

        TraceContext context = span.context();
        String traceId = context.traceIdString();
        String spanId = context.spanIdString();

        ServerHttpRequest.Builder builder = request.mutate();

        // Layer 1: 日志追踪头
        builder.header(HttpHeadersConstant.X_TRACE_ID, traceId);

        // Layer 2: B3 单头（推荐）
        builder.header(HttpHeadersConstant.B3,
            traceId + "-" + spanId + "-1");

        // B3 多头（兼容旧版）
        builder.header(HttpHeadersConstant.X_B3_TRACE_ID, traceId);
        builder.header(HttpHeadersConstant.X_B3_SPAN_ID, spanId);
        builder.header(HttpHeadersConstant.X_B3_SAMPLED, "1");

        // Layer 3: SkyWalking 头（传递已有 sw8 头）
        String existingSw8 = request.getHeaders().getFirst(SW8_HEADER);
        if (StringUtils.hasText(existingSw8)) {
            builder.header(SW8_HEADER, existingSw8);
        }

        return builder.build();
    }

    // ==================== 响应标签 ====================

    private void tagResponse(Span span, ServerHttpResponse response) {
        if (response.getStatusCode() != null) {
            span.tag("http.status_code",
                String.valueOf(response.getStatusCode().value()));
        }
    }

    @Override
    public int getOrder() {
        return GlobalFilterOrder.TRACING_FILTER_ORDER;
    }
}
