package com.carlos.gateway.config;

/**
 * Gateway 全局过滤器顺序定义
 * <p>
 * 所有自定义 GlobalFilter 的顺序必须在此接口中定义，严禁在过滤器类中硬编码数字。
 * 按功能分为 9 个阶段，阶段间预留 500~1000 的间隔，方便后续插入新过滤器。
 * <p>
 * Spring Cloud Gateway 使用 Ordered 机制：数值越小优先级越高，越先执行。
 * 内置关键过滤器参考：
 * <ul>
 *   <li>NettyWriteResponseFilter: -1</li>
 *   <li>RouteToRequestUrlFilter: 10000</li>
 *   <li>ReactiveLoadBalancerClientFilter: 10150</li>
 *   <li>NettyRoutingFilter: Integer.MAX_VALUE</li>
 * </ul>
 * 因此所有自定义全局过滤器应安排在 -10000 ~ 10000 之间。
 *
 * @author Carlos
 * @date 2025-01-10
 * @see org.springframework.core.Ordered
 */
public interface GlobalFilterOrder {

    // ==================== 阶段一：可观测性基础 ====================

    /**
     * 链路追踪过滤器顺序
     * <p>必须最先执行，为后续所有过滤器和下游服务生成/传递 traceId 和 requestId。
     * 使用最高优先级（-9000），确保在任何业务逻辑之前完成追踪上下文初始化。
     */
    int TRACING_FILTER_ORDER = -9000;

    /**
     * 指标收集过滤器顺序
     * <p>紧跟链路追踪之后启动，使用 doFinally 模式确保无论请求被哪个后续过滤器拒绝，
     * 指标（Counter/Timer/Gauge）都会被完整记录。
     */
    int METRICS_FILTER_ORDER = -8500;

    // ==================== 阶段二：准入控制 ====================

    /**
     * 统一白名单检查过滤器顺序
     * <p>在安全防护层之前执行，统一检查全局/认证/WAF/防重放/限流/前缀剥离等各类白名单，
     * 将匹配结果存入 Exchange Attributes，供后续过滤器共享，避免各过滤器重复进行 Ant 路径匹配。
     */
    int UNIFIED_WHITELIST_ORDER = -8000;

    /**
     * 身份头净化过滤器顺序
     * <p>紧随统一白名单之后、安全防护层之前：无条件剔除客户端自带的身份头
     * （{@code X-User-*} / {@code user_*} / {@code X-User-Context}），确保下游见到的身份头
     * 只可能来自网关自己。若不净化，命中白名单的路径（OAuth2 过滤器会整个短路）会把客户端
     * 伪造的身份头原样穿透到下游。
     */
    int IDENTITY_SANITIZE_ORDER = -7900;

    // ==================== 阶段三：安全防护 ====================

    /**
     * WAF 防火墙过滤器顺序
     * <p>尽早拦截恶意请求（SQL 注入、XSS、CSRF、路径遍历、敏感文件访问、命令注入），
     * 减少后续无意义的认证/鉴权开销。基于 BitSet 快速字符检查和 ThreadLocal Matcher 缓存实现高性能。
     */
    int WAF_FILTER_ORDER = -7000;

    /**
     * 防重放攻击过滤器顺序
     * <p>在 WAF 之后执行：WAF 是更轻量的正则规则匹配，防重放需要验证时间戳、检查 Nonce（Redis）、
     * 计算 HMAC-SHA256 签名，开销更大。放在 WAF 之后可以优先拦截明显的恶意流量。
     */
    int REPLAY_PROTECTION_ORDER = -6500;

    // ==================== 阶段四：流量控制 ====================

    /**
     * 限流过滤器顺序
     * <p>放在认证之前，可以保护认证服务本身免受暴力破解、Token 刷接口等攻击。
     * 基于 Redis 令牌桶算法，支持 IP/USER/API/COMBINED 等多种维度。
     */
    int RATE_LIMIT_ORDER = -6000;

    // ==================== 阶段五：认证授权 ====================

    /**
     * OAuth2 认证过滤器顺序
     * <p>在安全层（WAF/防重放）和限流之后执行，验证 JWT 或 Opaque Token 的有效性，
     * 构建用户上下文并存入 Exchange Attributes，供后续鉴权和上下文传递过滤器使用。
     */
    int AUTHENTICATION_ORDER = -5000;

    /**
     * OAuth2 鉴权过滤器顺序
     * <p>必须在认证过滤器之后执行，依赖 Exchange 中的 userContext 属性。
     * 支持 RBAC（基于角色）和 ABAC（基于属性）两种权限模式，使用 Caffeine 缓存用户权限以提升性能。
     */
    int AUTHORIZATION_ORDER = -4500;

    // ==================== 阶段六：路由准备 ====================

    /**
     * 灰度发布过滤器顺序
     * <p>在路由决策之前执行（内置 RouteToRequestUrlFilter 顺序为 10000）。
     * 根据用户、IP、权重等策略决定请求路由到灰度实例还是正式实例，并设置灰度标记头。
     */
    int GRAY_RELEASE_ORDER = -3000;

    /**
     * 路径前缀处理过滤器顺序
     * <p>在路由前处理路径格式，例如将 /api/v1/org/users 剥离前缀为 /org/users，
     * 确保后续路由匹配和后端服务收到的路径正确。
     */
    int PATH_PREFIX_ORDER = -2000;

    /**
     * 请求转换过滤器顺序
     * <p>在路径前缀处理之后执行：先完成标准前缀剥离，再执行自定义路径重写、API 版本控制和 Header 转换。
     * 使用预编译 Pattern 缓存避免重复编译正则表达式。
     */
    int REQUEST_TRANSFORM_ORDER = -1000;

    // ==================== 阶段七：上下文传递 ====================

    /**
     * 身份断言签发过滤器顺序
     * <p>在 OAuth2 认证（{@link #AUTHENTICATION_ORDER}）之后、响应缓存（{@link #RESPONSE_CACHE_ORDER}）
     * 之前执行：把认证得到的用户身份签成 {@code X-User-Context} 断言，供下游验签后重建登录身份。
     *
     * <p>历史上此处还有 {@code USER_CONTEXT_RELAY_ORDER = -500} 的「明文头中继」过滤器，它把身份降级成
     * 可伪造的 {@code X-User-*} 头、并靠下游的「可信来源 IP」白名单兜底。该方案有两个结构性问题
     * （挡不住同网段伪造者；内部 Feign 调用会把头转发出去而源 IP 变成发起方服务，导致多主机部署下
     * 每次内部调用都丢身份），已整体替换为签名断言并删除。</p>
     */
    int IDENTITY_SIGN_ORDER = -450;

    // ==================== 阶段八：响应优化 ====================

    /**
     * 响应缓存过滤器顺序
     * <p>在转发到后端服务之前检查缓存，对 GET/HEAD 请求优先返回本地 Caffeine 或 Redis 缓存的响应，
     * 命中则直接返回，避免后端调用。使用 MurmurHash3 快速生成缓存键。
     */
    int RESPONSE_CACHE_ORDER = 100;

    // ==================== 阶段九：访问日志 ====================

    /**
     * 访问日志过滤器顺序
     * <p>使用 doFinally 模式，在过滤链进入时开始计时，在整条链完成后记录完整信息。
     * 因此虽然顺序值较小（在路由前进入链），但实际日志输出发生在所有过滤器和后端响应完成后，
     * 能准确记录最终响应状态码和完整耗时。
     */
    int ACCESS_LOG_ORDER = 1000;

    // ==================== 保留常量（向下兼容）====================

    /** @deprecated 使用 {@link #PATH_PREFIX_ORDER} */
    @Deprecated
    int ORDER_FIRST = 1000;

    /** @deprecated 不再使用 */
    @Deprecated
    int ORDER_SECOND = 2000;

    /** @deprecated 不再使用 */
    @Deprecated
    int ORDER_THIRD = 3000;

    /** @deprecated 不再使用 */
    @Deprecated
    int ORDER_FOURTH = 4000;

    /** @deprecated 不再使用 */
    @Deprecated
    int ORDER_FIFTH = 5000;

    /** @deprecated 不再使用 */
    @Deprecated
    int ORDER_SIXTH = 6000;

    /** @deprecated 不再使用 */
    @Deprecated
    int ORDER_SEVENTH = 7000;

    /** @deprecated 使用 {@link #ACCESS_LOG_ORDER} */
    @Deprecated
    int ORDER_LAST = Integer.MAX_VALUE - 100;

}
