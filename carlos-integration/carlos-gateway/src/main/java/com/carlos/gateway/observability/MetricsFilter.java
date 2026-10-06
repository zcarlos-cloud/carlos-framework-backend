package com.carlos.gateway.observability;

import com.carlos.gateway.config.GlobalFilterOrder;
import com.carlos.gateway.constant.GatewayHeaderConstants;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * <p>
 * 指标收集过滤器 - 性能优化版
 * 基于 Micrometer 实现 Prometheus 指标暴露
 * </p>
 * <p>
 * 优化点：
 * 1. Counter/Timer 实例缓存（避免每次请求都注册新的指标）
 * 2. 路径模式提取（降低标签基数，如 /api/users/123 → /api/users/{id}）
 * 3. 活跃请求数使用 AtomicInteger + 单次 Gauge 注册
 * 4. 延迟日志参数计算
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 * @updated 2026/4/10 性能优化：指标缓存 + 路径模式提取
 */
@Slf4j
public class MetricsFilter implements GlobalFilter, Ordered {

    private final MeterRegistry meterRegistry;

    // 指标名称常量
    private static final String REQUEST_COUNTER = GatewayHeaderConstants.METRIC_REQUEST_COUNTER;
    private static final String REQUEST_LATENCY = GatewayHeaderConstants.METRIC_REQUEST_LATENCY;
    private static final String ACTIVE_REQUESTS = GatewayHeaderConstants.METRIC_ACTIVE_REQUESTS;
    private static final String METRIC_SUCCESS = GatewayHeaderConstants.METRIC_SUCCESS;
    private static final String METRIC_CLIENT_ERROR = GatewayHeaderConstants.METRIC_CLIENT_ERROR;
    private static final String METRIC_SERVER_ERROR = GatewayHeaderConstants.METRIC_SERVER_ERROR;
    private static final String METRIC_RATE_LIMITED = GatewayHeaderConstants.METRIC_RATE_LIMITED;
    private static final String METRIC_BLOCKED = GatewayHeaderConstants.METRIC_BLOCKED;

    // 指标实例缓存（使用 Caffeine 替代 ConcurrentHashMap，防止无限增长）
    private final Cache<String, Counter> counterCache;
    private final Cache<String, Timer> timerCache;
    private final Cache<String, Counter> statusCounterCache;

    // 活跃请求计数器（原子操作）
    private final AtomicInteger activeRequests = new AtomicInteger(0);

    // 路径模式提取正则
    private static final Pattern UUID_PATTERN = Pattern.compile("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("/\\d+");

    public MetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 预先注册活跃请求数 Gauge（只注册一次）
        Gauge.builder(ACTIVE_REQUESTS, activeRequests, AtomicInteger::get)
            .description("Number of active gateway requests")
            .register(meterRegistry);

        // 初始化 Caffeine 缓存（限制大小 + 过期时间，防止内存泄漏）
        this.counterCache = Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(Duration.ofHours(1))
            .build();
        this.timerCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(1))
            .build();
        this.statusCounterCache = Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(Duration.ofHours(1))
            .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Instant start = Instant.now();

        // 获取路由 ID
        String routeId = Optional.ofNullable(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_PREDICATE_ROUTE_ATTR))
            .map(Object::toString)
            .orElse("unknown");

        // 获取路径模式（降低基数）
        String path = exchange.getRequest().getURI().getPath();
        String pathPattern = extractPathPattern(path);
        String method = exchange.getRequest().getMethod().name();

        // 活跃请求数增加
        activeRequests.incrementAndGet();

        return chain.filter(exchange)
            .doFinally(signalType -> {
                long durationMs = Duration.between(start, Instant.now()).toMillis();
                activeRequests.decrementAndGet();
                recordMetrics(exchange, routeId, pathPattern, method, durationMs);
            });
    }

    /**
     * 提取路径模式（降低标签基数）
     * 例如：/api/users/12345 → /api/users/{id}
     *       /api/orders/550e8400-e29b-41d4-a716-446655440000 → /api/orders/{id}
     */
    private String extractPathPattern(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        // 替换 UUID
        String pattern = UUID_PATTERN.matcher(path).replaceAll("/{id}");
        // 替换数字 ID
        pattern = NUMBER_PATTERN.matcher(pattern).replaceAll("/{id}");

        return pattern;
    }

    /**
     * 记录指标（优化版 - 使用缓存的 Counter 和 Timer）
     */
    private void recordMetrics(ServerWebExchange exchange, String routeId, String pathPattern,
                               String method, long durationMs) {
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        String status = statusCode != null ? String.valueOf(statusCode.value()) : "unknown";

        // 构建缓存键
        String counterKey = routeId + ":" + pathPattern + ":" + method + ":" + status;
        String timerKey = routeId + ":" + method;

        // 获取或创建 Counter（Caffeine 缓存，自动淘汰）
        Counter counter = counterCache.get(counterKey, k ->
            Counter.builder(REQUEST_COUNTER)
                .tag("route", routeId)
                .tag("path_pattern", pathPattern)
                .tag("method", method)
                .tag("status", status)
                .description("Total number of gateway requests")
                .register(meterRegistry)
        );
        counter.increment();

        // 获取或创建 Timer
        Timer timer = timerCache.get(timerKey, k ->
            Timer.builder(REQUEST_LATENCY)
                .tag("route", routeId)
                .tag("method", method)
                .description("Gateway request duration")
                .register(meterRegistry)
        );
        timer.record(durationMs, TimeUnit.MILLISECONDS);

        // 根据状态码分类（使用缓存）
        if (statusCode != null) {
            recordStatusMetrics(routeId, status, statusCode);
        }
    }

    /**
     * 记录状态码分类指标
     */
    private void recordStatusMetrics(String routeId, String status, HttpStatusCode statusCode) {
        String statusKey = routeId + ":" + status;

        if (statusCode.is2xxSuccessful()) {
            Counter successCounter = statusCounterCache.get("success:" + routeId, k ->
                Counter.builder(METRIC_SUCCESS)
                    .tag("route", routeId)
                    .register(meterRegistry)
            );
            successCounter.increment();
        } else if (statusCode.is4xxClientError()) {
            Counter clientErrorCounter = statusCounterCache.get("client_error:" + statusKey, k ->
                Counter.builder(METRIC_CLIENT_ERROR)
                    .tag("route", routeId)
                    .tag("status", status)
                    .register(meterRegistry)
            );
            clientErrorCounter.increment();
        } else if (statusCode.is5xxServerError()) {
            Counter serverErrorCounter = statusCounterCache.get("server_error:" + statusKey, k ->
                Counter.builder(METRIC_SERVER_ERROR)
                    .tag("route", routeId)
                    .tag("status", status)
                    .register(meterRegistry)
            );
            serverErrorCounter.increment();
        }

        // 特定错误统计
        if (statusCode == HttpStatus.TOO_MANY_REQUESTS) {
            Counter rateLimitedCounter = statusCounterCache.get("rate_limited:" + routeId, k ->
                Counter.builder(METRIC_RATE_LIMITED)
                    .tag("route", routeId)
                    .register(meterRegistry)
            );
            rateLimitedCounter.increment();
        }

        if (statusCode == HttpStatus.FORBIDDEN) {
            Counter blockedCounter = statusCounterCache.get("blocked:" + routeId, k ->
                Counter.builder(METRIC_BLOCKED)
                    .tag("route", routeId)
                    .register(meterRegistry)
            );
            blockedCounter.increment();
        }
    }

    /**
     * 获取指标缓存统计（用于监控）
     */
    public Map<String, Long> getCacheStats() {
        return Map.of(
            "counterCacheSize", counterCache.estimatedSize(),
            "timerCacheSize", timerCache.estimatedSize(),
            "statusCounterCacheSize", statusCounterCache.estimatedSize()
        );
    }

    @Override
    public int getOrder() {
        // 紧跟链路追踪之后启动指标收集，使用 doFinally 确保所有请求都被记录
        return GlobalFilterOrder.METRICS_FILTER_ORDER;
    }
}
