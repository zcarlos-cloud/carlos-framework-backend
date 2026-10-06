package com.carlos.datascope.conf;


import com.carlos.core.interfaces.ApplicationExtend;
import com.carlos.datascope.DataScopeAspect;
import com.carlos.datascope.DataScopeHandler;
import com.carlos.datascope.DataScopeProvider;
import com.carlos.datascope.DefaultDataScopeProvider;
import com.carlos.datascope.properties.DataScopeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;

/**
 * <p>
 * 数据权限 配置类
 * </p>
 *
 * @author Carlos
 * @date 2022/11/21 16:55
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({DataScopeProperties.class})
public class DataScopeConfig {

    @Bean
    @ConditionalOnMissingBean
    public DataScopeProvider dataScopeProvider(@Nullable @Lazy ApplicationExtend requestExtend) {
        return new DefaultDataScopeProvider(requestExtend);
    }


    @Bean
    @ConditionalOnMissingBean
    public DataScopeHandler dataScopeHandler(@Nullable @Lazy DataScopeProvider dataScopeProvider) {
        return new DefaultDataScopeHandler(dataScopeProvider);
    }

    @Bean
    public DataScopeAspect dataScopeAspect(DataScopeHandler dataScopeHandler) {
        return new DataScopeAspect(dataScopeHandler);
    }
}
