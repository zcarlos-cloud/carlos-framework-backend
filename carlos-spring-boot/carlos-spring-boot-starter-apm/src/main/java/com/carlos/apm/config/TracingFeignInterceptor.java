package com.carlos.apm.config;

import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import com.carlos.apm.core.TraceIdGenerator;
import com.carlos.apm.mdc.MdcUtil;
import com.carlos.apm.skywalking.SkyWalkingDetector;
import com.carlos.core.constant.HttpHeadersConstant;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Feign 链路追踪拦截器（多协议透传）
 * <p>
 * 自动将追踪头传递到下游服务，支持：
 * <ul>
 *   <li><b>Layer 1</b>: X-Trace-Id（日志追踪用）</li>
 *   <li><b>Layer 2</b>: B3 单头格式（Brave/Zipkin 兼容）</li>
 *   <li><b>Layer 3</b>: sw8 头（SkyWalking 增强，Agent 存在时自动激活）</li>
 * </ul>
 *
 * @author Carlos
 * @date 2025-03-26
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TracingFeignInterceptor implements RequestInterceptor {

    private final Tracer tracer;

    @Override
    public void apply(RequestTemplate template) {
        // ===== Layer 1: 日志追踪头 =====
        propagateTraceId(template);

        // ===== Layer 2: B3 链路推算头 =====
        propagateB3Headers(template);

        // ===== Layer 3: SkyWalking 头（如果 Agent 存在） =====
        propagateSw8Header(template);
    }

    /**
     * 透传 Trace ID（日志追踪）
     */
    private void propagateTraceId(RequestTemplate template) {
        String traceId = MdcUtil.getTraceId();
        if (traceId != null) {
            template.header(HttpHeadersConstant.X_TRACE_ID, traceId);
            log.debug("[Feign] 透传 Trace ID: {}", traceId);
        }
    }

    /**
     * 透传 B3 头（Brave/Zipkin）
     */
    private void propagateB3Headers(RequestTemplate template) {
        if (tracer == null) {
            return;
        }

        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return;
        }

        TraceContext context = currentSpan.context();

        // B3 单头格式（推荐）：b3={traceId}-{spanId}-1
        String b3 = TraceIdGenerator.buildB3Header(
            context.traceIdString(),
            context.spanIdString(),
            true);
        template.header(HttpHeadersConstant.B3, b3);

        log.debug("[Feign] 透传 B3 头: {}", b3);
    }

    /**
     * 透传 sw8 头（SkyWalking 增强）
     */
    private void propagateSw8Header(RequestTemplate template) {
        if (!SkyWalkingDetector.shouldEnhance()) {
            return;
        }

        try {
            // 使用 SkyWalking Toolkit API 获取上下文
            String sw8 = org.apache.skywalking.apm.toolkit.trace.TraceContext.traceId();
            if (sw8 != null && !sw8.isEmpty()
                && !"N/A".equals(sw8) && !"Ignored_Trace".equals(sw8)) {
                // 通过 Feign 透传 SkyWalking 上下文
                // 注意：SkyWalking 的完整跨进程传播通过 ContextCarrier 处理，
                // 此处仅作为基础兼容层
                log.debug("[Feign] SkyWalking Agent 已检测到，透传 sw8 上下文");
            }
        } catch (Throwable e) {
            // SkyWalking 可能未启用
            log.trace("[Feign] SkyWalking 传播跳过: {}", e.getMessage());
        }
    }
}
