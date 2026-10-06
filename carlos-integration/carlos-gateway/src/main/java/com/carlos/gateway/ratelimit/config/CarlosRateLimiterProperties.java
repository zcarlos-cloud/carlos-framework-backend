package com.carlos.gateway.ratelimit.config;

import com.carlos.gateway.ratelimit.keyresolver.CarlosKeyResolver;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Carlos 限流器配置属性
 * 支持多维度限流策略配置
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
@Data
@Validated
@ConfigurationProperties(prefix = "carlos.gateway.rate-limiter")
public class CarlosRateLimiterProperties {

    /**
     * 是否启用限流
     */
    private boolean enabled = true;

    /**
     * Redis 故障时是否放行（fail-open）。
     * true = Redis 故障时放行请求（可能影响可用性）
     * false = Redis 故障时拒绝请求（默认，更安全）
     */
    private boolean failOpen = false;

    /**
     * 默认限流配置
     */
    private DefaultConfig defaultConfig = new DefaultConfig();

    /**
     * 路由特定的限流配置
     */
    private List<RouteConfig> routes = new ArrayList<>();

    /**
     * 白名单（不限流的 IP）
     */
    private Set<String> whitelist = new HashSet<>();

    /**
     * 黑名单（直接拒绝的 IP）
     */
    private Set<String> blacklist = new HashSet<>();

    /**
     * 响应配置
     */
    private ResponseConfig response = new ResponseConfig();

    /**
     * 监控配置
     */
    private MetricsConfig metrics = new MetricsConfig();

    /**
     * 默认限流配置
     */
    @Data
    public static class DefaultConfig {

        /**
         * 每秒补充的令牌数
         */
        @Min(1)
        private int replenishRate = 10;

        /**
         * 令牌桶容量
         */
        @Min(1)
        private int burstCapacity = 20;

        /**
         * 每次请求消耗的令牌数
         */
        @Min(1)
        private int requestedTokens = 1;

        /**
         * Key 解析策略
         */
        @NotNull
        private CarlosKeyResolver keyResolver = CarlosKeyResolver.IP;

        /**
         * 限流响应状态码
         */
        private int statusCode = 429;
    }

    /**
     * 路由特定配置
     */
    @Data
    public static class RouteConfig {

        /**
         * 路由 ID
         */
        @NotNull
        private String routeId;

        /**
         * 每秒补充的令牌数
         */
        @Min(1)
        private int replenishRate = 10;

        /**
         * 令牌桶容量
         */
        @Min(1)
        private int burstCapacity = 20;

        /**
         * 每次请求消耗的令牌数
         */
        @Min(1)
        private int requestedTokens = 1;

        /**
         * Key 解析策略
         */
        private CarlosKeyResolver keyResolver;

        /**
         * 是否继承默认配置
         */
        private boolean inheritDefault = true;
    }

    /**
     * 响应配置
     */
    @Data
    public static class ResponseConfig {

        /**
         * 是否包含限流响应头
         */
        private boolean includeHeaders = true;

        /**
         * 是否返回统一错误响应格式
         */
        private boolean uniformErrorResponse = true;

        /**
         * 建议重试时间（秒）
         */
        private int retryAfter = 60;

        /**
         * 自定义响应头名称
         */
        private HeaderNames headerNames = new HeaderNames();
    }

    /**
     * 响应头名称配置
     */
    @Data
    public static class HeaderNames {
        private String remaining = "X-RateLimit-Remaining";
        private String limit = "X-RateLimit-Limit";
        private String reset = "X-RateLimit-Reset";
        private String retryAfter = "Retry-After";
    }

    /**
     * 监控配置
     */
    @Data
    public static class MetricsConfig {

        /**
         * 是否启用限流指标收集
         */
        private boolean enabled = true;

        /**
         * 是否发布限流事件
         */
        private boolean publishEvents = true;

        /**
         * 采样率（0.0 - 1.0）
         */
        private double sampleRate = 1.0;
    }
}
