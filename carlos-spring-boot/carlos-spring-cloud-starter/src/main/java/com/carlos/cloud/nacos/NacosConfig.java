package com.carlos.cloud.nacos;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.carlos.cloud.health.ServiceHealthIndicator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Map;

/**
 * <p>
 * Nacos 扩展配置
 * </p>
 *
 * <p>
 * 提供 Nacos 服务注册发现、配置中心的增强功能，包括：
 * <ul>
 *   <li>服务元数据自动注册</li>
 *   <li>服务实例变更监听</li>
 *   <li>自定义心跳配置</li>
 * </ul>
 * </p>
 *
 * @author carlos
 * @date 2021/12/7 15:04
 */
@Slf4j
@AutoConfiguration
@AllArgsConstructor
@EnableConfigurationProperties(NacosCloudProperties.class)
@ConditionalOnClass(DiscoveryClient.class)
public class NacosConfig {

    private final NacosCloudProperties nacosCloudProperties;
    private final Environment environment;

    /**
     * 服务注册监听器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "carlos.cloud.nacos", name = "enabled", havingValue = "true", matchIfMissing = true)
    public NacosServiceRegistryListener nacosServiceRegistryListener(NacosRegistration nacosRegistration,
                                                                     Environment environment) {
        log.info("初始化 Nacos 服务注册监听器");
        return new NacosServiceRegistryListener(nacosRegistration, environment);
    }

    /**
     * 服务实例变更监听器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "carlos.cloud.nacos.subscription", name = "enabled", havingValue = "true", matchIfMissing = true)
    public NacosServiceInstanceListener nacosServiceInstanceListener(NacosServiceManager nacosServiceManager,
                                                                     NacosDiscoveryProperties nacosDiscoveryProperties,
                                                                     NacosCloudProperties nacosCloudProperties) {
        log.info("初始化 Nacos 服务实例变更监听器");
        return new NacosServiceInstanceListener(nacosServiceManager, nacosDiscoveryProperties, nacosCloudProperties);
    }

    /**
     * 初始化 Nacos 元数据
     */
    public void initNacosMetadata(NacosRegistration registration) {
        if (registration == null) {
            return;
        }

        Map<String, String> metadata = registration.getMetadata();

        // 添加版本号
        metadata.put("version", nacosCloudProperties.getVersion());

        // 添加区域
        metadata.put("region", nacosCloudProperties.getRegion());

        // 添加权重
        metadata.put("weight", String.valueOf(nacosCloudProperties.getWeight()));

        // 添加 Spring Profiles
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length > 0) {
            metadata.put("profiles", String.join(",", profiles));
        }

        // 添加自定义元数据
        if (nacosCloudProperties.getMetadata() != null) {
            metadata.putAll(nacosCloudProperties.getMetadata());
        }

        log.info("Nacos 元数据初始化完成: {}", metadata);
    }

    /**
     * 服务健康检查指示器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(NacosServiceDiscovery.class)
    @ConditionalOnProperty(prefix = "carlos.cloud.health", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ServiceHealthIndicator serviceHealthIndicator(NacosServiceDiscovery nacosServiceDiscovery) {
        log.info("初始化服务健康检查指示器");
        return new ServiceHealthIndicator(nacosServiceDiscovery);
    }
}
