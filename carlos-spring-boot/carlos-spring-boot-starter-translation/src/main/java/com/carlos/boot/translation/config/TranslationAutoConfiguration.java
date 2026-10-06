package com.carlos.boot.translation.config;

import com.carlos.boot.translation.aop.TranslationAspect;
import com.carlos.boot.translation.cache.RedissonTranslationCacheManager;
import com.carlos.boot.translation.cache.TranslationCache;
import com.carlos.boot.translation.cache.TranslationCacheManager;
import com.carlos.boot.translation.service.CachedTranslationService;
import com.carlos.boot.translation.service.TranslationService;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * 翻译模块自动配置
 * </p>
 *
 * @author carlos
 * @date 2025/01/20
 */
@AutoConfiguration
@EnableConfigurationProperties(TranslationProperties.class)
@ConditionalOnProperty(prefix = "carlos.translation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TranslationAutoConfiguration {

    /**
     * 缓存管理器（Caffeine 回退版本）
     * <p>
     * 当 RedissonClient 不存在时提供基础缓存能力。
     * 若 Redisson 可用，{@link RedissonTranslationConfig} 中定义的 Bean 会覆盖此实例。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public TranslationCache translationCacheManager(TranslationProperties properties) {
        return new TranslationCacheManager(properties);
    }

    /**
     * Redisson 多级缓存配置
     * <p>
     * 仅在 classpath 中存在 RedissonClient 时启用，提供更高效的 L1+L2 自动缓存同步。
     * </p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedissonClient.class)
    static class RedissonTranslationConfig {

        @Bean
        @ConditionalOnMissingBean
        public RedissonTranslationCacheManager redissonTranslationCacheManager(
            TranslationProperties properties,
            ObjectProvider<RedissonClient> redissonClientProvider) {
            RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
            return new RedissonTranslationCacheManager(properties, redissonClient);
        }
    }

    /**
     * 翻译服务
     *
     * @return TranslationService
     */
    @Bean
    @ConditionalOnMissingBean
    public TranslationService translationService(TranslationCache cacheManager) {
        return new CachedTranslationService(cacheManager);
    }

    /**
     * 翻译切面
     *
     * @param translationService 翻译服务
     * @return TranslationAspect
     */
    @Bean
    @ConditionalOnMissingBean
    public TranslationAspect translationAspect(TranslationService translationService) {
        return new TranslationAspect(translationService);
    }
}
