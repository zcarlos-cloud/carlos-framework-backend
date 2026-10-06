package com.carlos.apm.skywalking.util;

import com.carlos.apm.skywalking.SkyWalkingDetector;

/**
 * SkyWalking 增强工具类
 * <p>
 * 提供 SkyWalking APM 追踪相关的工具方法。
 * <p>
 * <b>前置条件</b>：需要部署 SkyWalking Java Agent 才能正常使用。
 * 所有方法都会先通过 {@link SkyWalkingDetector} 检查 Agent 是否存在，
 * 不存在时返回安全的默认值。
 * <p>
 * <b>使用建议</b>：优先使用 {@link com.carlos.apm.TraceUtil} 作为统一入口，
 * 该类提供分层获取能力（SkyWalking → Brave → MDC）。
 *
 * @author Carlos
 * @date 2024-12-09
 */
public final class SkyWalkingUtil {

    private SkyWalkingUtil() {
        // 工具类禁止实例化
    }

    /**
     * 获取 SkyWalking Trace ID
     *
     * @return Trace ID，Agent 不存在时返回空字符串
     */
    public static String getTraceId() {
        if (!SkyWalkingDetector.isToolkitAvailable()) {
            return "";
        }
        try {
            String traceId = org.apache.skywalking.apm.toolkit.trace.TraceContext.traceId();
            if (traceId != null && !"N/A".equals(traceId)
                && !"Ignored_Trace".equals(traceId)) {
                return traceId;
            }
        } catch (Throwable e) {
            // Agent 未部署
        }
        return "";
    }

    /**
     * 获取 SkyWalking Segment ID
     *
     * @return Segment ID
     */
    public static String getSegmentId() {
        if (!SkyWalkingDetector.isToolkitAvailable()) {
            return "";
        }
        try {
            return org.apache.skywalking.apm.toolkit.trace.TraceContext.segmentId();
        } catch (Throwable e) {
            return "";
        }
    }

    /**
     * 获取 SkyWalking Span ID
     *
     * @return Span ID，Agent 不存在时返回 -1
     */
    public static int getSpanId() {
        if (!SkyWalkingDetector.isToolkitAvailable()) {
            return -1;
        }
        try {
            return org.apache.skywalking.apm.toolkit.trace.TraceContext.spanId();
        } catch (Throwable e) {
            return -1;
        }
    }

    /**
     * 检查是否在 SkyWalking 追踪上下文中
     *
     * @return true 如果在追踪上下文中
     */
    public static boolean inTraceContext() {
        String traceId = getTraceId();
        return traceId != null && !traceId.isEmpty();
    }

    /**
     * 设置自定义标签（Agent 不存在时安全跳过）
     *
     * @param key   标签键
     * @param value 标签值
     */
    public static void setTag(String key, String value) {
        if (!inTraceContext() || key == null || value == null) {
            return;
        }
        try {
            org.apache.skywalking.apm.toolkit.trace.ActiveSpan.tag(key, value);
        } catch (Throwable e) {
            // 安全跳过
        }
    }

    /**
     * 记录信息日志到当前 Span
     *
     * @param message 日志信息
     */
    public static void logInfo(String message) {
        if (!inTraceContext() || message == null) {
            return;
        }
        try {
            org.apache.skywalking.apm.toolkit.trace.ActiveSpan.info(message);
        } catch (Throwable e) {
            // 安全跳过
        }
    }

    /**
     * 记录调试日志到当前 Span
     *
     * @param message 日志信息
     */
    public static void logDebug(String message) {
        if (!inTraceContext() || message == null) {
            return;
        }
        try {
            org.apache.skywalking.apm.toolkit.trace.ActiveSpan.debug(message);
        } catch (Throwable e) {
            // 安全跳过
        }
    }

    /**
     * 记录错误信息
     *
     * @param errorMessage 错误信息
     */
    public static void setError(String errorMessage) {
        if (!inTraceContext()) {
            return;
        }
        try {
            org.apache.skywalking.apm.toolkit.trace.ActiveSpan.error(errorMessage);
        } catch (Throwable e) {
            // 安全跳过
        }
    }

    /**
     * 记录异常
     *
     * @param throwable 异常对象
     */
    public static void setError(Throwable throwable) {
        if (!inTraceContext() || throwable == null) {
            return;
        }
        try {
            org.apache.skywalking.apm.toolkit.trace.ActiveSpan.error(throwable);
        } catch (Throwable e) {
            // 安全跳过
        }
    }

    /**
     * 获取统一的 SkyWalking 上下文信息
     *
     * @return 格式: traceId|segmentId|spanId
     */
    public static String getFullContext() {
        if (!inTraceContext()) {
            return "N/A";
        }
        return String.format("%s|%s|%d", getTraceId(), getSegmentId(), getSpanId());
    }
}
