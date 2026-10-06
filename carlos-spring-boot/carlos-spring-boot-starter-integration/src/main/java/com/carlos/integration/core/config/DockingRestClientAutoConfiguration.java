package com.carlos.integration.core.config;

import com.carlos.integration.core.client.DockingClientRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Map;

/**
 * <p>
 * RestClient 对接自动配置
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@EnableConfigurationProperties(DockingRestClientProperties.class)
@ConditionalOnProperty(prefix = "carlos.integration.restclient", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(DockingClientScanner.class)
public class DockingRestClientAutoConfiguration {

    private final DockingRestClientProperties properties;
    private final BeanFactory beanFactory;

    /**
     * 创建客户端注册中
     */
    @Bean
    public DockingClientRegistry dockingClientRegistry() {
        return new DockingClientRegistry(properties, beanFactory);
    }

    @PostConstruct
    public void init() {
        log.info("Docking RestClient module initialized. Clients configured: {}",
            properties.getClients().size());
    }
}

/**
 * 客户端扫描和注册
 */
@Slf4j
@RequiredArgsConstructor
class DockingClientScanner {

    private final DockingRestClientProperties properties;
    private final DockingClientRegistry registry;

    @PostConstruct
    public void init() {
        // 扫描配置中定义的客户端并注册
        for (Map.Entry<String, DockingRestClientProperties.ClientConfig> entry :
            properties.getClients().entrySet()) {
            String clientName = entry.getKey();
            DockingRestClientProperties.ClientConfig config = entry.getValue();

            if (!config.isEnabled()) {
                log.info("Skipping disabled client: {}", clientName);
                continue;
            }

            try {
                registerClient(clientName, config);
            } catch (Exception e) {
                log.error("Failed to register client: {}", clientName, e);
            }
        }
    }

    private void registerClient(String clientName, DockingRestClientProperties.ClientConfig config) {
        switch (config.getType()) {
            case "predefined" -> registerPredefinedClient(clientName, config);
            case "dynamic" -> registerDynamicClient(clientName, config);
            default -> throw new IllegalArgumentException("Unknown client type: " + config.getType());
        }
    }

    private void registerPredefinedClient(String clientName,
                                          DockingRestClientProperties.ClientConfig config) {
        try {
            String className = config.getInterfaceClass();
            if (className == null || className.isEmpty()) {
                throw new IllegalArgumentException("Interface class is required for predefined client");
            }

            Class<?> clazz = Class.forName(className);
            registry.registerPredefinedClient(clientName, clazz, config);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Interface class not found: " + config.getInterfaceClass(), e);
        }
    }

    private void registerDynamicClient(String clientName,
                                       DockingRestClientProperties.ClientConfig config) {
        registry.registerDynamicClient(clientName, config);
    }
}
