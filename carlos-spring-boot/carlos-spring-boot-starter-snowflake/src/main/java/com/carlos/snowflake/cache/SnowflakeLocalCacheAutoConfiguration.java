package com.carlos.snowflake.cache;

import com.carlos.snowflake.SnowflakeProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;

/**
 * <p>
 * 本地缓存自动配置（无 Redis 环境时使用）
 * </p>
 * <p>
 * 当类路径中不存在 RedisUtil 时，使用本地缓存管理器
 * </p>
 *
 * @author Carlos
 * @date 2025-03-17 14:09
 */
@AutoConfiguration
@ConditionalOnMissingClass("com.carlos.redis.util.RedisUtil")
public class SnowflakeLocalCacheAutoConfiguration {

    /**
     * <p>
     * 本地缓存管理器
     * </p>
     *
     * @author Carlos
     * @date 2025-03-17 14:0
     */
    @Bean
    @ConditionalOnMissingBean
    public SnowflakeCacheManager snowflakeCacheManager(SnowflakeProperties properties) {
        return new SnowflakeLocalCacheManager(properties);
    }
}
