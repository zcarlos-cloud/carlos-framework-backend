package com.carlos.redis.delay;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.function.BiConsumer;

/**
 * 延时队列原语层自动配置
 * <p>
 * 注册位置：{@code carlos-spring-boot-starter-redis} 的
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}。
 * </p>
 * <p>
 * <b>本配置只装配、不启动</b>：延时队列的启动时机与「到期条目送哪里」的语义都属于上层，
 * 由持有 {@link DelayDueHandler} 的一方调用 {@link RedissonDelayedQueueManager#startAll()}。
 * 未提供处理器时启动会被拒绝并告警，避免「到期条目被静默丢弃」。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@EnableConfigurationProperties(RedisDelayedQueueProperties.class)
@ConditionalOnProperty(prefix = "carlos.redis.delay", name = "enabled",
    havingValue = "true", matchIfMissing = true)
public class RedisDelayAutoConfiguration {

    /**
     * 延时队列桶管理器
     * <p>
     * 处理器通过 {@link ObjectProvider} 延迟解析：上层模块（carlos-spring-boot-starter-mq）
     * 提供 {@link DelayDueHandler} bean 后即可生效，无需担心自动配置的先后顺序。
     * </p>
     */
    @Bean(destroyMethod = "stopAll")
    @ConditionalOnMissingBean
    public RedissonDelayedQueueManager redissonDelayedQueueManager(
        RedissonClient redissonClient,
        RedisDelayedQueueProperties properties,
        ObjectProvider<DelayDueHandler> dueHandlerProvider) {

        DelayDueHandler handler = dueHandlerProvider.getIfAvailable();
        BiConsumer<String, String> dueConsumer = handler == null ? null : handler::onDue;
        if (handler == null) {
            log.info("未发现 DelayDueHandler 实现，延时队列将保持空闲（不会启动，入队会显式报错）");
        }
        return new RedissonDelayedQueueManager(redissonClient, properties, dueConsumer);
    }
}
