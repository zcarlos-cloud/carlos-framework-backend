package com.carlos.apm;

import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.carlos.apm.skywalking.SkyWalkingDetector;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * 分布式追踪工具类（渐进式可观测性）
 * <p>
 * 提供分层 Trace ID 获取能力，自动适配底层实现：
 * <ul>
 *   <li><b>Layer 3</b>：SkyWalking Agent（如已部署，自动接管）</li>
 *   <li><b>Layer 2</b>：Micrometer Tracing + Brave（内置，始终可用）</li>
 *   <li><b>Layer 1</b>：MDC 日志上下文（兜底）</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 获取当前 Trace ID（自动选择最佳来源）
 * String traceId = TraceUtil.getTraceId();
 *
 * // 为当前 Span 添加标签
 * TraceUtil.tag("user.id", userId);
 *
 * // 检查是否在追踪上下文中
 * if (TraceUtil.isInTraceContext()) {
 *     log.info("请求被追踪，Trace ID: {}", TraceUtil.getTraceId());
 * }
 * }</pre>
 *
 * @author Carlos
 * @date 2024-12-09
 * @since 3.0.0
 */
@Slf4j
public final class TraceUtil {

    private TraceUtil() {
        // 工具类禁止实例化
    }

    // ==================== 统一入口（推荐使用） ====================

    /**
     * 获取当前 Trace ID（分层优先级）
     * <ol>
     *   <li>SkyWalking Trace ID（Agent 已部署）</li>
     *   <li>Brave Trace ID（Micrometer Tracing）</li>
     *   <li>MDC traceId（日志层兜底）</li>
     * </ol>
     *
     * @return Trace ID，如果不在追踪上下文中返回空字符串
     */
    public static String getTraceId() {
        // Layer 3: SkyWalking Agent
        if (SkyWalkingDetector.isAgentPresent()) {
            String swTraceId = getSkyWalkingTraceId();
            if (StrUtil.isNotBlank(swTraceId)) {
                return swTraceId;
            }
        }

        // Layer 2: Brave (Micrometer Tracing)
        String braveTraceId = getBraveTraceId();
        if (StrUtil.isNotBlank(braveTraceId)) {
            return braveTraceId;
        }

        // Layer 1: MDC（日志上下文兜底）
        String mdcTraceId = MDC.get("traceId");
        if (StrUtil.isNotBlank(mdcTraceId)) {
            return mdcTraceId;
        }

        return StrUtil.EMPTY;
    }

    /**
     * 获取当前 Span ID
     *
     * @return Span ID，如果不在追踪上下文中返回空字符串
     */
    public static String getSpanId() {
        // Layer 3: SkyWalking
        if (SkyWalkingDetector.isAgentPresent()) {
            String swSpanId = getSkyWalkingSpanId();
            if (StrUtil.isNotBlank(swSpanId)) {
                return swSpanId;
            }
        }

        // Layer 2: Brave
        String braveSpanId = getBraveSpanId();
        if (StrUtil.isNotBlank(braveSpanId)) {
            return braveSpanId;
        }

        // Layer 1: MDC
        String mdcSpanId = MDC.get("spanId");
        if (StrUtil.isNotBlank(mdcSpanId)) {
            return mdcSpanId;
        }

        return StrUtil.EMPTY;
    }

    /**
     * 检查当前是否在任意追踪上下文中
     *
     * @return true 如果在追踪上下文中
     */
    public static boolean isInTraceContext() {
        return StrUtil.isNotBlank(getTraceId());
    }

    /**
     * 为当前 Span 添加标签
     * <p>
     * 自动适配底层实现（SkyWalking 或 Brave）
     *
     * @param key   标签键
     * @param value 标签值
     */
    public static void tag(String key, String value) {
        if (StrUtil.isBlank(key) || StrUtil.isBlank(value)) {
            return;
        }

        // Layer 3: SkyWalking
        if (SkyWalkingDetector.shouldEnhance()) {
            try {
                org.apache.skywalking.apm.toolkit.trace.ActiveSpan.tag(key, value);
                return;
            } catch (Throwable e) {
                log.debug("SkyWalking tag 失败，回退到 Brave: {}", e.getMessage());
            }
        }

        // Layer 2: Brave
        Span currentSpan = getBraveCurrentSpan();
        if (currentSpan != null) {
            currentSpan.tag(key, value);
        }
    }

    /**
     * 为当前 Span 添加事件
     *
     * @param value 事件描述
     */
    public static void annotate(String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        Span currentSpan = getBraveCurrentSpan();
        if (currentSpan != null) {
            currentSpan.annotate(value);
        }
    }

    /**
     * 记录错误到当前 Span
     *
     * @param throwable 异常
     */
    public static void recordError(Throwable throwable) {
        if (throwable == null) {
            return;
        }

        // Layer 3: SkyWalking
        if (SkyWalkingDetector.shouldEnhance()) {
            try {
                org.apache.skywalking.apm.toolkit.trace.ActiveSpan.error(throwable);
                return;
            } catch (Throwable e) {
                log.debug("SkyWalking error 记录失败: {}", e.getMessage());
            }
        }

        // Layer 2: Brave
        Span currentSpan = getBraveCurrentSpan();
        if (currentSpan != null) {
            currentSpan.error(throwable);
            currentSpan.tag("error", throwable.getClass().getSimpleName());
        }
    }

    // ==================== Brave (Layer 2) 专用方法 ====================

    /**
     * 获取 Brave Trace ID
     *
     * @return Trace ID，如果未在追踪上下文中返回空字符串
     */
    public static String getBraveTraceId() {
        try {
            Span currentSpan = getBraveCurrentSpan();
            if (currentSpan != null) {
                return currentSpan.context().traceIdString();
            }
        } catch (Exception e) {
            log.debug("获取 Brave Trace ID 失败: {}", e.getMessage());
        }
        return StrUtil.EMPTY;
    }

    /**
     * 获取 Brave Span ID
     *
     * @return Span ID，如果未在追踪上下文中返回空字符串
     */
    public static String getBraveSpanId() {
        try {
            Span currentSpan = getBraveCurrentSpan();
            if (currentSpan != null) {
                return currentSpan.context().spanIdString();
            }
        } catch (Exception e) {
            log.debug("获取 Brave Span ID 失败: {}", e.getMessage());
        }
        return StrUtil.EMPTY;
    }

    /**
     * 获取 Brave Parent Span ID
     *
     * @return Parent Span ID，如果没有父 Span 返回空字符串
     */
    public static String getParentId() {
        try {
            Span currentSpan = getBraveCurrentSpan();
            if (currentSpan != null) {
                TraceContext context = currentSpan.context();
                return context.parentIdString();
            }
        } catch (Exception e) {
            log.debug("获取 Brave Parent ID 失败: {}", e.getMessage());
        }
        return StrUtil.EMPTY;
    }

    /**
     * Tracer Bean 是否可用（缓存检查结果，避免重复异常）
     */
    private static volatile Boolean tracerAvailable;

    /**
     * 检查 Brave Tracer Bean 是否在 Spring 容器中可用
     */
    private static boolean isBraveTracerAvailable() {
        if (tracerAvailable != null) {
            return tracerAvailable;
        }
        synchronized (TraceUtil.class) {
            if (tracerAvailable != null) {
                return tracerAvailable;
            }
            try {
                tracerAvailable = SpringUtil.getApplicationContext()
                    .getBeanNamesForType(Tracer.class).length > 0;
            } catch (Exception e) {
                tracerAvailable = false;
            }
            return tracerAvailable;
        }
    }

    /**
     * 获取当前 Brave Span
     *
     * @return Span 对象，如果未在上下文中返回 null
     */
    public static Span getBraveCurrentSpan() {
        if (!isBraveTracerAvailable()) {
            return null;
        }
        try {
            Tracer tracer = SpringUtil.getBean(Tracer.class);
            if (tracer == null) {
                return null;
            }
            return tracer.currentSpan();
        } catch (Exception e) {
            log.debug("获取 Brave Span 失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 向后兼容方法 ====================

    /**
     * 获取统一 Trace ID（优先 SkyWalking，其次 Brave）
     *
     * @return Trace ID
     * @deprecated 请直接使用 {@link #getTraceId()}，行为完全一致
     */
    @Deprecated
    public static String getUnifiedTraceId() {
        return getTraceId();
    }

    // ==================== SkyWalking (Layer 3) 专用方法 ====================

    /**
     * 获取 SkyWalking Trace ID
     * <p>
     * 注意：需要在 SkyWalking Agent 启动后才能正常返回，
     * 如果未接入 Agent，将返回空字符串。
     *
     * @return Trace ID
     */
    public static String getSkyWalkingTraceId() {
        if (!SkyWalkingDetector.isToolkitAvailable()) {
            return StrUtil.EMPTY;
        }
        try {
            String traceId = org.apache.skywalking.apm.toolkit.trace.TraceContext.traceId();
            if (StrUtil.isNotBlank(traceId)
                && !"N/A".equals(traceId)
                && !"Ignored_Trace".equals(traceId)) {
                return traceId;
            }
        } catch (Throwable e) {
            log.debug("获取 SkyWalking Trace ID 失败: {}", e.getMessage());
        }
        return StrUtil.EMPTY;
    }

    /**
     * 获取 SkyWalking Segment ID
     *
     * @return Segment ID
     */
    public static String getSkyWalkingSegmentId() {
        if (!SkyWalkingDetector.isToolkitAvailable()) {
            return StrUtil.EMPTY;
        }
        try {
            return org.apache.skywalking.apm.toolkit.trace.TraceContext.segmentId();
        } catch (Throwable e) {
            log.debug("获取 SkyWalking Segment ID 失败: {}", e.getMessage());
            return StrUtil.EMPTY;
        }
    }

    /**
     * 获取 SkyWalking Span ID
     *
     * @return Span ID 的字符串形式
     */
    public static String getSkyWalkingSpanId() {
        if (!SkyWalkingDetector.isToolkitAvailable()) {
            return StrUtil.EMPTY;
        }
        try {
            int spanId = org.apache.skywalking.apm.toolkit.trace.TraceContext.spanId();
            return String.valueOf(spanId);
        } catch (Throwable e) {
            log.debug("获取 SkyWalking Span ID 失败: {}", e.getMessage());
            return StrUtil.EMPTY;
        }
    }

    // ==================== 便捷方法 ====================

    /**
     * 获取完整的追踪上下文信息（用于日志输出和调试）
     *
     * @return 格式化的追踪信息
     */
    public static String getTraceInfo() {
        StringBuilder sb = new StringBuilder("TraceContext{");

        String traceId = getTraceId();
        String spanId = getSpanId();

        sb.append("traceId='").append(traceId).append('\'');
        sb.append(", spanId='").append(spanId).append('\'');

        if (SkyWalkingDetector.isAgentPresent()) {
            String segmentId = getSkyWalkingSegmentId();
            if (StrUtil.isNotBlank(segmentId)) {
                sb.append(", swSegmentId='").append(segmentId).append('\'');
            }
        }

        sb.append("}");
        return sb.toString();
    }
}
