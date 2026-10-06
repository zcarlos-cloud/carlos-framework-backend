package com.carlos.auth.oauth2.config;

import com.carlos.auth.audit.AuditLogPublisher;
import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.provider.DefaultUserProvider;
import com.carlos.auth.provider.UserProvider;
import com.carlos.auth.risk.RiskEngine;
import com.carlos.auth.security.SecurityProperties;
import com.carlos.auth.security.TenantLoginChecker;
import com.carlos.auth.security.TenantProperties;
import com.carlos.auth.security.ext.ExtendAuthenticationProvider;
import com.carlos.auth.security.handle.CustomAuthenticationFailureHandler;
import com.carlos.auth.security.handle.CustomAuthenticationSuccessHandler;
import com.carlos.auth.security.manager.IpBlockManager;
import com.carlos.auth.security.manager.LoginAttemptManager;
import com.carlos.auth.security.password.PasswordProperties;
import com.carlos.auth.security.service.DefaultExtendUserDetailsService;
import com.carlos.auth.security.service.ExtendUserDetailsService;
import com.carlos.auth.util.AuditLogUtil;
import com.carlos.auth.util.IpLocationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 认证相关 Bean 配置
 *
 * <p>管理认证管理器、用户详情服务、登录处理器和扩展认证转换器/提供者。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({OAuth2Properties.class, SecurityProperties.class, TenantProperties.class})
@ConditionalOnProperty(
        prefix = "carlos.auth.authorization-server",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OAuth2AuthenticationConfig {

    /**
     * 表单登录成功处理器
     *
     * @param auditLogUtil    审计日志工具类
     * @param apiAuditLogMain 审计日志 Feign 接口
     * @return CustomAuthenticationSuccessHandler 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler(AuditLogUtil auditLogUtil,
                                                                                 AuditLogPublisher auditLogPublisher) {
        log.info("Configuring CustomAuthenticationSuccessHandler");
        return new CustomAuthenticationSuccessHandler(auditLogUtil, auditLogPublisher);
    }

    /**
     * 表单登录失败处理器
     *
     * @param auditLogUtil    审计日志工具类
     * @param apiAuditLogMain 审计日志 Feign 接口
     * @return CustomAuthenticationFailureHandler 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public CustomAuthenticationFailureHandler customAuthenticationFailureHandler(AuditLogUtil auditLogUtil,
                                                                                 AuditLogPublisher auditLogPublisher) {
        log.info("Configuring CustomAuthenticationFailureHandler");
        return new CustomAuthenticationFailureHandler(auditLogUtil, auditLogPublisher);
    }

    /**
     * 认证管理器
     *
     * <p>全局 AuthenticationManager，自动收集所有 AuthenticationProvider Bean
     *（包括 {@link com.carlos.auth.security.UsernamePasswordAuthenticationProvider} 和
     * {@link ExtendAuthenticationProvider}）。</p>
     *
     * @param authenticationConfiguration Spring Security 认证配置
     * @return AuthenticationManager 认证管理器
     * @throws Exception 配置异常
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        log.info("Configuring AuthenticationManager");
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 用户详情服务
     *
     * @param passwordEncoder 密码编码器
     * @param userProvider 用户信息提供者
     * @return ExtendUserDetailsService 用户详情服务
     */
    @Bean
    @ConditionalOnMissingBean
    public ExtendUserDetailsService extendUserDetailsService(PasswordEncoder passwordEncoder,
                                                             UserProvider userProvider) {
        log.info("Configuring default ExtendUserDetailsService (for development only)");
        return new DefaultExtendUserDetailsService(passwordEncoder, userProvider);
    }

    /**
     * 用户信息提供者
     *
     * @param passwordEncoder 密码编码器
     * @return UserProvider 用户信息提供者
     */
    @Bean
    @ConditionalOnMissingBean
    public UserProvider userProvider(PasswordEncoder passwordEncoder) {
        log.info("Configuring default UserProvider (for development only)");
        return new DefaultUserProvider(passwordEncoder);
    }

    /**
     * 扩展认证提供者
     *
     * <p>统一登录体系增强：注入风控组件，让 /oauth2/token 端点也受风控保护。</p>
     *
     * @param providerRegistry    身份源注册中心
     * @param userDetailsService  用户详情服务
     * @param loginAttemptManager 登录尝试管理器
     * @param ipBlockManager      IP 封禁管理器
     * @param riskEngine          风控引擎
     * @param ipLocationUtil      IP 定位工具
     * @param passwordProperties  密码策略配置（过期天数等）
     * @return ExtendAuthenticationProvider 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ExtendAuthenticationProvider extendAuthenticationProvider(
            com.carlos.auth.idp.IdentityProviderRegistry providerRegistry,
            ExtendUserDetailsService userDetailsService,
            LoginAttemptManager loginAttemptManager,
            IpBlockManager ipBlockManager,
            RiskEngine riskEngine,
            IpLocationUtil ipLocationUtil,
            PasswordProperties passwordProperties,
            TenantLoginChecker tenantLoginChecker) {
        log.info("Configuring ExtendAuthenticationProvider with risk engine integration");
        return new ExtendAuthenticationProvider(providerRegistry, userDetailsService,
                loginAttemptManager, ipBlockManager, riskEngine, ipLocationUtil, passwordProperties,
                tenantLoginChecker);
    }
}
