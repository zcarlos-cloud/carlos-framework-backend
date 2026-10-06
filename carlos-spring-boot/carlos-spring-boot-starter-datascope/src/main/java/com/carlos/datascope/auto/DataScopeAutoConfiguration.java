package com.carlos.datascope.auto;

import com.carlos.datascope.audit.DataScopeAuditLogger;
import com.carlos.datascope.cache.CaffeineDataScopeCache;
import com.carlos.datascope.cache.DataScopeCache;
import com.carlos.datascope.cache.RedisDataScopeCache;
import com.carlos.datascope.cache.RedissonLocalDataScopeCache;
import com.carlos.datascope.core.aspect.DataScopeAspect;
import com.carlos.datascope.core.engine.DataScopeRuleEngine;
import com.carlos.datascope.core.engine.DefaultRuleEngine;
import com.carlos.datascope.expression.ExpressionEngine;
import com.carlos.datascope.expression.SpelExpressionEngine;
import com.carlos.datascope.interceptor.MyBatisDataScopeInterceptor;
import com.carlos.datascope.properties.DataScopeProperties;
import com.carlos.datascope.provider.DataScopeProvider;
import com.carlos.datascope.repository.RuleRepository;
import com.carlos.datascope.repository.YamlRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.Interceptor;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 数据权限自动配置
 * <p>
 * 自动装配数据权限组件
 *
 * @author Carlos
 * @version 2.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(DataScopeProperties.class)
@ConditionalOnProperty(prefix = "carlos.datascope", name = "enabled", havingValue = "true")
@Import({DataScopeProviderConfiguration.class})
@RequiredArgsConstructor
public class DataScopeAutoConfiguration {

    private final DataScopeProperties properties;

    /**
     * 数据权限缓存
     *
     * <p>优先级：Redisson L1+L2 > Redis 分布式 > Caffeine 本地</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public DataScopeCache dataScopeCache(
        ObjectProvider<StringRedisTemplate> redisTemplateProvider,
        ObjectProvider<RedissonClient> redissonClientProvider) {

        // 1. 优先：Redisson RLocalCachedMap（L1 本地 + L2 Redis，自动一致性）
        RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
        if (redissonClient != null) {
            log.info("Using Redisson local cached map data scope cache");
            return new RedissonLocalDataScopeCache(redissonClient,
                properties.getCache().getRedis().getTtl().toMinutes());
        }

        // 2. 次选：Redis 分布式缓存
        if (properties.getCache().getRedis().isEnabled()) {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                log.info("Using Redis data scope cache");
                return new RedisDataScopeCache(redisTemplate,
                    properties.getCache().getRedis().getTtl().toMinutes());
            }
            log.warn("Redis cache enabled but StringRedisTemplate not available, fallback to Caffeine");
        }

        // 3. 兜底：Caffeine 本地缓存
        log.info("Using Caffeine data scope cache");
        return new CaffeineDataScopeCache(
            properties.getCache().getLocal().getMaxSize(),
            properties.getCache().getLocal().getTtl().toMinutes()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ExpressionEngine expressionEngine() {
        return new SpelExpressionEngine();
    }

    @Bean
    @ConditionalOnMissingBean
    public RuleRepository ruleRepository() {
        return new YamlRuleRepository(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataScopeAuditLogger dataScopeAuditLogger() {
        return new DataScopeAuditLogger();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataScopeRuleEngine dataScopeRuleEngine(
        RuleRepository ruleRepository,
        @Lazy DataScopeProvider dataProvider,
        DataScopeCache cache,
        ExpressionEngine expressionEngine) {
        log.info("Initializing DataScopeRuleEngine");
        return new DefaultRuleEngine(ruleRepository, dataProvider, cache, expressionEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataScopeAspect dataScopeAspect(
        DataScopeRuleEngine ruleEngine,
        DataScopeAuditLogger auditLogger) {
        log.info("Initializing DataScopeAspect");
        return new DataScopeAspect(ruleEngine, auditLogger);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(Interceptor.class)
    public MyBatisDataScopeInterceptor myBatisDataScopeInterceptor() {
        log.info("Initializing MyBatisDataScopeInterceptor");
        return new MyBatisDataScopeInterceptor();
    }
}

