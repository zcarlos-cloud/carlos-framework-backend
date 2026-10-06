package com.carlos.boot.web.autoconfigure;

import com.carlos.boot.web.config.WebProperties;
import com.carlos.boot.web.resource.DefaultResourceStore;
import com.carlos.boot.web.resource.ResourceScanService;
import com.carlos.boot.web.resource.ResourceStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 资源扫描自动配置。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class ResourceScanAutoConfiguration {

    private final WebProperties webProperties;

    @Bean
    @ConditionalOnMissingBean(ResourceStore.class)
    public ResourceStore resourceStore() {
        return new DefaultResourceStore();
    }

    @Bean
    @ConditionalOnProperty(prefix = "carlos.web.resource-scan", name = "enabled", havingValue = "true")
    public ResourceScanService resourceScanService(ResourceStore resourceStore) {
        return new ResourceScanService(webProperties.getResourceScan(), resourceStore);
    }
}
