package com.carlos.gateway.config;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.webflux.error.DefaultErrorAttributes;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * 网关基础设施自动配置类
 * <p>
 * 负责基础设施相关的 Bean 配置：
 * - WebClient（HTTP 客户端）
 * - ReactiveStringRedisTemplate（Redis 操作）
 * - 全局异常处理器
 * - 错误属性扩展
 *
 * @author carlos
 * @date 2026/3/27
 */
@Slf4j
@AutoConfiguration
public class InfrastructureAutoConfiguration {

    /**
     * WebClient 构建器（非阻塞优化版）
     * <p>
     * 配置 Reactor Netty HttpClient：
     * - 连接超时 5 秒
     * - 响应超时 10 秒
     * - 启用 Gzip 压缩
     * - 复用底层 TCP 连接（EventLoop 非阻塞 I/O）
     */
    @Bean
    @ConditionalOnMissingBean(name = "webClientBuilder")
    public WebClient.Builder webClientBuilder() {
        // 配置连接池，防止高并发下连接耗尽
        ConnectionProvider connectionProvider = ConnectionProvider.builder("gateway-pool")
            .maxConnections(500)
            .maxIdleTime(java.time.Duration.ofSeconds(30))
            .maxLifeTime(java.time.Duration.ofMinutes(5))
            .pendingAcquireTimeout(java.time.Duration.ofSeconds(10))
            .evictInBackground(java.time.Duration.ofSeconds(30))
            .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(java.time.Duration.ofSeconds(10))
            .compress(true);

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(logRequestFilter())
            .filter(logResponseFilter());
    }

    /**
     * WebClient 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public WebClient webClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.build();
    }

    private ExchangeFilterFunction logRequestFilter() {
        return (request, next) -> {
            if (log.isDebugEnabled()) {
                log.debug("WebClient Request: {} {}", request.method(), request.url());
            }
            return next.exchange(request);
        };
    }

    private ExchangeFilterFunction logResponseFilter() {
        return (request, next) -> next.exchange(request)
            .flatMap(response -> {
                if (log.isDebugEnabled()) {
                    log.debug("WebClient Response: {} {}", response.statusCode(), request.url());
                }
                return Mono.just(response);
            });
    }

    /**
     * Reactive Redis 模板
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
        ReactiveRedisConnectionFactory connectionFactory) {
        return new ReactiveStringRedisTemplate(connectionFactory);
    }

    /**
     * 网关启动器
     */
    @Bean
    public GatewayRunnerWorker gatewayRunnerWorker(GatewayProperties gatewayProperties) {
        return new GatewayRunnerWorker(gatewayProperties);
    }

    /**
     * 网关全局异常处理器
     * 覆盖 Spring Boot 默认的异常处理，返回统一的 JSON 格式错误响应
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ErrorWebExceptionHandler gatewayErrorWebExceptionHandler(ExceptionProperties exceptionProperties) {
        log.info("Initializing Gateway Exception Handler (devMode: {}, showStackTrace: {})",
            exceptionProperties.isDevMode(), exceptionProperties.isShowStackTrace());
        return new GatewayExceptionHandler(exceptionProperties.isDevMode(), exceptionProperties.isShowStackTrace());
    }

    /**
     * 错误属性扩展
     * 扩展 Spring Boot 默认的错误属性，添加网关特定的字段
     */
    @Bean
    @ConditionalOnMissingBean(value = ErrorAttributes.class)
    public DefaultErrorAttributes errorAttributes(ExceptionProperties exceptionProperties) {
        log.info("Initializing Gateway Error Attributes (devMode: {})", exceptionProperties.isDevMode());
        return new GatewayErrorAttributes(exceptionProperties.isDevMode());
    }
}
