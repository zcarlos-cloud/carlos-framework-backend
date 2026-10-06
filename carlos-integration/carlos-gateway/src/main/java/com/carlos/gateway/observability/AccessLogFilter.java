package com.carlos.gateway.observability;

import com.carlos.core.constant.HttpHeadersConstant;
import com.carlos.gateway.config.GlobalFilterOrder;
import com.carlos.gateway.util.ClientIpUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Gateway 访问日志过滤器
 * <p>
 * 记录所有经过 Gateway 的请求信息，包含：
 * - Trace ID
 * - 请求方法、路径
 * - 响应状态码
 * - 耗时
 *
 * @author Carlos
 * @date 2025-03-26
 */
@Slf4j
public class AccessLogFilter implements GlobalFilter, Ordered {

    /**
     * 访问日志 Logger,gateway.access与logback的logname对应
     */
    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("gateway.access");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Instant startTime = Instant.now();
        ServerHttpRequest request = exchange.getRequest();

        // 获取追踪信息
        String traceId = request.getHeaders().getFirst(HttpHeadersConstant.X_TRACE_ID);

        // 记录请求开始
        log.debug("[Gateway Access] 请求开始 - method: {}, path: {}, traceId: {}",
            request.getMethod(), request.getURI().getPath(), traceId);

        return chain.filter(exchange)
            .doFinally(signalType -> {
                // 计算耗时
                long duration = Duration.between(startTime, Instant.now()).toMillis();
                ServerHttpResponse response = exchange.getResponse();

                // 记录访问日志
                recordAccessLog(request, response, resolveTraceId(request), duration);
            });
    }

    /**
     * 解析访问日志使用的 traceId
     * <p>
     * 优先取 MDC（由 {@link MdcContextLifter} 从 Reactor Context 同步而来，是链路 ID 的唯一事实来源），
     * 兜底取 X-Trace-Id 请求头——CANCEL 信号不经过 MdcContextLifter 的 Subscriber 回调，
     * 此时 MDC 可能尚未写入或已被清理，而请求头是稳定的。
     *
     * @param request 当前请求
     * @return traceId，取不到时返回 "-"
     */
    private String resolveTraceId(ServerHttpRequest request) {
        String traceId = MDC.get(TraceContextKeys.TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = request.getHeaders().getFirst(HttpHeadersConstant.X_TRACE_ID);
        }
        return StringUtils.hasText(traceId) ? traceId : "-";
    }

    /**
     * 记录访问日志
     */
    private void recordAccessLog(ServerHttpRequest request, ServerHttpResponse response,
                                 String traceId, long duration) {
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String query = Optional.ofNullable(request.getURI().getQuery()).map(q -> "?" + q).orElse("");
        int status = response.getStatusCode() != null ? response.getStatusCode().value() : 0;

        // 格式：method|path|status|duration|traceId|clientIp
        String clientIp = ClientIpUtil.getClientIp(request);

        String logMessage = new StringBuilder(128)
            .append(method).append('|')
            .append(path).append(query).append('|')
            .append(status).append('|')
            .append(duration).append('|')
            .append(traceId != null ? traceId : "-").append('|')
            .append(clientIp)
            .toString();

        // 使用独立的 Logger 输出到 access 日志文件
        ACCESS_LOG.info(logMessage);

        // 同时输出到主日志
        if (status >= 500) {
            log.error("[Gateway Access] {} {} {} - {}ms, traceId: {}",
                method, path, status, duration, traceId);
        } else if (status >= 400) {
            log.warn("[Gateway Access] {} {} {} - {}ms, traceId: {}",
                method, path, status, duration, traceId);
        } else {
            log.debug("[Gateway Access] {} {} {} - {}ms, traceId: {}",
                method, path, status, duration, traceId);
        }
    }

    @Override
    public int getOrder() {
        // 访问日志过滤器应该最后执行，记录完整请求信息
        return GlobalFilterOrder.ACCESS_LOG_ORDER;
    }
}
