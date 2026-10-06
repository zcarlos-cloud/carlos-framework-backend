package com.carlos.redis.config;

import com.carlos.redis.serialize.ConfigurableRedisSerializer;
import com.carlos.redis.util.ReactiveRedisUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import reactor.core.publisher.Flux;

/**
 * <p>
 * Reactive Redis 配置类
 * </p>
 *
 * <p>
 * 为 Spring Cloud Gateway 等 WebFlux 应用提供 Reactive Redis 支持。
 * 基于 Lettuce 连接工厂，利用其原生 Reactive 特性。
 * </p>
 *
 * @author carlos
 * @date 2026-03-24
 */
@Slf4j
@AutoConfiguration(before = DataRedisAutoConfiguration.class)
@ConditionalOnClass({ReactiveRedisTemplate.class, Flux.class})
@ConditionalOnProperty(name = "carlos.cache.reactive.enabled", havingValue = "true", matchIfMissing = true)
@AllArgsConstructor
public class ReactiveRedisConfig {

    private final LettuceConnectionFactory lettuceConnectionFactory;
    private final CacheProperties cacheProperties;

    /**
     * 创建 Reactive Redis 序列化上下文
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisSerializationContext<String, Object> reactiveRedisSerializationContext(
        ConfigurableRedisSerializer valueSerializer) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        return RedisSerializationContext.<String, Object>newSerializationContext()
            .key(keySerializer)
            .hashKey(keySerializer)
            .value(valueSerializer)
            .hashValue(valueSerializer)
            .build();
    }

    /**
     * 配置 Reactive Redis Template
     * <p>
     * 用于通用对象存储，使用配置的序列化策略
     * </p>
     */
    @Primary
    @Bean
    @ConditionalOnMissingBean(name = "reactiveRedisTemplate")
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
        RedisSerializationContext<String, Object> serializationContext) {
        log.info("Initializing ReactiveRedisTemplate with serializer: {}",
            cacheProperties.getSerializer().getCode());
        return new ReactiveRedisTemplate<>(lettuceConnectionFactory, serializationContext);
    }

    /**
     * 配置 Reactive String Redis Template
     * <p>
     * 用于字符串操作，key 和 value 都使用 String 序列化
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(ReactiveStringRedisTemplate.class)
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate() {
        log.info("Initializing ReactiveStringRedisTemplate");
        return new ReactiveStringRedisTemplate(lettuceConnectionFactory);
    }

    // region----------------------  Reactive 数据结构操作 Bean  ------------------------

    /**
     * Reactive Value 操作
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactiveValueOperations<String, Object> reactiveValueOperations(
        ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        return reactiveRedisTemplate.opsForValue();
    }

    /**
     * Reactive Hash 操作
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactiveHashOperations<String, String, Object> reactiveHashOperations(
        ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        return reactiveRedisTemplate.opsForHash();
    }

    /**
     * Reactive List 操作
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactiveListOperations<String, Object> reactiveListOperations(
        ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        return reactiveRedisTemplate.opsForList();
    }

    /**
     * Reactive Set 操作
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactiveSetOperations<String, Object> reactiveSetOperations(
        ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        return reactiveRedisTemplate.opsForSet();
    }

    /**
     * Reactive ZSet 操作
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactiveZSetOperations<String, Object> reactiveZSetOperations(
        ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        return reactiveRedisTemplate.opsForZSet();
    }

    // endregion

    // region----------------------  ReactiveRedisUtil 初始化  ------------------------

    @Bean
    public ReactiveRedisUtil reactiveRedisUtilInitializer(
        ReactiveRedisTemplate<String, Object> reactiveRedisTemplate,
        ReactiveValueOperations<String, Object> reactiveValueOperations,
        ReactiveHashOperations<String, String, Object> reactiveHashOperations,
        ReactiveListOperations<String, Object> reactiveListOperations,
        ReactiveSetOperations<String, Object> reactiveSetOperations,
        ReactiveZSetOperations<String, Object> reactiveZSetOperations) {
        ReactiveRedisUtil.init(reactiveRedisTemplate, reactiveValueOperations, reactiveHashOperations,
            reactiveListOperations, reactiveSetOperations, reactiveZSetOperations, lettuceConnectionFactory);
        return new ReactiveRedisUtil();
    }

    // endregion
}
