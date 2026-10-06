package com.carlos.cloud.health;

import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import java.util.List;

/**
 * <p>
 * 微服务健康检查指示器
 * </p>
 *
 * <p>
 * 用于 Spring Boot Actuator 健康检查端点，检查：
 * <ul>
 *   <li>Nacos 服务连接状态</li>
 *   <li>服务注册状态</li>
 * </ul>
 * </p>
 *
 * @author carlos
 * @date 2024/01/15
 */
@Slf4j
@RequiredArgsConstructor
@ConditionalOnClass(NacosServiceDiscovery.class)
public class ServiceHealthIndicator implements HealthIndicator {

    private final NacosServiceDiscovery nacosServiceDiscovery;

    @Override
    public Health health() {
        if (nacosServiceDiscovery == null) {
            return Health.up()
                .withDetail("nacos", "未启用 Nacos 服务发现")
                .build();
        }

        try {
            // 尝试获取服务列表，验证 Nacos 连接
            List<String> services = nacosServiceDiscovery.getServices();
            return Health.up()
                .withDetail("nacos", "连接正常")
                .withDetail("services.count", services.size())
                .build();
        } catch (Exception e) {
            log.error("Nacos 健康检查失败", e);
            return Health.down()
                .withDetail("nacos", "连接异常")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
