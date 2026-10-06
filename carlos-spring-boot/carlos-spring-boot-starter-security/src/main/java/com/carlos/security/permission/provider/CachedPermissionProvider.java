package com.carlos.security.permission.provider;

import com.carlos.security.permission.PermissionProvider;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 带本地缓存的权限提供者
 * </p>
 *
 * <p>三层缓存架构：</p>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  1. Caffeine 本地缓存 (L1)                                        │
 * │     - 线程安全的 Guava Cache 替代品                                │
 * │     - 支持过期时间、最大容量限制                                    │
 * │     - 使用 LoadingCache 自动加载                                  │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  2. 委托 Provider (通常是 RedisPermissionProvider)                │
 * │     - Redis 分布式缓存                                            │
 * │     - 多服务共享                                                   │
 * ├─────────────────────────────────────────────────────────────────┤
 * │  3. 自定义加载器 (如 DbPermissionProvider)                         │
 * │     - 从数据库或其他数据源加载                                       │
 * │     - 兜底策略                                                     │
 * └─────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see PermissionProvider
 */
@Slf4j
public class CachedPermissionProvider implements PermissionProvider {

    /**
     * 本地 Caffeine 缓存（key 为角色编码）
     */
    private final LoadingCache<String, Set<String>> localCache;

    /**
     * 委托的权限提供者（通常是 RedisPermissionProvider）
     */
    private final PermissionProvider delegate;

    /**
     * 缓存名称（用于区分多个实例）
     */
    private final String name;

    /**
     * 创建带本地缓存的权限提供者
     *
     * @param delegate 委托的权限提供者（不能为 null）
     * @param ttl      本地缓存过期时间
     * @param maxSize  本地缓存最大条目数
     */
    public CachedPermissionProvider(PermissionProvider delegate, Duration ttl, int maxSize) {
        this(delegate, ttl, maxSize, "default");
    }

    /**
     * 创建带本地缓存的权限提供者
     *
     * @param delegate 委托的权限提供者（不能为 null）
     * @param ttl      本地缓存过期时间
     * @param maxSize  本地缓存最大条目数
     * @param name     缓存名称标识
     */
    public CachedPermissionProvider(PermissionProvider delegate, Duration ttl, int maxSize, String name) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate PermissionProvider cannot be null");
        }
        this.delegate = delegate;
        this.name = name;

        this.localCache = Caffeine.newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(maxSize)
            .recordStats()  // 开启统计
            .build(this::loadFromDelegate);

        log.info("CachedPermissionProvider '{}' initialized: ttl={}, maxSize={}",
            name, ttl, maxSize);
    }

    /**
     * 从委托提供者加载权限
     */
    private Set<String> loadFromDelegate(String roleCode) {
        return delegate.getPermissions(roleCode);
    }

    @Override
    public Set<String> getPermissions(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return Collections.emptySet();
        }

        // 从本地缓存获取（不存在时自动从 delegate 加载）
        Set<String> permissions = localCache.get(roleCode);

        // 空集合表示缓存了"无权限"的状态
        return permissions != null ? permissions : Collections.emptySet();
    }

    @Override
    public Set<String> getPermissions(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptySet();
        }

        // 批量获取（命中缓存的直接返回，未命中的从 delegate 加载）
        Map<String, Set<String>> resultMap = localCache.getAll(roleCodes);

        // 合并所有权限
        return resultMap.values().stream()
            .flatMap(Set::stream)
            .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public void savePermissions(String roleCode, Set<String> permissions) {
        // 先保存到 delegate（Redis）
        delegate.savePermissions(roleCode, permissions);

        // 更新本地缓存
        if (permissions != null) {
            localCache.put(roleCode, permissions);
        } else {
            localCache.invalidate(roleCode);
        }

        log.debug("Saved permissions to cache for role: {}", roleCode);
    }

    @Override
    public void savePermissions(String roleCode, Set<String> permissions, long ttl) {
        // 先保存到 delegate（Redis，带 TTL）
        delegate.savePermissions(roleCode, permissions, ttl);

        // 更新本地缓存（本地缓存有独立的过期时间）
        if (permissions != null) {
            localCache.put(roleCode, permissions);
        } else {
            localCache.invalidate(roleCode);
        }

        log.debug("Saved permissions to cache for role: {} with Redis TTL: {}", roleCode, ttl);
    }

    @Override
    public void clearCache(String roleCode) {
        // 清除本地缓存
        clearLocalCache(roleCode);

        // 清除委托缓存（Redis）
        delegate.clearCache(roleCode);

        log.info("Cleared all caches for role: {}", roleCode);
    }

    /**
     * 仅清除本地缓存（用于跨实例同步场景）
     *
     * @param roleCode 角色编码
     */
    public void clearLocalCache(String roleCode) {
        if (StringUtils.hasText(roleCode)) {
            localCache.invalidate(roleCode);
            log.debug("Cleared local cache for role: {}", roleCode);
        }
    }

    /**
     * 清除所有本地缓存
     */
    public void clearAllLocalCache() {
        localCache.invalidateAll();
        log.info("Cleared all local caches in provider: {}", name);
    }

    /**
     * 获取缓存名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取本地缓存统计信息
     */
    public CacheStats getStats() {
        return localCache.stats();
    }

    /**
     * 获取缓存统计信息（Map 格式）
     */
    public Map<String, Object> getStatsMap() {
        CacheStats stats = localCache.stats();
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("hitCount", stats.hitCount());
        map.put("missCount", stats.missCount());
        map.put("hitRate", stats.hitRate());
        map.put("missRate", stats.missRate());
        map.put("loadSuccessCount", stats.loadSuccessCount());
        map.put("loadFailureCount", stats.loadFailureCount());
        map.put("totalLoadTimeMs", stats.totalLoadTime() / 1_000_000);
        map.put("evictionCount", stats.evictionCount());
        map.put("estimatedSize", localCache.estimatedSize());
        return map;
    }

    /**
     * 获取原始 Caffeine 缓存实例（用于高级操作）
     */
    public Cache<String, Set<String>> getNativeCache() {
        return localCache;
    }
}
