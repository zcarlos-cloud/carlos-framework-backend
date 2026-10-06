package com.carlos.core.constant;

/**
 * HTTP 请求头常量
 * <p>
 * 定义系统中使用的所有 HTTP 请求头标识，包括：
 * - 链路追踪相关（Trace ID、Request ID 等）
 * - B3 传播协议（Sleuth/Micrometer Tracing）
 * - SkyWalking 相关
 * - 认证授权相关
 * - 自定义业务头
 *
 * @author Carlos
 * @date 2025-03-26
 */
public interface HttpHeadersConstant {

    // ==================== 链路追踪 - 业务层 ====================

    /**
     * Request ID 请求头（业务自定义请求标识）
     * <p>
     * 用于单次请求的业务追踪，可由前端传入或网关自动生成
     */
    String X_REQUEST_ID = "X-Request-Id";

    /**
     * Trace ID 请求头（分布式链路追踪标识）
     * <p>
     * Micrometer Tracing / Sleuth 生成的全局唯一追踪 ID
     */
    String X_TRACE_ID = "X-Trace-Id";

    /**
     * Span ID 请求头（链路追踪跨度标识）
     * <p>
     * 标识当前服务在处理链路中的位置
     */
    String X_SPAN_ID = "X-Span-Id";

    /**
     * Parent Span ID 请求头
     * <p>
     * 标识父级 Span，用于构建调用链树
     */
    String X_PARENT_SPAN_ID = "X-Parent-Span-Id";

    // ==================== B3 传播协议 ====================

    /**
     * B3 单头格式（推荐）
     * <p>
     * 格式：{TraceId}-{SpanId}-{Sampling}-{ParentSpanId}
     * 示例：4d1e00c0db9010db86156a4e3c7fcde2-4d1e00c0db9010db-1
     */
    String B3 = "b3";

    /**
     * B3 Trace ID（多头部格式）
     */
    String X_B3_TRACE_ID = "X-B3-TraceId";

    /**
     * B3 Span ID（多头部格式）
     */
    String X_B3_SPAN_ID = "X-B3-SpanId";

    /**
     * B3 Parent Span ID（多头部格式）
     */
    String X_B3_PARENT_SPAN_ID = "X-B3-ParentSpanId";

    /**
     * B3 采样标志（多头部格式）
     * <p>
     * 取值：1（采样）/ 0（不采样）/ true / false
     */
    String X_B3_SAMPLED = "X-B3-Sampled";

    /**
     * B3 调试标志
     * <p>
     * 当设置为 "1" 时，强制采样并忽略采样率设置
     */
    String X_B3_FLAGS = "X-B3-Flags";

    // ==================== SkyWalking ====================

    /**
     * SkyWalking 8.0+ 传播协议头
     * <p>
     * 包含：版本、Trace ID、Parent Segment ID、Parent Span ID、服务名等
     */
    String SW8 = "sw8";

    /**
     * SkyWalking 关联ID（Correlation Context）
     */
    String SW8_CORRELATION = "sw8-correlation";

    /**
     * SkyWalking 承载头（Carrier Header）
     */
    String SW8_X = "sw8-x";

    /**
     * SkyWalking Trace ID（简化版）
     */
    String SW_TRACE_ID = "sw-trace-id";

    // ==================== W3C 标准（可选）====================

    /**
     * W3C Trace Context 标准头
     * <p>
     * 格式：{version}-{trace-id}-{parent-id}-{trace-flags}
     */
    String TRACEPARENT = "traceparent";

    /**
     * W3C Trace State
     */
    String TRACESTATE = "tracestate";

    // ==================== 认证授权 ====================

    /**
     * 认证令牌请求头
     */
    String AUTHORIZATION = "Authorization";

    /**
     * 令牌类型前缀：Bearer
     */
    String BEARER_PREFIX = "Bearer ";

    /**
     * 令牌类型前缀：Basic
     */
    String BASIC_PREFIX = "Basic ";

    /**
     * 用户 ID 请求头
     */
    String X_USER_ID = "X-User-Id";

    /**
     * 用户名请求头
     */
    String X_USER_NAME = "X-User-Name";

    /**
     * 租户 ID 请求头
     */
    String X_TENANT_ID = "X-Tenant-Id";

    /**
     * 角色信息请求头
     *
     * <p>身份已统一由 {@link #X_USER_CONTEXT} 签名断言承载，本头<b>当前无写入方与读取方</b>。
     * 之所以保留，是因为网关的 {@code IdentityHeaderSanitizeFilter} 仍把它列入「必须剥离、
     * 永不可信」的名单 —— 若将来有人重新加回读取方，这里是唯一的兜底，读取方也不得把它
     * 当作可信身份。</p>
     */
    String X_USER_ROLES = "X-User-Roles";

    /**
     * 部门 ID 请求头
     *
     * @see #X_USER_ROLES 关于「无读取方但仍被网关剥离」的说明
     */
    String X_DEPT_ID = "X-Dept-Id";

    /**
     * 网关签发的身份断言请求头
     *
     * <p>形态：{@code v1.<payloadBase64Url>.<signatureBase64Url>}，由网关用共享密钥做
     * HMAC-SHA256 签名，下游服务验签后据此重建登录身份（替代「信任来源 IP」的做法）。
     * 载荷为紧凑 JSON（uid / acc / roles / dept / ten / iat / exp）。</p>
     *
     * <p><b>命名说明：</b>与 {@link com.carlos.core.auth.AuthConstant#USER_CONTEXT} 不同 ——
     * 后者是网关 exchange 的 attribute key，本常量是 HTTP 头名。</p>
     *
     * <p><b>为什么必须以 {@code X-} 开头：</b>{@code carlos-spring-cloud-starter} 的
     * {@code FeignRequestInterceptor} 只转发 {@code x-} 前缀的头，本头借此天然支持
     * 「网关 → 服务 A → 服务 B」的多跳身份传递，无需为 Feign 单独适配。</p>
     */
    String X_USER_CONTEXT = "X-User-Context";

    // ==================== 灰度发布 / 路由 ====================

    /**
     * 灰度版本标记
     */
    String X_GRAY_VERSION = "X-Gray-Version";

    /**
     * 灰度流量标记
     */
    String X_GRAY_TAG = "X-Gray-Tag";

    /**
     * 目标服务版本
     */
    String X_TARGET_VERSION = "X-Target-Version";

    // ==================== 网关 / 代理 ====================

    /**
     * 真实客户端 IP（经过代理后）
     */
    String X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * 原始主机名
     */
    String X_FORWARDED_HOST = "X-Forwarded-Host";

    /**
     * 原始协议
     */
    String X_FORWARDED_PROTO = "X-Forwarded-Proto";

    /**
     * 原始端口
     */
    String X_FORWARDED_PORT = "X-Forwarded-Port";

    /**
     * 客户端真实 IP（Nginx 等常用）
     */
    String X_REAL_IP = "X-Real-IP";

    // ==================== 请求上下文 ====================

    /**
     * 请求来源（内部服务调用标记）
     */
    String X_REQUEST_FROM = "X-Request-From";

    /**
     * 请求时间戳
     */
    String X_REQUEST_TIME = "X-Request-Time";

    /**
     * 设备类型
     */
    String X_DEVICE_TYPE = "X-Device-Type";

    /**
     * 应用版本
     */
    String X_APP_VERSION = "X-App-Version";

    // ==================== 灰度发布 ====================

    /**
     * 灰度发布标记请求头（传递给后端服务）
     */
    String X_GRAY_RELEASE = "X-Gray-Release";

    // ==================== API 版本控制 ====================

    /**
     * API 版本请求头（客户端传入）
     */
    String X_API_VERSION = "X-API-Version";

    /**
     * 路由后的 API 版本请求头（传递给后端）
     */
    String X_ROUTED_VERSION = "X-Routed-Version";

    // ==================== 安全防护 ====================

    /**
     * 请求时间戳头（用于防重放攻击验证）
     */
    String X_TIMESTAMP = "X-Timestamp";

    /**
     * 请求随机数头（用于防重放攻击验证）
     */
    String X_NONCE = "X-Nonce";

    /**
     * 请求签名头（用于防重放攻击验证）
     */
    String X_SIGNATURE = "X-Signature";

    /**
     * CSRF Token 请求头
     */
    String X_CSRF_TOKEN = "X-CSRF-Token";

    // ==================== 缓存控制 ====================

    /**
     * 缓存命中标记头
     */
    String X_CACHE = "X-Cache";

    /**
     * 缓存命中标记值
     */
    String CACHE_HIT = "HIT";

    /**
     * 缓存未命中标记值
     */
    String CACHE_MISS = "MISS";

    // ==================== OAuth2 / Token ====================

    /**
     * Query 参数中的 Token 键名（用于 WebSocket 等场景）
     */
    String ACCESS_TOKEN_PARAM = "access_token";

}
