package com.carlos.gateway.constant;

/**
 * Gateway 模块专用常量
 * <p>
 * 本模块只保留网关特有的常量，通用 HTTP 请求头请使用：
 * {@link com.carlos.core.constant.HttpHeadersConstant}
 * </p>
 *
 * @author carlos
 * @date 2026/3/27
 * @see com.carlos.core.constant.HttpHeadersConstant
 */
public interface GatewayHeaderConstants {

    // ==================== 灰度发布（网关特有）====================

    /**
     * Exchange 属性中存储灰度标记的键名
     */
    String GRAY_RELEASE_ATTR = "gray.release";

    /**
     * 服务实例元数据中的灰度标记键名
     */
    String GRAY_METADATA_KEY = "gray.release";

    /**
     * 服务实例元数据中的灰度标记（简写形式）
     */
    String GRAY_METADATA_SHORT = "gray";

    // ==================== 请求转换（网关特有）====================

    /**
     * 网关标识请求头
     */
    String X_GATEWAY_ID = "X-Gateway-Id";

    /**
     * 网关转发时间戳请求头
     */
    String X_GATEWAY_TIMESTAMP = "X-Gateway-Timestamp";

    // ==================== 安全防护（Redis 键前缀）====================

    /**
     * Redis 中存储 Nonce 的键前缀
     */
    String NONCE_CACHE_PREFIX = "gateway:nonce:";

    // ==================== 缓存（Redis 键前缀）====================

    /**
     * Redis 缓存键前缀
     */
    String CACHE_KEY_PREFIX = "gateway:cache:response:";

    // ==================== 指标监控（Micrometer 指标名称）====================

    /**
     * Micrometer 指标名称 - 请求总数
     */
    String METRIC_REQUEST_COUNTER = "gateway.requests.total";

    /**
     * Micrometer 指标名称 - 请求延迟
     */
    String METRIC_REQUEST_LATENCY = "gateway.requests.duration";

    /**
     * Micrometer 指标名称 - 活跃请求数
     */
    String METRIC_ACTIVE_REQUESTS = "gateway.requests.active";

    /**
     * Micrometer 指标名称 - 成功请求数
     */
    String METRIC_SUCCESS = "gateway.requests.success";

    /**
     * Micrometer 指标名称 - 客户端错误数
     */
    String METRIC_CLIENT_ERROR = "gateway.requests.client_error";

    /**
     * Micrometer 指标名称 - 服务端错误数
     */
    String METRIC_SERVER_ERROR = "gateway.requests.server_error";

    /**
     * Micrometer 指标名称 - 限流触发数
     */
    String METRIC_RATE_LIMITED = "gateway.requests.rate_limited";

    /**
     * Micrometer 指标名称 - 被阻止请求数
     */
    String METRIC_BLOCKED = "gateway.requests.blocked";

}
