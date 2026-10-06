package com.carlos.auth.config;

import com.carlos.security.permission.PermissionService;
import com.carlos.security.permission.provider.RedisPermissionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 权限码解析配置（仅供 /auth/user/me 的 buttons 使用）
 *
 * <p>carlos-auth 是 IdP，拥有独立的授权/默认过滤器链，不能开启
 * {@code carlos.resource-server.enabled=true} —— 那会同时激活 @EnableMethodSecurity，
 * 而 {@code CarlosPermissionEvaluator} 要求 principal 为 UserContext（auth 里是 SecurityUser），
 * 会把我方控制器上的 @PreAuthorize 全部变成 403。</p>
 *
 * <p>因此这里只注册权限读取链的最小集：RedisPermissionProvider 直读
 * {@code auth:permissions:{roleCode}}（由 carlos-org 负责 DB 加载与回填），
 * 不做本地 Caffeine 缓存、不启用方法级安全。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-10-01
 */
@Slf4j
@Configuration
public class AuthPermissionConfig {

    /**
     * Redis 权限提供者（无 DB 委托，缓存未命中返回空集合，由 carlos-org 保证缓存填充）
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisPermissionProvider redisPermissionProvider() {
        log.info("RedisPermissionProvider initialized for carlos-auth (read-only)");
        return new RedisPermissionProvider();
    }

    /**
     * 权限服务（同步管理器为 null：auth 只读权限，不发缓存同步事件）
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionService permissionService(RedisPermissionProvider redisPermissionProvider) {
        return new PermissionService(redisPermissionProvider, null);
    }
}
