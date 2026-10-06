package com.carlos.apm.core;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 统一 Trace ID 生成器
 * <p>
 * 生成和管理分布式追踪的 Trace ID 和 Span ID，支持多种来源提取。
 * <p>
 * 提取优先级：
 * <ol>
 *   <li>SkyWalking sw8 头（增强层）</li>
 *   <li>B3 头（链路层）</li>
 *   <li>X-Trace-ID 头（日志层）</li>
 *   <li>生成新的 Trace ID</li>
 * </ol>
 *
 * @author Carlos
 * @date 2026-06-16
 */
public final class TraceIdGenerator {

    /**
     * Trace ID 默认长度（B3 标准 64 位 = 16 位十六进制）
     */
    public static final int TRACE_ID_LENGTH = 16;

    private TraceIdGenerator() {
        // 工具类禁止实例化
    }

    /**
     * 生成新的 Trace ID
     * <p>
     * 返回 16 位十六进制字符串，与 B3 协议的 64 位 Trace ID 兼容。
     * 使用 {@link IdUtil#fastSimpleUUID()} 的无连字符 UUID 前 16 位（64 bit 随机数），
     * 避免每次调用新建 Snowflake 实例，也不会因同毫秒并发而重复。
     *
     * @return 16 位十六进制 Trace ID
     */
    public static String generateTraceId() {
        return IdUtil.fastSimpleUUID().substring(0, TRACE_ID_LENGTH);
    }

    /**
     * 生成新的 Span ID
     *
     * @return 16 位十六进制 Span ID
     */
    public static String generateSpanId() {
        return IdUtil.fastSimpleUUID().substring(0, TRACE_ID_LENGTH);
    }

    /**
     * 从 B3 单头格式提取 Trace ID
     * <p>
     * 格式：b3={traceId}-{spanId}-{sampled}
     *
     * @param b3Header B3 头值
     * @return Trace ID，解析失败返回 null
     */
    public static String extractTraceIdFromB3(String b3Header) {
        if (StrUtil.isBlank(b3Header)) {
            return null;
        }
        String[] parts = b3Header.split("-");
        if (parts.length >= 2 && StrUtil.isNotBlank(parts[0])) {
            return parts[0];
        }
        return null;
    }

    /**
     * 从 B3 单头格式提取 Span ID
     *
     * @param b3Header B3 头值
     * @return Span ID，解析失败返回 null
     */
    public static String extractSpanIdFromB3(String b3Header) {
        if (StrUtil.isBlank(b3Header)) {
            return null;
        }
        String[] parts = b3Header.split("-");
        if (parts.length >= 2 && StrUtil.isNotBlank(parts[1])) {
            return parts[1];
        }
        return null;
    }

    /**
     * 从 sw8 头提取 Trace ID
     * <p>
     * SkyWalking sw8 格式由 Base64 编码的各字段拼接而成，
     * 首个字段为版本号，第二个字段为 Trace ID。
     * <p>
     * 注意：这里做简易提取，完整解析请使用 SkyWalking Toolkit API。
     *
     * @param sw8Header sw8 头值
     * @return Trace ID，解析失败返回 null
     */
    public static String extractTraceIdFromSw8(String sw8Header) {
        if (StrUtil.isBlank(sw8Header)) {
            return null;
        }
        try {
            // sw8 格式: {version}-{traceId}-... 或 Base64 编码
            // 如果包含 "-" 则按多段解析（明文格式）
            if (sw8Header.contains("-")) {
                String[] parts = sw8Header.split("-");
                if (parts.length >= 2 && StrUtil.isNotBlank(parts[1])) {
                    return parts[1];
                }
            }
        } catch (Exception e) {
            // 解析失败返回 null，由调用方 fallback
        }
        return null;
    }

    /**
     * 构建 B3 单头格式
     *
     * @param traceId  Trace ID
     * @param spanId   Span ID
     * @param sampled  是否采样（true = 1, false = 0）
     * @return B3 头值
     */
    public static String buildB3Header(String traceId, String spanId, boolean sampled) {
        return StrUtil.format("{}-{}-{}", traceId, spanId, sampled ? "1" : "0");
    }

    /**
     * 检查是否为有效的 Trace ID
     *
     * @param traceId Trace ID 字符串
     * @return true 如果有效
     */
    public static boolean isValidTraceId(String traceId) {
        return StrUtil.isNotBlank(traceId) && traceId.length() >= 8;
    }
}
