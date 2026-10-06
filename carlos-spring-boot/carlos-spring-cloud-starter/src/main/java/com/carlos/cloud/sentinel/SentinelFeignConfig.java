package com.carlos.cloud.sentinel;

import com.alibaba.cloud.sentinel.feign.SentinelFeign;
import feign.Feign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

/**
 * <p>
 * Sentinel Feign 集成配置
 * </p>
 *
 * <p>
 * 为 Feign 客户端添加 Sentinel 熔断降级支持。
 * 启用后，Feign 调用将被 Sentinel 保护，支持：
 * <ul>
 *   <li>服务调用熔断</li>
 *   <li>慢调用降级</li>
 *   <li>异常比例/数量熔断</li>
 *   <li>Fallback 处理</li>
 * </ul>
 * </p>
 *
 * @author carlos
 * @date 2024/01/15
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(SentinelFeign.class)
@ConditionalOnProperty(prefix = "spring.cloud.sentinel.feign", name = "enabled", havingValue = "true")
public class SentinelFeignConfig {

    /**
     * 创建 Sentinel 保护的 Feign Builder
     * 每个 Feign 客户端都有独立的熔断规则
     */
    @Bean
    @Scope("prototype")
    public Feign.Builder feignBuilder() {
        return SentinelFeign.builder();
    }
}
