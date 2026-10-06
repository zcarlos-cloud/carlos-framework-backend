package com.carlos.org.config;

import com.carlos.core.interfaces.ApplicationExtend;
import com.carlos.org.encoder.DefaultPasswordEncoderProvider;
import com.carlos.org.encoder.PasswordEncoderProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * <p>
 * 用户模块配置
 * </p>
 *
 * @author Carlos
 * @date 2023/10/31 21:53
 */
@Configuration
@EnableConfigurationProperties({OrgProperties.class})
public class OrgConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ApplicationExtend applicationExtend() {
        return new OrgApplicationExtendImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoderProvider passwordEncoderProvider() {
        return new DefaultPasswordEncoderProvider();
    }


}
