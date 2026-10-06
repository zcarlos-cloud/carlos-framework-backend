package com.carlos.auth.oauth2.config;

import com.carlos.auth.audit.AuditLogPublisher;
import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.oauth2.UnauthorizedEntryPoint;
import com.carlos.auth.security.ext.ExtendAuthenticationConverter;
import com.carlos.auth.security.ext.ExtendAuthenticationProvider;
import com.carlos.auth.security.ext.handler.GrantTypeHandler;
import com.carlos.auth.security.filter.OpaqueTokenAuthenticationFilter;
import com.carlos.auth.security.filter.PasswordDecryptFilter;
import com.carlos.auth.security.handle.FormLoginFailureHandler;
import com.carlos.auth.security.handle.FormLoginSuccessHandler;
import com.carlos.auth.util.AuditLogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

import java.util.List;

/**
 * OAuth2 授权服务器安全配置
 *
 * <p>本配置类专注于授权服务器的安全过滤器链配置，包括：</p>
 * <ul>
 *   <li>授权服务器端点过滤器链（/oauth2/**、/.well-known/** 等）</li>
 *   <li>默认安全过滤器链（其他所有请求）</li>
 * </ul>
 *
 * <p>其他 OAuth2 相关 Bean 已拆分到以下配置类：</p>
 * <ul>
 *   <li>{@link OAuth2JwkConfig} - JWT 密钥与编解码</li>
 *   <li>{@link OAuth2TokenConfig} - Token 生成与定制</li>
 *   <li>{@link OAuth2AuthorizationServiceConfig} - 授权服务与确认服务</li>
 *   <li>{@link OAuth2PasswordEncoderConfig} - 密码编码器</li>
 *   <li>{@link OAuth2AuthenticationConfig} - 认证管理器与用户服务</li>
 * </ul>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-02-22
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(OAuth2Properties.class)
@ConditionalOnProperty(
        prefix = "carlos.auth.authorization-server",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OAuth2AuthorizationServerConfig {

    /**
     * OAuth2 配置属性
     */
    private final OAuth2Properties oauth2Properties;

    /**
     * RequestCache（用于 SSO 流程中保存原始 /oauth2/authorize 请求）
     */
    @Bean
    public RequestCache requestCache() {
        return new HttpSessionRequestCache();
    }

    /**
     * 表单登录成功处理器（SSO 链路专用）
     *
     * <p>认证成功后从 RequestCache 恢复原始 /oauth2/authorize 请求并重定向。</p>
     */
    @Bean
    public FormLoginSuccessHandler formLoginSuccessHandler(AuditLogUtil auditLogUtil,
                                                           AuditLogPublisher auditLogPublisher,
                                                           RequestCache requestCache) {
        return new FormLoginSuccessHandler(auditLogUtil, auditLogPublisher, requestCache);
    }

    /**
     * 授权服务器安全过滤器链
     *
     * <p>配置授权服务器的所有端点，包括：</p>
     * <ul>
     *   <li>/oauth2/authorize - 授权端点</li>
     *   <li>/oauth2/token - 令牌端点</li>
     *   <li>/oauth2/introspect - 令牌自省端点</li>
     *   <li>/oauth2/revoke - 令牌撤销端点</li>
     *   <li>/oauth2/jwks - JWK Set 端点</li>
     *   <li>/userinfo - 用户信息端点（OIDC）</li>
     * </ul>
     *
     * <p><strong>优先级：</strong>最高优先级（Ordered.HIGHEST_PRECEDENCE），
     * 确保授权服务器端点在其他过滤器之前处理。</p>
     *
     * <p><strong>安全说明（CSRF - P0-008 修复）：</strong>
     * 启用 CSRF 保护，但忽略纯 API 端点：</p>
     * <ul>
     *   <li>/oauth2/token、/oauth2/introspect、/oauth2/revoke 为机器间 API 调用，忽略 CSRF</li>
     *   <li>/oauth2/authorize（含 consent 表单提交）启用 CSRF 保护</li>
     *   <li>使用 CookieCsrfTokenRepository 支持 AJAX 前端获取 Token</li>
     * </ul>
     *
     * @param http HttpSecurity 配置构建器
     * @param grantTypeHandlers 授权类型参数处理器列表
     * @param extendAuthenticationProvider 扩展认证提供者
     * @return SecurityFilterChain 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            List<GrantTypeHandler> grantTypeHandlers,
            ExtendAuthenticationProvider extendAuthenticationProvider) throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                // 关键：要求所有授权服务器端点的请求都必须已认证
                // 未认证时 ExceptionTranslationFilter 会触发 AuthenticationEntryPoint 跳转登录页
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                // P0-008 修复：启用 CSRF，但忽略 API 端点
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                "/oauth2/token",
                                "/oauth2/introspect",
                                "/oauth2/revoke",
                                "/oauth2/jwks",
                                "/userinfo"
                        )
                )
                .with(authorizationServerConfigurer, (configurer) -> {
                    configurer
                            .oidc(Customizer.withDefaults())
                            .authorizationEndpoint(authorizationEndpoint ->
                                    authorizationEndpoint.consentPage("/oauth2/consent")
                            )
                            .tokenEndpoint(tokenEndpoint ->
                                    tokenEndpoint
                                            // 不允许注册为 Bean：Spring Security 7 的资源服务器配置器
                                            // 会探测容器中任意 AuthenticationConverter Bean 给 Bearer Token
                                            // 认证用，本转换器返回 null 会导致 JWT 请求一律匿名 401
                                            .accessTokenRequestConverter(new ExtendAuthenticationConverter(grantTypeHandlers))
                                            .authenticationProvider(extendAuthenticationProvider)
                            );
                })
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(new UnauthorizedEntryPoint())
                );

        log.info("OAuth2 Authorization Server configured with endpoints:");
        log.info("  - Authorization endpoint: {}", oauth2Properties.getAuthorizationServer().getAuthorizationEndpoint());
        log.info("  - Token endpoint: {}", oauth2Properties.getAuthorizationServer().getTokenEndpoint());
        log.info("  - JWK Set endpoint: {}", oauth2Properties.getAuthorizationServer().getJwkSetEndpoint());

        return http.build();
    }

    /**
     * 默认安全过滤器链
     *
     * <p>处理非授权服务器端点的请求。</p>
     *
     * <p><strong>安全说明（CSRF - P0-008 修复）：</strong>
     * 启用 CSRF 保护，但忽略 API 端点：
     * </p>
     * <ul>
     *   <li>/login、/oauth2/consent 等浏览器表单端点启用 CSRF 保护</li>
     *   <li>/auth/user/login、/auth/user/pre-check、/auth/captcha/** 等 AJAX API 端点忽略 CSRF</li>
     *   <li>使用 CookieCsrfTokenRepository 支持前端 AJAX 获取 Token</li>
     * </ul>
     *
     * @param http HttpSecurity 配置构建器
     * @return SecurityFilterChain 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          FormLoginSuccessHandler formLoginSuccessHandler,
                                                          FormLoginFailureHandler formLoginFailureHandler,
                                                          OAuth2AuthorizationService authorizationService) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login",
                                "/error",
                                "/oauth2/consent",
                                "/actuator/**",
                                "/assets/**",
                                "/webjars/**",
                                "/swagger-ui/**",
                                "/doc.html",
                                "/v3/api-docs/**",
                                "/auth/user/refreshToken",
                                "/auth/user/login",
                                "/auth/user/pre-check",
                                "/auth/user/logout",
                                "/auth/public-key",
                                "/auth/mfa/step-up-verify",
                                "/auth/captcha/**",
                                "/auth/qrcode/**",
                                "/auth/passkey/authenticate/**",
                                "/api/auth/password/encode"
                        ).permitAll()
                        // 应用门户 API 需要 Bearer Token 认证（由 oauth2ResourceServer 验证）
                        .requestMatchers("/auth/app/portal/**").authenticated()
                        // 应用管理 API 需要 Bearer Token 认证（P0 修复：原 permitAll 已移除）
                        .requestMatchers("/auth/app/client/**").authenticated()
                        .requestMatchers("/auth/app/category/**").authenticated()
                        .requestMatchers("/api/auth/password/match").authenticated()
                        .anyRequest().authenticated()
                )
                // P0-008 修复：启用 CSRF，忽略 API 端点
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                "/auth/user/refreshToken",
                                "/auth/user/login",
                                "/auth/user/pre-check",
                                "/auth/user/logout",
                                "/auth/public-key",
                                "/auth/mfa/step-up-verify",
                                "/auth/captcha/**",
                                "/auth/app/portal/**",
                                // 客户端/分类管理是 Bearer Token 认证的 AJAX 管理接口，不适用 CSRF
                                "/auth/app/client/**",
                                "/auth/app/category/**",
                                "/auth/qrcode/**",
                                "/auth/passkey/authenticate/**",
                                "/auth/password/encode",
                                "/auth/password/match",
                                "/api/auth/password/encode",
                                "/api/auth/password/match"
                        )
                )
                // OAuth2 Resource Server：验证 Bearer Token（门户 API 等使用）
                // 自定义 BearerTokenResolver：不透明 Token 返回 null，跳过 BearerTokenAuthenticationFilter
                // 的 JWT 认证，避免覆盖 OpaqueTokenAuthenticationFilter 已建立的 SecurityContext 而 401
                .oauth2ResourceServer(resourceServer ->
                        resourceServer
                                .bearerTokenResolver(jwtOnlyBearerTokenResolver())
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                // 统一登录体系：启用表单登录，支持 SSO
                // GET /login 渲染登录页，POST /login 由 UsernamePasswordAuthenticationFilter 处理
                // 认证成功后种下 Session Cookie，并重定向回 /oauth2/authorize 继续授权流程
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(formLoginSuccessHandler)
                        .failureHandler(formLoginFailureHandler)
                )
                // 密码传输解密过滤器：必须在 UsernamePasswordAuthenticationFilter 之前执行
                .addFilterBefore(new PasswordDecryptFilter(), UsernamePasswordAuthenticationFilter.class)
                // Opaque Token 认证过滤器：在 BearerTokenAuthenticationFilter 之前解析不透明 Token
                .addFilterBefore(new OpaqueTokenAuthenticationFilter(authorizationService),
                        BearerTokenAuthenticationFilter.class)
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(new UnauthorizedEntryPoint())
                );

        log.info("Default SecurityFilterChain configured with formLogin (SSO enabled)");

        return http.build();
    }

    /**
     * 仅放行 JWT 的 Bearer Token 解析器
     *
     * <p>密码登录等自定义授权类型签发的是不透明 Token（不含 '.'），
     * 由 {@link OpaqueTokenAuthenticationFilter} 先行认证。若此处照样解析，
     * 下游 BearerTokenAuthenticationFilter 会按 JWT 解码失败并清空 SecurityContext 返回 401。
     * 因此对不透明 Token 返回 null，使 BearerTokenAuthenticationFilter 直接放行。</p>
     */
    private BearerTokenResolver jwtOnlyBearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        return request -> {
            String token = delegate.resolve(request);
            if (token != null && !token.contains(".")) {
                return null;
            }
            return token;
        };
    }

    /**
     * JWT 认证转换器
     *
     * <p>从 JWT Token 中提取 authorities 声明作为 GrantedAuthority，
     * 使得 PortalController 等可以通过 SecurityContextHolder 获取用户角色。</p>
     *
     * <p>Token 中 authorities 声明格式为逗号分隔的角色字符串，
     * 例如：ROLE_ADMIN,ROLE_USER</p>
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = new java.util.ArrayList<org.springframework.security.core.GrantedAuthority>();
            // 从 authorities 声明中提取角色
            String authoritiesClaim = jwt.getClaimAsString("authorities");
            if (authoritiesClaim != null && !authoritiesClaim.isEmpty()) {
                for (String authority : authoritiesClaim.split(",")) {
                    String trimmed = authority.trim();
                    if (!trimmed.isEmpty()) {
                        authorities.add(new SimpleGrantedAuthority(trimmed));
                    }
                }
            }
            // 同时保留 scope 声明
            var scopes = jwt.getClaimAsStringList("scope");
            if (scopes != null) {
                for (String scope : scopes) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
                }
            }
            return authorities;
        });
        return converter;
    }
}
