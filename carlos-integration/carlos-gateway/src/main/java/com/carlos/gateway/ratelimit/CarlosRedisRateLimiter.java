package com.carlos.gateway.ratelimit;

import com.carlos.gateway.ratelimit.config.CarlosRateLimiterProperties;
import com.carlos.gateway.ratelimit.event.RateLimitExceededEvent;
import com.carlos.gateway.ratelimit.keyresolver.CarlosKeyResolver;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <p>
 * Carlos 扩展版 Redis 限流器
 * 基于 Spring Cloud Gateway RedisRateLimiter 扩展，添加以下特性：
 * 1. 多维度 KeyResolver 策略（IP/USER/API/COMBINED 等）
 * 2. 黑白名单支持
 * 3. 路由特定的限流配置
 * 4. 限流事件发布（用于监控）
 * 5. 更丰富的响应头信息
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 * @see org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
 */
@Slf4j
public class CarlosRedisRateLimiter extends RedisRateLimiter {

    private final CarlosRateLimiterProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, CarlosKeyResolver> routeKeyResolvers = new ConcurrentHashMap<>();

    /**
     * 创建限流器（用于依赖注入）
     */
    public CarlosRedisRateLimiter(ReactiveStringRedisTemplate redisTemplate,
                                  RedisScript<List<Long>> script,
                                  ConfigurationService configurationService,
                                  CarlosRateLimiterProperties properties,
                                  ApplicationEventPublisher eventPublisher) {
        super(redisTemplate, script, configurationService);
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        initializeRouteConfigs();
    }

    /**
     * 创建限流器（用于 Java DSL）
     */
    public CarlosRedisRateLimiter(int defaultReplenishRate, int defaultBurstCapacity,
                                  CarlosRateLimiterProperties properties,
                                  ApplicationEventPublisher eventPublisher) {
        super(defaultReplenishRate, defaultBurstCapacity);
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 创建限流器（用于 Java DSL，支持自定义请求令牌数）
     */
    public CarlosRedisRateLimiter(int defaultReplenishRate, int defaultBurstCapacity, int defaultRequestedTokens,
                                  CarlosRateLimiterProperties properties,
                                  ApplicationEventPublisher eventPublisher) {
        super(defaultReplenishRate, defaultBurstCapacity, defaultRequestedTokens);
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 初始化路由配置
     */
    @PostConstruct
    public void initializeRouteConfigs() {
        // 加载路由特定的 KeyResolver 配置
        if (properties.getRoutes() != null) {
            for (CarlosRateLimiterProperties.RouteConfig routeConfig : properties.getRoutes()) {
                if (routeConfig.getKeyResolver() != null) {
                    routeKeyResolvers.put(routeConfig.getRouteId(), routeConfig.getKeyResolver());
                }
            }
        }
        log.info("CarlosRedisRateLimiter initialized with {} route configs", routeKeyResolvers.size());
    }

    /**
     * 检查 IP 是否在白名单中
     *
     * @param ip IP 地址
     * @return 是否在白名单中
     */
    public boolean isWhitelisted(String ip) {
        return properties.getWhitelist() != null && properties.getWhitelist().contains(ip);
    }

    /**
     * 检查 IP 是否在黑名单中
     *
     * @param ip IP 地址
     * @return 是否在黑名单中
     */
    public boolean isBlacklisted(String ip) {
        return properties.getBlacklist() != null && properties.getBlacklist().contains(ip);
    }

    /**
     * 获取路由对应的 KeyResolver
     *
     * @param routeId 路由 ID
     * @return KeyResolver 策略
     */
    public CarlosKeyResolver getKeyResolverForRoute(String routeId) {
        return routeKeyResolvers.getOrDefault(routeId, properties.getDefaultConfig().getKeyResolver());
    }

    /**
     * 获取路由配置
     *
     * @param routeId 路由 ID
     * @return 路由配置（如果不存在则返回默认配置）
     */
    public Optional<CarlosRateLimiterProperties.RouteConfig> getRouteConfig(String routeId) {
        if (properties.getRoutes() == null) {
            return Optional.empty();
        }
        return properties.getRoutes().stream()
            .filter(config -> config.getRouteId().equals(routeId))
            .findFirst();
    }

    /**
     * 检查请求是否被允许（带限流事件发布）
     *
     * @param routeId 路由 ID
     * @param id      限流键
     * @return 限流响应
     */
    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        // 如果限流被禁用，直接允许
        if (!properties.isEnabled()) {
            return Mono.just(new Response(true, getHeaders(getDefaultConfig(), -1L)));
        }

        // 检查是否在黑名单中
        if (isBlacklisted(id)) {
            log.warn("Request blocked by blacklist: id={}", id);
            return Mono.just(new Response(false, getHeaders(getDefaultConfig(), 0L)));
        }

        // 检查是否在白名单中
        if (isWhitelisted(id)) {
            return Mono.just(new Response(true, getHeaders(getDefaultConfig(), -1L)));
        }

        return super.isAllowed(routeId, id)
            .doOnNext(response -> {
                // 发布限流事件
                if (!response.isAllowed() && shouldPublishEvent()) {
                    publishRateLimitEvent(routeId, id, response);
                }
            });
    }

    /**
     * 是否应该发布限流事件
     *
     * @return 是否发布
     */
    private boolean shouldPublishEvent() {
        CarlosRateLimiterProperties.MetricsConfig metrics = properties.getMetrics();
        if (!metrics.isEnabled() || !metrics.isPublishEvents()) {
            return false;
        }
        // 采样率检查（使用 ThreadLocalRandom 避免全局同步）
        return ThreadLocalRandom.current().nextDouble() < metrics.getSampleRate();
    }

    /**
     * 发布限流超限事件
     *
     * @param routeId  路由 ID
     * @param id       限流键
     * @param response 限流响应
     */
    private void publishRateLimitEvent(String routeId, String id, Response response) {
        try {
            RateLimitExceededEvent event = new RateLimitExceededEvent(this, routeId, id,
                Instant.now(), response.getHeaders());
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish rate limit event", e);
        }
    }

    /**
     * 获取自定义响应头
     *
     * @param config     配置
     * @param tokensLeft 剩余令牌数
     * @return 响应头映射
     */
    @Override
    public Map<String, String> getHeaders(Config config, Long tokensLeft) {
        Map<String, String> headers = super.getHeaders(config, tokensLeft);

        CarlosRateLimiterProperties.ResponseConfig responseConfig = properties.getResponse();
        if (!responseConfig.isIncludeHeaders()) {
            return headers;
        }

        CarlosRateLimiterProperties.HeaderNames headerNames = responseConfig.getHeaderNames();

        // 添加额外的响应头
        if (tokensLeft >= 0) {
            headers.put(headerNames.getRemaining(), tokensLeft.toString());
        }
        headers.put(headerNames.getLimit(), String.valueOf(config.getBurstCapacity()));
        headers.put(headerNames.getReset(), String.valueOf(Instant.now().plusSeconds(60).getEpochSecond()));

        return headers;
    }

    /**
     * 获取路由特定的限流配置
     * 注：不覆盖父类的 loadConfiguration 方法（包级访问），
     * 而是提供此方法供外部获取自定义配置
     *
     * @param routeId 路由 ID
     * @return 限流配置
     */
    public Config getRouteSpecificConfig(String routeId) {
        Config config = getDefaultConfig();

        // 如果有路由特定配置，应用它
        getRouteConfig(routeId).ifPresent(routeConfig -> {
            if (!routeConfig.isInheritDefault()) {
                config.setReplenishRate(routeConfig.getReplenishRate());
                config.setBurstCapacity(routeConfig.getBurstCapacity());
                config.setRequestedTokens(routeConfig.getRequestedTokens());
            }
        });

        return config;
    }

    /**
     * 获取默认配置
     *
     * @return 默认配置
     */
    public Config getDefaultConfig() {
        CarlosRateLimiterProperties.DefaultConfig defaultConfig = properties.getDefaultConfig();
        Config config = new Config();
        config.setReplenishRate(defaultConfig.getReplenishRate());
        config.setBurstCapacity(defaultConfig.getBurstCapacity());
        config.setRequestedTokens(defaultConfig.getRequestedTokens());
        return config;
    }

    /**
     * 获取属性配置
     *
     * @return 属性配置
     */
    public CarlosRateLimiterProperties getProperties() {
        return properties;
    }

    /**
     * 获取白名单
     *
     * @return 白名单集合
     */
    public Set<String> getWhitelist() {
        return properties.getWhitelist();
    }

    /**
     * 获取黑名单
     *
     * @return 黑名单集合
     */
    public Set<String> getBlacklist() {
        return properties.getBlacklist();
    }
}
