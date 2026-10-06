package com.carlos.gateway.circuitbreaker;

import cn.hutool.json.JSONUtil;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.gateway.exception.ErrorResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * <p>
 * 熔断降级过滤器（基于 Resilience4j）
 * 支持服务级别的熔断保护
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 * @updated 2026/3/24 优化异常处理，统一响应格式
 */
@Slf4j
public class Resilience4jCircuitBreakerFilter extends
    AbstractGatewayFilterFactory<Resilience4jCircuitBreakerFilter.Config> {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Cache<String, CircuitBreaker> circuitBreakers;

    public Resilience4jCircuitBreakerFilter() {
        super(Config.class);

        // 使用 Caffeine 限制 CircuitBreaker 实例数量，防止内存泄漏
        this.circuitBreakers = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofHours(1))
            .build();

        // 默认配置
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)                    // 失败率阈值
            .slowCallRateThreshold(80)                   // 慢调用阈值
            .slowCallDurationThreshold(Duration.ofSeconds(2))  // 慢调用时间阈值
            .permittedNumberOfCallsInHalfOpenState(10)   // 半开状态允许的调用数
            .slidingWindowSize(100)                      // 滑动窗口大小
            .minimumNumberOfCalls(10)                    // 最小调用数
            .waitDurationInOpenState(Duration.ofSeconds(30))  // 熔断持续时间
            .automaticTransitionFromOpenToHalfOpenEnabled(true)  // 自动转换
            .recordException(throwable -> switch (throwable) {
                case TimeoutException e -> true;
                case WebClientResponseException e -> e.getStatusCode().value() >= 500;
                case Exception e -> true;
                default -> false;
            })
            .build();

        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(defaultConfig);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String routeId = exchange.getAttribute(
                org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_ROUTE_ATTR);
            String circuitBreakerName = config.getName() != null ? config.getName() : (routeId != null ? routeId : "default");

            CircuitBreaker circuitBreaker = circuitBreakers.get(circuitBreakerName,
                name -> {
                    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
                    // 添加事件监听器
                    cb.getEventPublisher()
                        .onStateTransition(event -> log.warn("CircuitBreaker [{}] state transition: {} -> {}",
                            name, event.getStateTransition().getFromState(), event.getStateTransition().getToState()))
                        .onError(event -> log.debug("CircuitBreaker [{}] recorded error: {}",
                            name, event.getThrowable().getMessage()))
                        .onSuccess(event -> log.debug("CircuitBreaker [{}] recorded success", name));
                    return cb;
                });

            return chain.filter(exchange)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    CircuitBreaker.State state = circuitBreaker.getState();
                    log.error("Circuit breaker [{}] triggered (state: {}, failureRate: {}%): {}",
                        circuitBreakerName, state, circuitBreaker.getMetrics().getFailureRate(), throwable.getMessage());

                    // 根据配置选择降级方式
                    if (config.getFallbackUri() != null) {
                        // 转发到降级接口
                        exchange.getAttributes().put(
                            org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR,
                            config.getFallbackUri()
                        );
                        return Mono.empty();
                    } else {
                        // 返回统一格式的降级响应
                        return writeCircuitBreakerResponse(exchange.getResponse(), circuitBreakerName, state,
                            circuitBreaker.getMetrics().getFailureRate());
                    }
                });
        };
    }

    /**
     * 写入熔断降级响应
     */
    private Mono<Void> writeCircuitBreakerResponse(ServerHttpResponse response, String circuitBreakerName,
                                                   CircuitBreaker.State state, float failureRate) {
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 构建标准化的错误响应
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.SERVICE_UNAVAILABLE.value())
            .code(CommonErrorCode.SERVICE_UNAVAILABLE.getCode())
            .msg("服务暂时不可用，请稍后重试")
            .extra(java.util.Map.of(
                "circuitBreakerName", circuitBreakerName,
                "circuitBreakerState", state.name(),
                "failureRate", failureRate,
                "retryAfter", 30  // 建议客户端30秒后重试
            ))
            .build();

        // 或者抛出异常让全局异常处理器处理（推荐）
        // 这里选择直接返回响应，避免额外的异常处理开销
        return response.writeWith(Mono.fromSupplier(() -> {
            try {
                byte[] bytes = JSONUtil.toJsonStr(errorResponse).getBytes(StandardCharsets.UTF_8);
                return response.bufferFactory().wrap(bytes);
            } catch (Exception e) {
                log.error("Failed to serialize circuit breaker response", e);
                // 使用 ErrorResponse 对象构建降级响应
                ErrorResponse fallbackResponse = ErrorResponse.builder()
                    .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                    .code("5503")
                    .msg("Service temporarily unavailable")
                    .extra(java.util.Map.of(
                        "circuitBreakerName", circuitBreakerName,
                        "circuitBreakerState", state.name()
                    ))
                    .build();
                return response.bufferFactory().wrap(
                    JSONUtil.toJsonStr(fallbackResponse).getBytes(StandardCharsets.UTF_8));
            }
        }));
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("name", "fallbackUri");
    }

    @Data
    public static class Config {

        private String name;           // 熔断器名称
        private String fallbackUri;    // 降级接口地址
    }
}
