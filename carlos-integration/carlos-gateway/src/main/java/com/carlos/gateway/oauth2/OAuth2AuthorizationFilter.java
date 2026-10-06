package com.carlos.gateway.oauth2;

import com.carlos.core.auth.AuthConstant;
import com.carlos.core.auth.UserContext;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.core.util.PathMatchUtil;
import com.carlos.gateway.config.GlobalFilterOrder;
import com.carlos.gateway.oauth2.provider.PermissionProvider;
import com.carlos.gateway.whitelist.WhitelistContext;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * OAuth2 鉴权过滤器 - 性能优化版
 * 基于 RBAC 或 ABAC 的权限控制
 * </p>
 * <p>
 * 优化点：
 * 1. 优先使用统一白名单检查结果（UnifiedWhitelistFilter）
 * 2. Caffeine 缓存用户权限，减少 Redis 查询
 * 3. 使用 PathMatchUtil 替代正则匹配，避免重复编译
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 * @updated 2026-04-10 优化：统一白名单 + 权限缓存
 */
@Slf4j
public class OAuth2AuthorizationFilter implements GlobalFilter, Ordered {

    private final OAuth2GatewayProperties properties;
    private final PermissionProvider permissionProvider;

    // 权限缓存：userId -> 权限集合（异步加载，避免阻塞 EventLoop）
    private final AsyncLoadingCache<String, Set<String>> permissionCache;

    // 权限匹配结果缓存：userId:method:path -> boolean（无需加载器，手动 put）
    private final Cache<String, Boolean> matchResultCache;

    public OAuth2AuthorizationFilter(OAuth2GatewayProperties properties,
                                     PermissionProvider permissionProvider) {
        this.properties = properties;
        this.permissionProvider = permissionProvider;

        // 初始化权限缓存（5分钟过期，最大10000条）
        this.permissionCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .buildAsync((key, executor) -> loadUserPermissions(key));

        // 初始化匹配结果缓存（1分钟过期，最大50000条）
        this.matchResultCache = Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterWrite(Duration.ofMinutes(1))
            .recordStats()
            .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单快速放行（优先使用统一白名单检查结果）
        if (isWhitelisted(exchange, path)) {
            return chain.filter(exchange);
        }

        // 未启用授权或 OAuth2，直接放行
        if (!properties.isEnabled() || !properties.isAuthorizationEnabled()) {
            return chain.filter(exchange);
        }

        HttpMethod method = request.getMethod();

        // 获取用户上下文
        UserContext userContext = exchange.getAttribute(AuthConstant.USER_CONTEXT);
        if (userContext == null) {
            log.warn("No user context found for path: {}", path);
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "Authentication required");
        }

        String userId = String.valueOf(userContext.getUserId());
        String cacheKey = buildCacheKey(userId, method, path);

        // 根据授权模式进行权限校验
        Mono<Boolean> permissionMono = switch (properties.getAuthorizationMode()) {
            case RBAC -> checkRbacPermissionOptimized(userContext, path, method, cacheKey);
            case ABAC -> checkAbacPermission(userContext, path, method, exchange);
        };

        return permissionMono.flatMap(hasPermission -> {
            if (Boolean.TRUE.equals(hasPermission)) {
                return chain.filter(exchange);
            }
            if (log.isWarnEnabled()) {
                log.warn("User {} has no permission for {} {}", userContext.getAccount(), method, path);
            }
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Access denied");
        });
    }

    /**
     * 检查路径是否在白名单中（优化版）
     * <p>
     * 优先使用统一白名单检查结果，如果统一白名单未启用，则使用本地白名单
     *
     * @param exchange ServerWebExchange
     * @param path     请求路径
     * @return true 如果在白名单中
     */
    private boolean isWhitelisted(ServerWebExchange exchange, String path) {
        // 1. 优先使用统一白名单检查结果
        if (WhitelistContext.isAuthWhitelisted(exchange)) {
            return true;
        }

        // 2. 降级兼容：统一白名单未启用或结果不存在，使用本地白名单
        return PathMatchUtil.antMatchAny(properties.getWhitelist(), path);
    }

    /**
     * 优化的 RBAC 权限校验（带缓存）
     * <p>
     * 使用异步权限加载，避免阻塞 Netty EventLoop
     */
    private Mono<Boolean> checkRbacPermissionOptimized(UserContext userContext, String path,
                                                       HttpMethod method, String cacheKey) {
        // 先从本地缓存获取匹配结果
        Boolean cachedResult = matchResultCache.getIfPresent(cacheKey);
        if (cachedResult != null) {
            log.debug("Permission cache hit for user {}: {}", userContext.getUserId(), cachedResult);
            return Mono.just(cachedResult);
        }

        String userId = String.valueOf(userContext.getUserId());

        // 异步从缓存获取权限集合，避免阻塞 EventLoop
        return Mono.fromFuture(permissionCache.get(userId))
            .flatMap(permissions -> {
                if (permissions == null || permissions.isEmpty()) {
                    matchResultCache.put(cacheKey, false);
                    return Mono.just(false);
                }

                // 执行权限匹配
                boolean hasPermission = matchPermissionOptimized(permissions, method, path);

                // 缓存结果
                matchResultCache.put(cacheKey, hasPermission);

                return Mono.just(hasPermission);
            });
    }

    /**
     * 优化的权限匹配（使用 PathMatchUtil）
     */
    private boolean matchPermissionOptimized(Set<String> permissions, HttpMethod method, String path) {
        String requiredPermission = method + ":" + path;

        for (String permission : permissions) {
            // 1. 精确匹配（O(1)）
            if (permission.equals(requiredPermission)) {
                return true;
            }

            // 2. 使用 PathMatchUtil 进行 Ant 风格匹配
            if (permission.contains("*") || permission.contains("?")) {
                // 提取 pattern 的方法部分和路径部分
                int colonIndex = permission.indexOf(':');
                if (colonIndex > 0) {
                    String permMethod = permission.substring(0, colonIndex);
                    String permPath = permission.substring(colonIndex + 1);

                    // 方法匹配（* 表示任意方法）
                    if ("*".equals(permMethod) || permMethod.equals(method.name())) {
                        // 使用 PathMatchUtil 进行路径匹配
                        if (PathMatchUtil.match(permPath, path)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 异步加载用户权限（用于 AsyncLoadingCache）
     * <p>
     * 将 Reactive Mono 转换为 CompletableFuture，避免在 WebFlux EventLoop 上调用 .block()
     */
    private CompletableFuture<Set<String>> loadUserPermissions(String userId) {
        return permissionProvider.getUserPermissions(userId)
            .doOnNext(permissions ->
                log.debug("Loaded permissions for user {}: {} items", userId, permissions != null ? permissions.size() : 0))
            .switchIfEmpty(Mono.just(Set.of()))
            .onErrorResume(e -> {
                log.error("Failed to load permissions for user {}", userId, e);
                return Mono.just(Set.of());
            })
            .toFuture();
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(String userId, HttpMethod method, String path) {
        return userId + ":" + method + ":" + path;
    }

    /**
     * ABAC 权限校验（基于属性）
     */
    private Mono<Boolean> checkAbacPermission(UserContext userContext, String path,
                                              HttpMethod method, ServerWebExchange exchange) {
        // 构建属性集合（使用不可变 Map 减少内存分配）
        Map<String, Object> attributes = Map.of(
            "userId", userContext.getUserId(),
            "roleIds", userContext.getRoleIds(),
            "deptId", userContext.getDepartmentId(),
            "tenantId", userContext.getTenantId(),
            "path", path,
            "method", method.name(),
            "time", System.currentTimeMillis()
        );

        // 执行 ABAC 策略
        return permissionProvider.evaluateAbacPolicy(attributes);
    }

    /**
     * 刷新用户权限缓存（供外部调用，如权限变更时）
     */
    public void invalidatePermissionCache(String userId) {
        permissionCache.synchronous().invalidate(userId);
        // 同时清除该用户的所有匹配结果缓存
        matchResultCache.asMap().keySet().removeIf(key -> key.startsWith(userId + ":"));
        if (log.isInfoEnabled()) {
            log.info("Invalidated permission cache for user {}", userId);
        }
    }

    /**
     * 获取缓存统计信息（用于监控）
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
            "permissionCache", permissionCache.synchronous().stats().toString(),
            "matchResultCache", matchResultCache.stats().toString(),
            "permissionCacheSize", permissionCache.synchronous().estimatedSize(),
            "matchResultCacheSize", matchResultCache.estimatedSize()
        );
    }

    @Override
    public int getOrder() {
        // 必须在认证过滤器之后执行，依赖 exchange 中的 userContext
        return GlobalFilterOrder.AUTHORIZATION_ORDER;
    }
}
