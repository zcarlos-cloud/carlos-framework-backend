package com.carlos.gateway.ratelimit.config;

import com.carlos.gateway.ratelimit.CarlosRateLimiterGatewayFilterFactory;
import com.carlos.gateway.ratelimit.CarlosRedisRateLimiter;
import com.carlos.gateway.ratelimit.keyresolver.CarlosKeyResolver;
import com.carlos.gateway.ratelimit.listener.RateLimitAlertListener;
import com.carlos.gateway.ratelimit.listener.RateLimitMetricsListener;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;
import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * Carlos 限流器自动配置类
 * 基于 Spring Cloud Gateway 自动配置扩展
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "carlos.gateway.rate-limiter.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass({ReactiveStringRedisTemplate.class, RedisScript.class, GatewayProperties.class})
@AutoConfigureAfter({DataRedisAutoConfiguration.class, GatewayAutoConfiguration.class})
@EnableConfigurationProperties(CarlosRateLimiterProperties.class)
public class CarlosRateLimiterAutoConfiguration {

    /**
     * Redis 限流 Lua 脚本名称
     */
    public static final String REDIS_RATE_LIMITER_SCRIPT_NAME = "carlosRedisRateLimiterScript";

    /**
     * 配置默认的 KeyResolver（基于 IP）
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(KeyResolver.class)
    public KeyResolver defaultKeyResolver(CarlosRateLimiterProperties properties) {
        CarlosKeyResolver resolver = properties.getDefaultConfig().getKeyResolver();
        if (resolver == null) {
            resolver = CarlosKeyResolver.IP;
        }
        log.info("Carlos Rate Limiter: Default KeyResolver configured: {}", resolver);
        return resolver.getResolver();
    }

    /**
     * 配置 Redis 限流 Lua 脚本
     */
    @Bean(name = REDIS_RATE_LIMITER_SCRIPT_NAME)
    @SuppressWarnings("unchecked")
    @ConditionalOnMissingBean(name = REDIS_RATE_LIMITER_SCRIPT_NAME)
    public RedisScript<List<Long>> redisRateLimiterScript() {
        // 使用 Spring Cloud Gateway 官方 Lua 脚本
        DefaultRedisScript<List<Long>> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(
            new ClassPathResource("META-INF/scripts/request_rate_limiter.lua")));
        // 使用通配符类型避免编译警告
        redisScript.setResultType((Class<List<Long>>) (Class<?>) List.class);
        log.debug("Carlos Rate Limiter: Redis Lua script loaded");
        return redisScript;
    }

    /**
     * 配置 Carlos 扩展版 Redis 限流器
     */
    @Bean
    @Primary
    @ConditionalOnBean(ReactiveStringRedisTemplate.class)
    @ConditionalOnMissingBean(CarlosRedisRateLimiter.class)
    public CarlosRedisRateLimiter carlosRedisRateLimiter(
        ReactiveStringRedisTemplate redisTemplate,
        ObjectProvider<RedisScript<List<Long>>> redisScriptProvider,
        ConfigurationService configurationService,
        CarlosRateLimiterProperties properties,
        ApplicationEventPublisher eventPublisher) {

        RedisScript<List<Long>> redisScript = redisScriptProvider.getIfAvailable(() -> {
            // 如果脚本未定义，创建默认脚本
            return redisRateLimiterScript();
        });

        log.info("Carlos Rate Limiter: Initializing CarlosRedisRateLimiter");
        return new CarlosRedisRateLimiter(redisTemplate, redisScript, configurationService,
            properties, eventPublisher);
    }

    /**
     * 配置限流过滤器工厂
     */
    @Bean
    @ConditionalOnBean(CarlosRedisRateLimiter.class)
    @ConditionalOnMissingBean(CarlosRateLimiterGatewayFilterFactory.class)
    public CarlosRateLimiterGatewayFilterFactory carlosRateLimiterGatewayFilterFactory(
        CarlosRedisRateLimiter rateLimiter,
        CarlosRateLimiterProperties properties) {

        log.info("Carlos Rate Limiter: Initializing CarlosRateLimiterGatewayFilterFactory");
        return new CarlosRateLimiterGatewayFilterFactory(rateLimiter, properties);
    }

    /**
     * 配置限流指标监听器
     */
    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(RateLimitMetricsListener.class)
    public RateLimitMetricsListener rateLimitMetricsListener(MeterRegistry meterRegistry) {
        log.debug("Carlos Rate Limiter: Initializing RateLimitMetricsListener");
        return new RateLimitMetricsListener(meterRegistry);
    }

    /**
     * 配置限流告警监听器
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitAlertListener.class)
    public RateLimitAlertListener rateLimitAlertListener() {
        log.debug("Carlos Rate Limiter: Initializing RateLimitAlertListener");
        return new RateLimitAlertListener();
    }

    /**
     * 简单的 RedisScript 实现类（用于内部创建默认脚本）
     */
    private static class DefaultRedisScript<T> implements RedisScript<T> {
        private org.springframework.scripting.ScriptSource scriptSource;
        private Class<T> resultType;

        public void setScriptSource(org.springframework.scripting.ScriptSource scriptSource) {
            this.scriptSource = scriptSource;
        }

        public void setResultType(Class<T> resultType) {
            this.resultType = resultType;
        }

        @Override
        public String getSha1() {
            return null;
        }

        @Override
        public Class<T> getResultType() {
            return resultType;
        }

        @Override
        public String getScriptAsString() {
            try {
                return scriptSource.getScriptAsString();
            } catch (Exception e) {
                throw new GlobalException("Failed to load Lua script", e);
            }
        }
    }
}
