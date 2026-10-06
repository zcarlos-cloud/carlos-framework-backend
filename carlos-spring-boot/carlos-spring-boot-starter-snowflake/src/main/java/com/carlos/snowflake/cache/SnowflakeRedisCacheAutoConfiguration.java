package com.carlos.snowflake.cache;

import com.carlos.snowflake.SnowflakeProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;


/**
 * <p>
 * Redis 缓存自动配置（有 Redis 环境时使用）
 * </p>
 * <p>
 * 当类路径中存在 RedisUtil 时，使用 Redis 缓存管理器实现分布式 workerId 分配
 * </p>
 *
 * @author Carlos
 * @date 2025-03-17 14:09
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.carlos.redis.util.RedisUtil")
public class SnowflakeRedisCacheAutoConfiguration {
    /**
     * <p>
     * Redis 缓存管理器
     * </p>
     *
     * @author Carlos
     * @date 2025-03-17 14:0
     */
    @Bean
    @ConditionalOnMissingBean
    public SnowflakeCacheManager snowflakeCacheManager(SnowflakeProperties properties) {
        return new SnowflakeRedisCacheManager(properties);
    }

}


