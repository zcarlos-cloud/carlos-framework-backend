package com.carlos.apm.mdc;

import brave.Span;
import brave.Tracer;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.carlos.apm.skywalking.SkyWalkingDetector;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * MDC (Mapped Diagnostic Context) 工具类
 * <p>
 * 管理日志上下文中的 Trace ID / Span ID 等追踪信息。
 * <p>
 * 设计原则：
 * <ul>
 *   <li>Brave 存在时使用 Brave 的 Trace ID</li>
 *   <li>SkyWalking Agent 存在时自动增强</li>
 *   <li>始终有兜底（UUID 生成）</li>
 * </ul>
 *
 * @author Carlos
 * @date 2024-12-09
 */
@Slf4j
public final class MdcUtil {

    /**
     * MDC 中 Trace ID 的 key
     */
    public static final String MDC_TRACE_ID_KEY = "traceId";

    /**
     * MDC 中 Span ID 的 key
     */
    public static final String MDC_SPAN_ID_KEY = "spanId";

    /**
     * MDC 中 SkyWalking Trace ID 的 key
     */
    public static final String MDC_SKYWALKING_TRACE_ID_KEY = "tid";

    private MdcUtil() {
        // 工具类禁止实例化
    }

    // ==================== Trace ID ====================

    /**
     * 从 Brave Tracer 获取 Trace ID 并设置到 MDC
     * <p>
     * 如果 Brave 不可用，尝试 SkyWalking；都不可用则不做任何操作。
     */
    public static void setTraceIdFromBrave() {
        // 先检查 Tracer Bean 是否存在，避免异常日志噪音
        if (!isBraveTracerAvailable()) {
            return;
        }
        try {
            Tracer tracer = SpringUtil.getBean(Tracer.class);
            if (tracer == null) {
                return;
            }
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                String traceId = currentSpan.context().traceIdString();
                if (StrUtil.isNotBlank(traceId)) {
                    setTraceId(traceId);
                    setSpanId(currentSpan.context().spanIdString());
                }
            }
        } catch (Exception e) {
            log.debug("从 Brave 设置 MDC 失败: {}", e.getMessage());
        }
    }

    /**
     * Tracer Bean 是否可用（缓存检查结果，避免重复异常）
     */
    private static volatile Boolean tracerAvailable;

    private static boolean isBraveTracerAvailable() {
        if (tracerAvailable != null) {
            return tracerAvailable;
        }
        synchronized (MdcUtil.class) {
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
     * 设置 MDC 中的 Trace ID
     *
     * @param traceId Trace ID
     */
    public static void setTraceId(String traceId) {
        if (StrUtil.isNotBlank(traceId)) {
            MDC.put(MDC_TRACE_ID_KEY, traceId);
        }
    }

    /**
     * 设置 MDC 中的 Span ID
     *
     * @param spanId Span ID
     */
    public static void setSpanId(String spanId) {
        if (StrUtil.isNotBlank(spanId)) {
            MDC.put(MDC_SPAN_ID_KEY, spanId);
        }
    }

    /**
     * 获取 MDC 中的 Trace ID
     *
     * @return Trace ID，如果不存在返回 null
     */
    public static String getTraceId() {
        return MDC.get(MDC_TRACE_ID_KEY);
    }

    /**
     * 获取 MDC 中的 Span ID
     *
     * @return Span ID，如果不存在返回 null
     */
    public static String getSpanId() {
        return MDC.get(MDC_SPAN_ID_KEY);
    }

    // ==================== SkyWalking 增强 ====================

    /**
     * 设置 SkyWalking Trace ID 到 MDC（仅当 Agent 存在时）
     */
    public static void setSkyWalkingTraceIdIfPresent() {
        if (!SkyWalkingDetector.isToolkitAvailable()) {
            return;
        }
        try {
            String traceId = org.apache.skywalking.apm.toolkit.trace.TraceContext.traceId();
            if (StrUtil.isNotBlank(traceId)
                && !"N/A".equals(traceId)
                && !"Ignored_Trace".equals(traceId)) {
                MDC.put(MDC_SKYWALKING_TRACE_ID_KEY, traceId);

                // SkyWalking 存在时，使用它的 Trace ID 作为主 Trace ID
                if (MDC.get(MDC_TRACE_ID_KEY) == null) {
                    setTraceId(traceId);
                }
            }
        } catch (Throwable e) {
            log.debug("设置 SkyWalking Trace ID 到 MDC 失败: {}", e.getMessage());
        }
    }

    // ==================== 生命周期 ====================

    /**
     * 清除 MDC 中的追踪信息
     * <p>
     * 必须在请求结束时调用，防止内存泄漏和跨请求污染。
     * <p>
     * 只移除本工具类写入的三个追踪键，不使用 {@link MDC#clear()}——
     * 请求线程上可能还有业务组件自己写入的 MDC 条目（如消息模块的 messageId），
     * 整体清空会误删它们。
     */
    public static void clear() {
        MDC.remove(MDC_TRACE_ID_KEY);
        MDC.remove(MDC_SPAN_ID_KEY);
        MDC.remove(MDC_SKYWALKING_TRACE_ID_KEY);
    }

    // ==================== 通用 MDC 操作 ====================

    /**
     * 设置自定义 MDC 值
     *
     * @param key   键
     * @param value 值
     */
    public static void put(String key, String value) {
        if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(value)) {
            MDC.put(key, value);
        }
    }

    /**
     * 获取自定义 MDC 值
     *
     * @param key 键
     * @return 值
     */
    public static String get(String key) {
        return MDC.get(key);
    }

    /**
     * 移除指定 MDC 值
     *
     * @param key 键
     */
    public static void remove(String key) {
        if (StrUtil.isNotBlank(key)) {
            MDC.remove(key);
        }
    }
}
