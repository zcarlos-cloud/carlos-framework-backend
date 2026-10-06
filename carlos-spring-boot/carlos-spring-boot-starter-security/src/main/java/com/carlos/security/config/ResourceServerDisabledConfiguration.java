package com.carlos.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * <p>
 * 资源服务器禁用时的安全配置
 * </p>
 *
 * <p>当 {@code carlos.resource-server.enabled=false} 或未配置时，提供 permit-all
 * 的 SecurityFilterChain，防止 Spring Boot 默认安全配置（表单登录重定向到 /login）接管。</p>
 *
 * <p>本配置与其他安全配置互斥：</p>
 * <ul>
 *   <li>{@code carlos.resource-server.enabled=true} → {@link ResourceServerAutoConfiguration} 激活，提供完整的安全过滤器链</li>
 *   <li>存在 {@code authorizationServerSecurityFilterChain} Bean（如授权服务器） → 本配置不激活</li>
 *   <li>{@code carlos.resource-server.enabled=false} 且无其他安全配置 → 本配置激活，放行所有请求</li>
 * </ul>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-06-16
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "carlos.resource-server", name = "enabled", havingValue = "false", matchIfMissing = true)
@ConditionalOnMissingBean(name = "authorizationServerSecurityFilterChain")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ResourceServerDisabledConfiguration {

    /**
     * 禁用模式的安全过滤器链
     * <p>放行所有请求，不使用任何认证机制。</p>
     */
    @Bean
    @Order(100)
    @ConditionalOnMissingBean(name = "resourceServerSecurityFilterChain")
    public SecurityFilterChain disabledSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF
            .csrf(AbstractHttpConfigurer::disable)

            // 无状态会话
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 放行所有请求
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        log.info("SecurityFilterChain configured for disabled resource server (permit-all)");
        return http.build();
    }
}
