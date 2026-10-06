package com.carlos.redis.config;

import com.carlos.redis.caffeine.CaffeineConfig;
import com.carlos.redis.localcache.RedissonLocalCacheManager;
import com.carlos.redis.lock.RedisLockAspect;
import com.carlos.redis.lock.RedisLockProperties;
import com.carlos.redis.lock.RedissonLockUtil;
import com.carlos.redis.multilevel.MultiLevelCacheUtil;
import com.carlos.core.interfaces.ApplicationExtend;
import com.carlos.redis.ratelimit.RateLimitAspect;
import com.carlos.redis.ratelimit.RateLimitProperties;
import com.carlos.redis.ratelimit.RateLimitUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RedissonClient;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Redisson配置
 * 提供RedissonConnectionFactory、分布式锁、限流和本地缓存支持
 *
 * @author carlos
 * @date 2026-02-01
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@EnableConfigurationProperties({RedisLockProperties.class, RateLimitProperties.class})
@Import({CaffeineConfig.class})
public class RedissonConfig {

    /**
     * 创建RedissonConnectionFactory
     * 使用Redisson作为Redis连接工厂
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redissonClient) {
        log.info("Initializing RedissonConnectionFactory");
        return new RedissonConnectionFactory(redissonClient);
    }

    /**
     * Redisson本地缓存管理器
     * 基于RLocalCachedMap实现自动L1+L2两级缓存
     */
    @Bean
    @ConditionalOnMissingBean
    public RedissonLocalCacheManager redissonLocalCacheManager(RedissonClient redissonClient) {
        RedissonLocalCacheManager manager = new RedissonLocalCacheManager(redissonClient);
        // 同时初始化 MultiLevelCacheUtil，使用相同配置
        LocalCachedMapOptions<String, Object> options = LocalCachedMapOptions.<String, Object>defaults()
            .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU)
            .cacheSize(10000)
            .timeToLive(5, TimeUnit.MINUTES)
            .maxIdle(5, TimeUnit.MINUTES)
            .storeMode(LocalCachedMapOptions.StoreMode.LOCALCACHE_REDIS)
            .syncStrategy(LocalCachedMapOptions.SyncStrategy.INVALIDATE)
            .reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.CLEAR);
        MultiLevelCacheUtil.init(redissonClient, options);
        return manager;
    }

    /**
     * 分布式锁切面
     */
    @Bean
    @ConditionalOnProperty(prefix = "carlos.redis.lock", name = "enabled", havingValue = "true")
    public RedisLockAspect redisLockAspect(RedissonClient redissonClient, RedisLockProperties lockProperties) {
        return new RedisLockAspect(redissonClient, lockProperties);
    }

    /**
     * 限流切面
     */
    @Bean
    @ConditionalOnProperty(prefix = "carlos.redis.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitAspect rateLimitAspect(RedissonClient redissonClient, RateLimitProperties properties,
                                          ObjectProvider<ApplicationExtend> applicationExtendProvider) {
        return new RateLimitAspect(redissonClient, properties, applicationExtendProvider);
    }

    /**
     * 初始化 RedissonLockUtil
     */
    @Bean
    public RedissonLockUtil redissonLockUtilInitializer(RedissonClient redissonClient) {
        RedissonLockUtil.init(redissonClient);
        return new RedissonLockUtil();
    }

    /**
     * 初始化 RateLimitUtil
     */
    @Bean
    public RateLimitUtil rateLimitUtilInitializer(RedissonClient redissonClient, RateLimitProperties properties) {
        RateLimitUtil.init(redissonClient, properties);
        return new RateLimitUtil();
    }
}
