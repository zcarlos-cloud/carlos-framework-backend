package com.carlos.redis.config;

import com.carlos.redis.health.RedisHealthIndicator;
import com.carlos.redis.metrics.CacheMetrics;
import com.carlos.redis.metrics.CacheMetricsCollector;
import com.carlos.redis.serialize.ConfigurableRedisSerializer;
import com.carlos.redis.util.RedisUtil;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * <p>
 * 缓存配置
 * </p>
 *
 * <p><strong>为什么必须 {@code before = DataRedisAutoConfiguration}：</strong></p>
 * <p>redisson-spring-boot-starter 的 {@code RedissonAutoConfigurationV4} 声明了
 * {@code @AutoConfiguration(before = DataRedisAutoConfiguration.class)}，
 * 并注册了一个带 {@code @ConditionalOnMissingBean(name = "redisTemplate")} 的同名
 * {@code redisTemplate}。</p>
 * <p>本类必须抢在它之前注册，否则会报「bean 'redisTemplate' 已存在且不允许覆盖」而启动失败。
 * 两者约束相同时按类名字母序排序，{@code com.carlos.*} 先于 {@code org.redisson.*}，故本类胜出。</p>
 * <p><strong>注意：此处不能写成 {@code after}</strong> —— 那会让本类排到 Boot 之后，
 * 而 Redisson 在 Boot 之前，结果仍是 Redisson 先注册，冲突依旧。</p>
 *
 * <p><strong>关于 Spring Cache：</strong>本类不再提供 {@code CacheManager} / {@code KeyGenerator} /
 * {@code CacheErrorHandler}，也不再开启 {@code @EnableCaching}。框架不使用 {@code @Cacheable}
 * 系列注解，缓存一律由各模块在 Manager 层用 {@link RedisUtil} 显式读写、key 前缀自行常量书写；
 * 原先依附注解缓存的 {@code carlos.cache.use-prefix} / {@code key-prefix} 一致前缀机制随之移除
 * （它只挂在 {@code cacheManager()} 上，对 {@link RedisUtil} 手工拼的 key 从来无效）。</p>
 *
 * @author carlos
 * @date 2020/4/13 13:28
 */
@Slf4j
@AutoConfiguration(before = DataRedisAutoConfiguration.class)
@AllArgsConstructor
@EnableConfigurationProperties(CacheProperties.class)
public class RedisCacheConfig implements DisposableBean {

    private final LettuceConnectionFactory factory;
    private final ConfigurableRedisSerializer configurableRedisSerializer;

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // key 序列化使用 String
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        // value 序列化使用配置的序列化器
        template.setValueSerializer(configurableRedisSerializer);
        template.setHashValueSerializer(configurableRedisSerializer);

        template.afterPropertiesSet();
        log.info("RedisTemplate initialized with {} serializer", configurableRedisSerializer.getSerializerType());
        return template;
    }

    // region----------------------  数据结构操作 Bean  ------------------------

    @Bean
    public HashOperations<String, String, Object> hashOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForHash();
    }

    @Bean
    public ValueOperations<String, Object> valueOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForValue();
    }

    @Bean
    public ListOperations<String, Object> listOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForList();
    }

    @Bean
    public SetOperations<String, Object> setOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForSet();
    }

    @Bean
    public ZSetOperations<String, Object> zSetOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForZSet();
    }

    // endregion

    // region----------------------  工具类初始化  ------------------------

    @Bean
    public RedisUtil redisUtilInitializer(@Qualifier("redisTemplate") RedisTemplate<String, Object> redisTemplate) {
        RedisUtil.init(redisTemplate);
        return new RedisUtil();
    }

    @Bean
    public CacheMetrics cacheMetrics(MeterRegistry meterRegistry) {
        CacheMetrics cacheMetrics = new CacheMetrics();
        cacheMetrics.init(meterRegistry);
        return cacheMetrics;
    }

    @Bean
    public CacheMetricsCollector cacheMetricsCollector(CacheMetrics cacheMetrics) {
        return new CacheMetricsCollector(cacheMetrics);
    }

    @Override
    public void destroy() {
        RedisUtil.destroy();
    }

    // endregion

    /**
     * Redis 健康检查指示器
     */
    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    public RedisHealthIndicator redisHealthIndicator(LettuceConnectionFactory factory) {
        return new RedisHealthIndicator(factory);
    }
}
