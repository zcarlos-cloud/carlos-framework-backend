package com.carlos.auth.audit;

import com.carlos.audit.api.ApiAuditLogMain;
import com.carlos.mq.core.MqTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 审计日志投递的装配
 * <p>
 * 按本模块约定用 {@code @EnableConfigurationProperties} 注册配置类
 * （与 {@code OAuth2AuthenticationConfig} / {@code RiskEngine} 的写法一致），
 * 而不是在配置类上加 {@code @Component}。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Configuration
@EnableConfigurationProperties(AuditProperties.class)
public class AuditMqConfig {

    /**
     * 审计日志统一投递点
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditLogPublisher auditLogPublisher(AuditProperties auditProperties,
                                              ObjectProvider<MqTemplate> mqTemplateProvider,
                                              ApiAuditLogMain apiAuditLogMain) {
        return new AuditLogPublisher(auditProperties, mqTemplateProvider, apiAuditLogMain);
    }
}
