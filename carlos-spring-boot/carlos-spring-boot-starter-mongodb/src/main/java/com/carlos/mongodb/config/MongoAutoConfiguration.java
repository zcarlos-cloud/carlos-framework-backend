package com.carlos.mongodb.config;

import com.carlos.mongodb.health.MongoHealthIndicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * <p>
 * MongoDB 自动配置类
 * </p>
 *
 * <p><strong>mongo-plus 集成（2026-06-21）：</strong></p>
 * <ul>
 *   <li>mongo-plus 的自动配置（{@code MongoPlusAutoConfiguration}）已接管核心功能：<br>
 *       BaseMapper、MongoConverter、字段自动填充、逻辑删除、乐观锁、拦截器等</li>
 *   <li>本配置类仅注册 Carlos 特有的 Bean（如 {@link MongoHealthIndicator}）</li>
 *   <li>通过 {@code carlos.mongodb.enabled=true} 控制模块启用</li>
 * </ul>
 *
 * <p><strong>配置方式：</strong></p>
 * <pre>{@code
 * carlos:
 *   mongodb:
 *     enabled: true           # 启用 MongoDB 模块
 *     field-fill-enabled: true # 启用字段自动填充（通过 @CollectionField 注解）
 * }</pre>
 *
 * @author carlos
 * @date 2024/01/15
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(MongoTemplate.class)
@ConditionalOnProperty(prefix = "carlos.mongodb", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MongoProperties.class)
public class MongoAutoConfiguration {

    /**
     * MongoDB 健康检查指示器
     */
    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    public MongoHealthIndicator mongoHealthIndicator(MongoTemplate mongoTemplate) {
        return new MongoHealthIndicator(mongoTemplate);
    }
}
