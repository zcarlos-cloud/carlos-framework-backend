package com.carlos.redis.config;

import com.carlos.redis.serialize.ConfigurableRedisSerializer;
import com.carlos.redis.serialize.RedisSerializerStrategy;
import com.carlos.redis.serialize.SerializerFactory;
import com.carlos.redis.serialize.SerializerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * <p>
 * Redis 核心自动配置类
 * </p>
 *
 * <p>
 * 提供序列化相关的公共 Bean，供同步和响应式两个 starter 共享。
 * 确保两个 starter 使用一致的序列化策略。
 * </p>
 *
 * @author carlos
 * @date 2026-04-09
 */
@Slf4j
@AutoConfiguration(before = DataRedisAutoConfiguration.class)
@EnableConfigurationProperties(CacheProperties.class)
public class RedisCoreAutoConfiguration {

    /**
     * 创建序列化策略
     * <p>
     * 根据配置创建对应的序列化策略（Jackson、Fastjson、Kryo、JDK）
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisSerializerStrategy redisSerializerStrategy(CacheProperties cacheProperties) {
        SerializerType type = cacheProperties.getSerializer();
        log.info("Initializing Redis serializer strategy: {} ({})", type.getCode(), type.getDescription());
        return SerializerFactory.getSerializer(type);
    }

    /**
     * 创建 Redis 序列化器
     * <p>
     * 基于策略模式的可配置序列化器，用于 value 的序列化/反序列化
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfigurableRedisSerializer configurableRedisSerializer(RedisSerializerStrategy strategy) {
        log.info("Initializing ConfigurableRedisSerializer with strategy: {}", strategy.getType());
        return new ConfigurableRedisSerializer(strategy);
    }
}
