package com.carlos.gateway.identity;

import com.carlos.core.auth.IdentityAssertionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * <p>
 * 身份断言自动配置
 * </p>
 *
 * <p>装配两个全局过滤器：</p>
 * <ul>
 *   <li>{@link IdentityHeaderSanitizeFilter} —— 无条件剔除客户端自带的身份头（无开关）</li>
 *   <li>{@link IdentityAssertionSignFilter} —— 认证后签发 {@code X-User-Context} 断言</li>
 * </ul>
 *
 * <p>两个过滤器都<b>始终注册</b>，「是否签发」在请求时读配置判定，因此 Nacos 改配置或单侧回滚
 * 都能即时生效，不依赖 Bean 重建。</p>
 *
 * <p>密钥与开关来自 {@link IdentityAssertionProperties}（{@code carlos.identity-assertion.*}），
 * 与全部下游服务共读 Nacos 共享配置 {@code common.yml}，从根上保证两侧配置一致。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-09-23
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(IdentityAssertionProperties.class)
public class IdentityGatewayAutoConfiguration {

    /**
     * 身份头净化过滤器（无开关，始终生效）
     */
    @Bean
    @ConditionalOnMissingBean(IdentityHeaderSanitizeFilter.class)
    public IdentityHeaderSanitizeFilter identityHeaderSanitizeFilter() {
        log.info("IdentityHeaderSanitizeFilter initialized: 客户端自带的身份头将被无条件剔除");
        return new IdentityHeaderSanitizeFilter();
    }

    /**
     * 身份断言签发过滤器
     */
    @Bean
    @ConditionalOnMissingBean(IdentityAssertionSignFilter.class)
    public IdentityAssertionSignFilter identityAssertionSignFilter(IdentityAssertionProperties properties) {
        log.info("IdentityAssertionSignFilter initialized: signEnabled={}, ttl={}, secretConfigured={}",
            properties.isSignEnabled(), properties.getTtl(), properties.hasSecret());
        return new IdentityAssertionSignFilter(properties);
    }

}
