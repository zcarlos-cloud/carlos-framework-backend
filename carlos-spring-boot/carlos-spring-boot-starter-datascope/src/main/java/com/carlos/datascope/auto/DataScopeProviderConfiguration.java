package com.carlos.datascope.auto;

import com.carlos.datascope.provider.DataScopeProvider;
import com.carlos.datascope.provider.DefaultDataScopeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据权限提供器配置
 * <p>
 * 配置数据权限数据提供器
 *
 * @author Carlos
 * @version 2.0
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class DataScopeProviderConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataScopeProvider dataScopeProvider() {
        log.warn("Using DefaultDataScopeProvider, please implement your own DataScopeProvider for production");
        return new DefaultDataScopeProvider();
    }
}
