package com.carlos.security.config;

import com.carlos.core.auth.IdentityAssertionProperties;
import com.carlos.core.interfaces.ApplicationExtend;
import com.carlos.redis.util.RedisUtil;
import com.carlos.security.auth.IdentityAssertionVerifier;
import com.carlos.security.permission.DataScopeProvider;
import com.carlos.security.permission.PermissionChecker;
import com.carlos.security.permission.PermissionProvider;
import com.carlos.security.permission.PermissionService;
import com.carlos.security.permission.cache.PermissionCacheSyncManager;
import com.carlos.security.permission.evaluator.CarlosPermissionEvaluator;
import com.carlos.security.permission.provider.CachedPermissionProvider;
import com.carlos.security.permission.provider.RedisPermissionProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Duration;
import java.util.List;

/**
 * <p>
 * 资源服务器自动配置
 * </p>
 *
 * <p>本配置类自动启用以下功能：</p>
 * <ol>
 *   <li><b>安全过滤器链</b> - 配置无状态会话和 X-User-Id 认证</li>
 *   <li><b>方法级安全</b> - 启用 @PreAuthorize/@PostAuthorize 注解</li>
 *   <li><b>权限提供者</b> - 自动配置 Redis + Caffeine 多层缓存</li>
 *   <li><b>缓存同步</b> - Redis Pub/Sub 实现多实例缓存同步</li>
 *   <li><b>权限评估器</b> - 支持 hasPermission() SpEL 表达式</li>
 * </ol>
 *
 * <h3>配置示例：</h3>
 * <pre>{@code
 * carlos:
 *   resource-server:
 *     enabled: true
 *     permission:
 *       cache:
 *         enabled: true
 *         local-ttl: 600
 *         max-size: 10000
 *       redis-ttl: 604800
 * }</pre>
 *
 * <h3>使用说明：</h3>
 * <p>网关认证后，通过 HTTP Header 传递用户信息（使用 {@link com.carlos.core.constant.HttpHeadersConstant} 中定义的常量）：</p>
 * <pre>
 * X-User-Id: 10001
 * X-User-Name: admin
 * X-User-Roles: 1,2,3
 * </pre>
 * <p>本过滤器提取这些信息构建 Spring Security 上下文。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({ResourceServerProperties.class, IdentityAssertionProperties.class})
@ConditionalOnProperty(prefix = "carlos.resource-server", name = "enabled", havingValue = "true")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({RedisUtil.class})
@RequiredArgsConstructor
public class ResourceServerAutoConfiguration {

    private final ResourceServerProperties properties;

    // ==================== 应用扩展点 ====================

    /**
     * 默认的 {@link ApplicationExtend} 实现（提供登录人上下文）
     * <p>
     * {@code ApplicationExtend.getUserContext()} 的接口默认实现恒返回 null，导致未自带实现的模块
     * （carlos-job / carlos-workflow / carlos-agent）在任何需要「当前登录人」的地方都拿不到身份 ——
     * {@code @Log} 审计日志会记成匿名。这里补一个可用的默认实现。
     * </p>
     * <p>
     * {@code @ConditionalOnMissingBean} 保证已自带实现的模块（carlos-system / carlos-org 等）
     * <b>不受影响</b>：它们的实现由组件扫描先注册，本兜底不生效。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(ApplicationExtend.class)
    public ApplicationExtend securityApplicationExtend() {
        log.info("注册默认 ApplicationExtend（SecurityApplicationExtend），提供登录人上下文");
        return new SecurityApplicationExtend();
    }

    /**
     * 方法级鉴权拒绝通知（@Bean 注册：starter 不参与宿主组件扫描）
     * <p>把 @PreAuthorize 抛出的 AccessDeniedException 归位为 403，而不是落进全局兜底的 500。</p>
     */
    @Bean
    @ConditionalOnMissingBean(AccessDeniedExceptionAdvice.class)
    public AccessDeniedExceptionAdvice accessDeniedExceptionAdvice() {
        return new AccessDeniedExceptionAdvice();
    }

    // ==================== Redis Pub/Sub 配置 ====================

    /**
     * Redis 消息监听器容器（用于 Pub/Sub）
     */
    @Bean
    @ConditionalOnMissingBean(RedisMessageListenerContainer.class)
    public RedisMessageListenerContainer redisMessageListenerContainer(
        RedisConnectionFactory connectionFactory,
        PermissionCacheSyncManager permissionCacheSyncManager) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 添加权限变更消息监听器
        container.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                permissionCacheSyncManager.onMessage(message.getBody());
            }
        }, new ChannelTopic(PermissionCacheSyncManager.PERMISSION_CHANGE_CHANNEL));

        log.info("RedisMessageListenerContainer initialized for permission cache sync");
        return container;
    }

    // ==================== 权限提供者配置 ====================

    /**
     * Redis 权限提供者（始终创建，支持 DB 委托回退）
     * <p>当存在自定义 PermissionProvider 实现（如 DbPermissionProvider）时，自动设置为 Redis 的委托</p>
     */
    @Bean
    public RedisPermissionProvider redisPermissionProvider() {
        RedisPermissionProvider provider = new RedisPermissionProvider();
        log.info("RedisPermissionProvider initialized");
        return provider;
    }

    /**
     * 注入 DB 层委托到 RedisPermissionProvider
     * <p>如果存在自定义 PermissionProvider（如 DbPermissionProvider），将其设为 Redis 的缓存未命中回退</p>
     */
    @Bean
    public Object redisPermissionProviderDelegateInitializer(
            RedisPermissionProvider redisProvider,
            List<PermissionProvider> allProviders) {
        for (PermissionProvider p : allProviders) {
            // 跳过 RedisPermissionProvider 自身 和 CachedPermissionProvider
            if (p != redisProvider && !(p instanceof CachedPermissionProvider)) {
                redisProvider.setDelegate(p);
                log.info("RedisPermissionProvider delegate set to: {}", p.getClass().getSimpleName());
                break;
            }
        }
        return new Object();
    }

    /**
     * 权限提供者（带本地缓存）
     * <p>根据配置决定是否启用 Caffeine 本地缓存</p>
     * <p>标记为 @Primary，确保缓存链优先于独立的 DbPermissionProvider 被注入</p>
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "cachedPermissionProvider")
    public PermissionProvider permissionProvider(RedisPermissionProvider redisProvider) {
        if (properties.getPermission().getCache().isEnabled()) {
            long localTtl = properties.getPermission().getCache().getLocalTtl();
            int maxSize = properties.getPermission().getCache().getMaxSize();

            CachedPermissionProvider cachedProvider = new CachedPermissionProvider(
                redisProvider,
                Duration.ofSeconds(localTtl),
                maxSize,
                "resource-server-cache"
            );

            log.info("CachedPermissionProvider initialized: localTtl={}s, maxSize={}", localTtl, maxSize);
            return cachedProvider;
        }

        log.info("PermissionProvider using Redis without local cache");
        return redisProvider;
    }

    /**
     * 权限同步管理器（Pub/Sub）
     */
    @Bean
    @ConditionalOnMissingBean(PermissionCacheSyncManager.class)
    public PermissionCacheSyncManager permissionCacheSyncManager(PermissionProvider permissionProvider) {
        PermissionCacheSyncManager syncManager = new PermissionCacheSyncManager();

        // 如果权限提供者是 CachedPermissionProvider，添加到管理器
        if (permissionProvider instanceof CachedPermissionProvider cached) {
            syncManager.setCachedProviders(List.of(cached));
        }

        log.info("PermissionCacheSyncManager initialized");
        return syncManager;
    }

    /**
     * 权限服务
     */
    @Bean
    @ConditionalOnMissingBean(PermissionService.class)
    public PermissionService permissionService(PermissionProvider permissionProvider,
                                               PermissionCacheSyncManager syncManager) {
        return new PermissionService(permissionProvider, syncManager);
    }

    // ==================== 安全过滤器配置 ====================

    /**
     * 用户上下文认证过滤器
     * <p>从 X-User-Id/X-User-Roles Header 构建 SecurityContext</p>
     */
    @Bean
    @ConditionalOnMissingBean(UserContextAuthenticationFilter.class)
    public UserContextAuthenticationFilter userContextAuthenticationFilter(
        PermissionProvider permissionProvider,
        IdentityAssertionProperties identityAssertionProperties) {
        UserContextAuthenticationFilter filter = new UserContextAuthenticationFilter(permissionProvider);
        filter.setWhitelistPaths(properties.getAuth().getWhitelistPaths());
        filter.setStrictHeaderAuth(properties.getAuth().isStrictHeaderAuth());
        filter.setSuperRoleCode(properties.getAuth().getSuperRoleCode());
        filter.setIdentityAssertionVerifier(new IdentityAssertionVerifier(identityAssertionProperties));
        // 这行日志是排查「身份拿不到」的第一现场：它直接告诉你验签通道开没开、密钥配没配
        log.info("UserContextAuthenticationFilter initialized: identityAssertion(verifyEnabled={}, secretConfigured={}), "
                + "strictHeaderAuth={}",
            identityAssertionProperties.isVerifyEnabled(), identityAssertionProperties.hasSecret(),
            properties.getAuth().isStrictHeaderAuth());
        return filter;
    }

    /**
     * 安全过滤器链配置
     * <p>无状态会话 + Header 认证</p>
     */
    @Bean
    @Order(100)
    @ConditionalOnMissingBean(name = "resourceServerSecurityFilterChain")
    public SecurityFilterChain resourceServerSecurityFilterChain(
        HttpSecurity http,
        UserContextAuthenticationFilter userContextFilter) throws Exception {

        http
            // 禁用 CSRF（API 无状态）
            .csrf(AbstractHttpConfigurer::disable)

            // 无状态会话
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 添加自定义过滤器（在 UsernamePasswordAuthenticationFilter 之前）
            .addFilterBefore(userContextFilter, UsernamePasswordAuthenticationFilter.class)

            // 授权配置：本层不做路径级准入，一切放行。
            //
            // 准入的权威点在网关：OAuth2AuthenticationFilter 对非白名单路径强制校验 token，
            // 下游服务拿不到 token，没有能力做出比网关更强的判定，能做的只是转述网关的结论。
            // 转述失败（无 X-User-Id、来源不可信、解析异常）应当降级为匿名，而不是升级为拒绝 ——
            // 若在此处 anyRequest().authenticated()，UserContextAuthenticationFilter 的每一次降级
            // 都会变成 401，一处配置疏漏即可让整个服务不可用（典型事故：网关经 Nacos 的 lb:// 连到
            // 本服务的注册地址而非回环地址，源 IP 不在 trusted-ip-ranges 内，X-User-* 头被拒 → 全站 401）。
            //
            // 「需要登录人 / 需要权限」改为接口级显式声明：
            //   @PreAuthorize("isAuthenticated()")        —— 需要登录人
            //   @PreAuthorize("hasPermission(...)")       —— 需要权限
            // 由下方 @EnableMethodSecurity 与 CarlosPermissionEvaluator 处理，失败时由
            // UnauthorizedEntryPoint / ForbiddenAccessDeniedHandler 返回统一的错误响应。
            //
            // 前提与代价：本层不再拦截未认证请求，因此下游端口在网络上必须只对网关可达 ——
            // 边界由应用层 IP 白名单移交给网络层（内网隔离 / 安全组 / 服务网格），
            // 应用层替代不了它：它一收紧就会连自己的正常流量一起拦掉。
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

            // 异常处理
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(new UnauthorizedEntryPoint())
                .accessDeniedHandler(new ForbiddenAccessDeniedHandler())
            );

        log.info("SecurityFilterChain configured for resource server");
        return http.build();
    }

    // ==================== 方法级安全配置 ====================

    /**
     * 启用方法级安全注解
     */
    @Configuration
    @EnableMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true
    )
    @ConditionalOnProperty(prefix = "carlos.resource-server", name = "method-security-enabled", havingValue = "true", matchIfMissing = true)
    public static class MethodSecurityConfig {

        /**
         * 自定义权限评估器
         * <p>支持 hasPermission() 表达式</p>
         */
        @Bean
        @ConditionalOnMissingBean(name = "permissionEvaluator")
        public CarlosPermissionEvaluator permissionEvaluator(ObjectProvider<DataScopeProvider> dataScopeProvider) {
            log.info("PermissionEvaluator initialized");
            return new CarlosPermissionEvaluator(dataScopeProvider.getIfAvailable());
        }

        /**
         * 多维鉴权表达式 Bean（SpEL 引用名 {@code ps}）
         *
         * <p>供 {@code @PreAuthorize("@ps.hasAnyPermissions(...)")} 之类的表达式使用。</p>
         *
         * <p><b>框架层必须用 {@code @Bean} 而非 {@code @Component} 注册</b>：starter 不参与宿主应用的
         * 组件扫描（扫描根是宿主自己的包），类上加 {@code @Component} 会因包不在扫描根内而漏注册，
         * 表现为运行时 {@code @ps} 解析不到 Bean。</p>
         */
        @Bean("ps")
        @ConditionalOnMissingBean(name = "ps")
        public PermissionChecker permissionChecker(ObjectProvider<DataScopeProvider> dataScopeProvider) {
            log.info("PermissionChecker(@ps) initialized");
            return new PermissionChecker(dataScopeProvider);
        }

        /**
         * 方法安全表达式处理器
         */
        @Bean
        @ConditionalOnMissingBean(MethodSecurityExpressionHandler.class)
        public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            CarlosPermissionEvaluator permissionEvaluator) {
            DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
            handler.setPermissionEvaluator(permissionEvaluator);
            log.info("MethodSecurityExpressionHandler configured with custom PermissionEvaluator");
            return handler;
        }
    }
}
