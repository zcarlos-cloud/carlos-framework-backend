package com.carlos.sms.config;

import com.carlos.sms.SmsUtil;
import com.carlos.sms.postal.config.PostalFactory;
import com.carlos.sms.ratelimit.LocalSmsRateLimiter;
import com.carlos.sms.ratelimit.SmsRateLimiter;
import com.carlos.sms.template.DefaultSmsTemplateManager;
import com.carlos.sms.template.SmsTemplateManager;
import com.carlos.sms.ums.config.UmsFactory;
import com.carlos.sms.wocloud.config.WoCloudFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * <p>
 * 短信Factory配置
 * </p>
 *
 * @author Carlos
 * @date 2023/11/20 18:47
 */
@AutoConfiguration
@EnableConfigurationProperties(SmsProperties.class)
@RequiredArgsConstructor
public class SmsCustomizeConfig {

    private final SmsProperties smsProperties;

    /**
     * 邮政云短信注册
     */
    @Bean
    @ConditionalOnProperty(prefix = "sms.blends.postal", name = "supplier", havingValue = "postal")
    public PostalFactory postalFactory() {
        return PostalFactory.instance();
    }

    /**
     * 企信短信注册
     */
    @Bean
    @ConditionalOnProperty(prefix = "sms.blends.wocloud", name = "supplier", havingValue = "wocloud")
    public WoCloudFactory woCloudFactory() {
        return WoCloudFactory.instance();
    }

    /**
     * 一信通
     */
    @Bean
    @ConditionalOnProperty(prefix = "sms.blends.ums", name = "supplier", havingValue = "ums")
    public UmsFactory umsFactory() {
        return UmsFactory.instance();
    }

    /**
     * 注册短信模板管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public SmsTemplateManager smsTemplateManager() {
        return new DefaultSmsTemplateManager(smsProperties.getTemplates());
    }

    /**
     * 注册本地短信限流器
     */
    @Bean
    @ConditionalOnMissingBean
    public SmsRateLimiter smsRateLimiter() {
        return new LocalSmsRateLimiter(smsProperties.getRateLimit());
    }

    @Bean
    public SmsUtil smsUtil(SmsTemplateManager templateManager, SmsRateLimiter smsRateLimiter) {
        return new SmsUtil(templateManager, smsRateLimiter);
    }

}
