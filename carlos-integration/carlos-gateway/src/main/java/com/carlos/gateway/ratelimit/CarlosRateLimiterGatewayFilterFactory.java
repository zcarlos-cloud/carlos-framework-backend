package com.carlos.gateway.ratelimit;

import cn.hutool.json.JSONUtil;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.gateway.exception.ErrorResponse;
import com.carlos.gateway.ratelimit.config.CarlosRateLimiterProperties;
import com.carlos.gateway.ratelimit.keyresolver.CarlosKeyResolver;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.HttpStatusHolder;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * <p>
 * Carlos 扩展版限流过滤器工厂
 * 基于 Spring Cloud Gateway RequestRateLimiterGatewayFilterFactory 扩展
 * 添加 Carlos 特有的功能：
 * 1. 统一的 ErrorResponse 错误响应格式
 * 2. 内置 KeyResolver 策略（IP/USER/API/COMBINED 等）
 * 3. 更丰富的限流响应头
 * 4. 支持自定义响应配置
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 * @see org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory
 */
@Slf4j
public class CarlosRateLimiterGatewayFilterFactory
    extends AbstractGatewayFilterFactory<CarlosRateLimiterGatewayFilterFactory.Config> {

    private static final String EMPTY_KEY = "____EMPTY_KEY__";

    private final CarlosRedisRateLimiter rateLimiter;
    private final CarlosRateLimiterProperties properties;

    public CarlosRateLimiterGatewayFilterFactory(CarlosRedisRateLimiter rateLimiter,
                                                 CarlosRateLimiterProperties properties) {
        super(Config.class);
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("replenishRate", "burstCapacity", "keyResolver", "requestedTokens");
    }

    @Override
    public GatewayFilter apply(Config config) {
        // 解析 KeyResolver
        KeyResolver resolver = resolveKeyResolver(config);

        // 解析限流器（支持自定义或默认）
        CarlosRedisRateLimiter limiter = config.getRateLimiter() != null
            ? config.getRateLimiter()
            : rateLimiter;

        return (exchange, chain) -> {
            // 如果限流被全局禁用
            if (!properties.isEnabled()) {
                return chain.filter(exchange);
            }

            return resolver.resolve(exchange)
                .defaultIfEmpty(EMPTY_KEY)
                .flatMap(key -> handleRateLimit(exchange, chain, key, limiter, config));
        };
    }

    /**
     * 处理限流逻辑
     */
    private Mono<Void> handleRateLimit(ServerWebExchange exchange,
                                       GatewayFilterChain chain,
                                       String key,
                                       CarlosRedisRateLimiter limiter,
                                       Config config) {
        // 空键处理
        if (EMPTY_KEY.equals(key)) {
            log.warn("Rate limit key resolved to empty");
            return chain.filter(exchange);
        }

        // 获取路由 ID
        String routeId = resolveRouteId(exchange, config);

        // 检查黑名单
        if (limiter.isBlacklisted(key)) {
            log.warn("Request blocked by blacklist: key={}, routeId={}", key, routeId);
            return writeForbiddenResponse(exchange.getResponse(), key, "IP/User is blacklisted");
        }

        // 检查白名单
        if (limiter.isWhitelisted(key)) {
            return chain.filter(exchange);
        }

        // 执行限流检查
        return limiter.isAllowed(routeId, key)
            .flatMap(response -> {
                // 添加响应头
                addRateLimitHeaders(exchange.getResponse(), response.getHeaders());

                if (response.isAllowed()) {
                    return chain.filter(exchange);
                } else {
                    // 触发限流
                    log.warn("Rate limit exceeded: key={}, routeId={}", key, routeId);
                    return writeRateLimitResponse(exchange, key, config);
                }
            })
            .onErrorResume(e -> {
                log.error("Rate limit check failed: key={}, routeId={}", key, routeId, e);
                if (properties.isFailOpen()) {
                    // Redis 故障时放行请求（fail-open 模式，可能影响可用性）
                    return chain.filter(exchange);
                } else {
                    // Redis 故障时拒绝请求（默认 fail-closed 模式，更安全）
                    return writeRateLimitResponse(exchange, key, config);
                }
            });
    }

    /**
     * 解析 KeyResolver
     */
    private KeyResolver resolveKeyResolver(Config config) {
        if (config.getKeyResolver() != null) {
            return config.getKeyResolver();
        }

        // 使用配置的默认策略
        CarlosKeyResolver defaultResolver = properties.getDefaultConfig().getKeyResolver();
        return defaultResolver != null ? defaultResolver.getResolver() : CarlosKeyResolver.IP.getResolver();
    }

    /**
     * 解析路由 ID
     */
    private String resolveRouteId(ServerWebExchange exchange, Config config) {
        if (config.getRouteId() != null) {
            return config.getRouteId();
        }

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route != null ? route.getId() : "default";
    }

    /**
     * 添加限流响应头
     */
    private void addRateLimitHeaders(ServerHttpResponse response, Map<String, String> headers) {
        CarlosRateLimiterProperties.ResponseConfig responseConfig = properties.getResponse();
        if (!responseConfig.isIncludeHeaders()) {
            return;
        }

        headers.forEach((key, value) -> {
            if (value != null) {
                response.getHeaders().add(key, value);
            }
        });

        // 添加标准限流头
        CarlosRateLimiterProperties.HeaderNames headerNames = responseConfig.getHeaderNames();
        if (!headers.containsKey(headerNames.getReset())) {
            response.getHeaders().add(headerNames.getReset(),
                String.valueOf(Instant.now().plusSeconds(60).getEpochSecond()));
        }
    }

    /**
     * 写入限流响应
     */
    private Mono<Void> writeRateLimitResponse(ServerWebExchange exchange, String key, Config config) {
        CarlosRateLimiterProperties.ResponseConfig responseConfig = properties.getResponse();
        ServerHttpResponse response = exchange.getResponse();

        // 设置状态码
        HttpStatus statusCode = config.getStatusCode() != null
            ? config.getStatusCode()
            : HttpStatus.TOO_MANY_REQUESTS;
        setResponseStatus(exchange, HttpStatusHolder.parse(String.valueOf(statusCode.value())));

        // 添加 Retry-After 头
        response.getHeaders().add(responseConfig.getHeaderNames().getRetryAfter(),
            String.valueOf(responseConfig.getRetryAfter()));

        // 如果启用统一错误响应格式
        if (responseConfig.isUniformErrorResponse()) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            ErrorResponse errorResponse = ErrorResponse.builder()
                .status(statusCode.value())
                .code(CommonErrorCode.SERVICE_UNAVAILABLE.getCode())  // 业务错误码
                .msg("请求过于频繁，请稍后重试")
                .extra(Map.of(
                    "limitKey", key,
                    "retryAfter", responseConfig.getRetryAfter()
                ))
                .build();

            return response.writeWith(Mono.fromSupplier(() -> {
                byte[] bytes = JSONUtil.toJsonStr(errorResponse).getBytes(StandardCharsets.UTF_8);
                return response.bufferFactory().wrap(bytes);

            }));
        } else {
            // 简单响应
            return response.setComplete();
        }
    }

    /**
     * 写入禁止访问响应（黑名单）
     */
    private Mono<Void> writeForbiddenResponse(ServerHttpResponse response, String key, String reason) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.FORBIDDEN.value())
            .code(CommonErrorCode.SERVICE_CALL_ERROR.getCode())
            .msg("访问被拒绝")
            .detail(reason)
            .extra(Map.of("key", key))
            .build();

        return response.writeWith(Mono.fromSupplier(() -> {
            byte[] bytes = JSONUtil.toJsonStr(errorResponse).getBytes(StandardCharsets.UTF_8);
            return response.bufferFactory().wrap(bytes);

        }));
    }

    /**
     * 配置类
     */
    @Getter
    @Setter
    @Validated
    public static class Config {

        /**
         * 每秒补充的令牌数
         */
        private Integer replenishRate;

        /**
         * 令牌桶容量
         */
        private Integer burstCapacity;

        /**
         * 每次请求消耗的令牌数
         */
        private Integer requestedTokens;

        /**
         * Key 解析器
         */
        private KeyResolver keyResolver;

        /**
         * 自定义限流器（可选）
         */
        private CarlosRedisRateLimiter rateLimiter;

        /**
         * 限流响应状态码
         */
        private HttpStatus statusCode;

        /**
         * 路由 ID（用于 per-route 配置）
         */
        private String routeId;
    }
}
