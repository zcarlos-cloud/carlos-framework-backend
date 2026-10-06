package com.carlos.system.config;


import com.carlos.core.interfaces.ApplicationExtend;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * 系统服务配置类
 * </p>
 *
 * @author Carlos
 * @date 2024/1/24 15:50
 */
@Configuration
public class SystemConfig {

    @Bean
    @ConditionalOnMissingBean
    public ApplicationExtend applicationExtend() {
        return new SystemApplicationExtendImpl();
    }

}
