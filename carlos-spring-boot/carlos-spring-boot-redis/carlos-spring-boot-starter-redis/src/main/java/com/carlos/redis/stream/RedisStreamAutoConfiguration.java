package com.carlos.redis.stream;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis Stream 原语层自动配置
 * <p>
 * 注册位置：{@code carlos-spring-boot-starter-redis} 的
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * （本模块的 {@code -core} 子模块没有 Redisson 依赖，故不能放在那里）。
 * </p>
 * <p>
 * 不声明 {@code @AutoConfiguration(after = ...)}：本配置的 bean 直接依赖
 * {@code StringRedisTemplate}（由 Spring Boot 提供），依赖注入会自然保证创建顺序。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@EnableConfigurationProperties(RedisStreamProperties.class)
@ConditionalOnProperty(prefix = "carlos.redis.stream", name = "enabled",
    havingValue = "true", matchIfMissing = true)
public class RedisStreamAutoConfiguration {

    /**
     * Stream 原语模板
     * <p>
     * 若未配置长度上限，在启动期告警一次——这是「共用 Redis 实例」场景下的核心保护项。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisStreamTemplate redisStreamTemplate(StringRedisTemplate stringRedisTemplate,
                                                   RedisStreamProperties properties) {
        if (properties.getMaxLength() == null) {
            log.warn("carlos.redis.stream.max-length 未设置：Stream 将无长度上限，"
                + "在与缓存/token 共用 Redis 实例的场景下可能导致内存无界增长，建议显式设置");
        }
        return new DefaultRedisStreamTemplate(stringRedisTemplate, properties);
    }

    /**
     * 指标（MeterRegistry 缺失时自动降级为空操作）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "carlos.redis.stream", name = "metrics-enabled",
        havingValue = "true", matchIfMissing = true)
    public RedisStreamMetrics redisStreamMetrics(ObjectProvider<MeterRegistry> registryProvider,
                                                 RedisStreamTemplate redisStreamTemplate) {
        RedisStreamMetrics metrics = new RedisStreamMetrics(registryProvider.getIfAvailable(),
            redisStreamTemplate);
        if (metrics.isEnabled()) {
            metrics.registerMemory();
        }
        return metrics;
    }

    /**
     * 健康指示器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(HealthIndicator.class)
    public RedisStreamHealthIndicator redisStreamHealthIndicator(RedisStreamTemplate redisStreamTemplate) {
        return new RedisStreamHealthIndicator(redisStreamTemplate);
    }
}
