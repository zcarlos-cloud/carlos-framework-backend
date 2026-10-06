package com.carlos.security.permission.provider;

import com.carlos.core.constant.RedisKeys;
import com.carlos.redis.util.RedisUtil;
import com.carlos.security.permission.PermissionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * 基于 Redis 的权限提供者实现（支持 DB 委托回退）
 * </p>
 *
 * <p>使用框架 {@link RedisUtil} 进行缓存操作。当 Redis 缓存未命中时，自动回退到委托的
 * {@link PermissionProvider}（如 {@code DbPermissionProvider}）加载数据，并回填缓存。</p>
 *
 * <pre>
 * 三层缓存架构：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Caffeine L1 → Redis L2 → Delegate (DB) L3                   │
 * │  Cache miss 时自动回退加载 + 回填缓存                           │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Redis Key 结构：
 *   Key: auth:permissions:{roleCode}
 *   Value: Set&lt;String&gt; (权限标识集合)
 *   TTL: 可配置（默认7天）
 *
 * 示例：
 *   SET auth:permissions:SUPER_ADMIN -> ["user:create", "user:read", "order:*"]
 *   EXPIRE auth:permissions:SUPER_ADMIN 604800
 * </pre>
 *
 * <p><b>注意：</b>此类依赖 {@code carlos-spring-boot-starter-redis} 模块，</p>
 * <p>确保已配置 Redis 连接信息。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see PermissionProvider
 * @see RedisUtil
 */
@Slf4j
public class RedisPermissionProvider implements PermissionProvider {

    /**
     * Redis Key 前缀（跨模块共用，见 {@link RedisKeys#AUTH_PERMISSIONS}）
     */
    private static final String KEY_PREFIX = RedisKeys.AUTH_PERMISSIONS;

    /**
     * 默认缓存时间（7天，单位：秒）
     */
    private static final long DEFAULT_TTL = 7 * 24 * 60 * 60;

    /**
     * DB 委托提供者（Redis 缓存未命中时回退加载）
     */
    private PermissionProvider delegate;

    /**
     * 无参构造
     */
    public RedisPermissionProvider() {
    }

    /**
     * 带委托的构造
     *
     * @param delegate DB 层权限提供者（缓存未命中时回退）
     */
    public RedisPermissionProvider(PermissionProvider delegate) {
        this.delegate = delegate;
    }

    /**
     * 设置委托提供者
     */
    public void setDelegate(PermissionProvider delegate) {
        this.delegate = delegate;
    }

    /**
     * 从 Redis 获取指定角色的权限（缓存未命中时回退到 DB 委托）
     *
     * @param roleCode 角色编码
     * @return 权限集合，缓存和 DB 都不存在返回空集合（不是 null）
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getPermissions(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return Collections.emptySet();
        }

        String key = KEY_PREFIX + roleCode;

        try {
            Object cached = RedisUtil.getValue(key);
            if (cached instanceof Set) {
                Set<String> permissions = (Set<String>) cached;
                log.debug("Redis cache hit for role: {}, permissions: {}", roleCode, permissions.size());
                return new HashSet<>(permissions);
            }
        } catch (Exception e) {
            log.error("Failed to get permissions from Redis for role: {}", roleCode, e);
        }

        // Redis 未命中，回退到 DB 委托加载
        log.debug("Redis cache miss for role: {}, falling back to delegate", roleCode);
        if (delegate != null) {
            try {
                Set<String> permissions = delegate.getPermissions(roleCode);
                if (permissions != null && !permissions.isEmpty()) {
                    // 回填 Redis 缓存
                    savePermissions(roleCode, permissions);
                    log.debug("Loaded from delegate and cached for role: {}, count: {}", roleCode, permissions.size());
                    return new HashSet<>(permissions);
                }
            } catch (Exception e) {
                log.error("Failed to load permissions from delegate for role: {}", roleCode, e);
            }
        }

        return Collections.emptySet();
    }

    /**
     * 批量获取多个角色的权限
     *
     * @param roleCodes 角色编码集合
     * @return 合并后的权限集合
     */
    @Override
    public Set<String> getPermissions(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> allPermissions = new HashSet<>();
        for (String roleCode : roleCodes) {
            allPermissions.addAll(getPermissions(roleCode));
        }

        return allPermissions;
    }

    /**
     * 保存权限到 Redis（使用默认 TTL）
     *
     * @param roleCode    角色编码
     * @param permissions 权限集合
     */
    @Override
    public void savePermissions(String roleCode, Set<String> permissions) {
        savePermissions(roleCode, permissions, DEFAULT_TTL);
    }

    /**
     * 保存权限到 Redis（指定 TTL）
     *
     * @param roleCode    角色编码
     * @param permissions 权限集合
     * @param ttl         过期时间（秒）
     */
    @Override
    public void savePermissions(String roleCode, Set<String> permissions, long ttl) {
        if (!StringUtils.hasText(roleCode)) {
            return;
        }

        String key = KEY_PREFIX + roleCode;

        try {
            if (permissions == null || permissions.isEmpty()) {
                // 保存空集合，防止缓存穿透
                RedisUtil.setValue(key, Collections.emptySet(), ttl);
            } else {
                RedisUtil.setValue(key, new HashSet<>(permissions), ttl);
            }
            log.debug("Saved permissions to Redis for role: {}, ttl: {}s", roleCode, ttl);
        } catch (Exception e) {
            log.error("Failed to save permissions to Redis for role: {}", roleCode, e);
        }
    }

    /**
     * 清除指定角色的缓存
     *
     * @param roleCode 角色编码
     */
    @Override
    public void clearCache(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return;
        }

        String key = KEY_PREFIX + roleCode;

        try {
            RedisUtil.delete(key);
            log.debug("Cleared Redis cache for role: {}", roleCode);
        } catch (Exception e) {
            log.error("Failed to clear Redis cache for role: {}", roleCode, e);
        }
    }

    /**
     * 批量清除缓存
     *
     * @param roleCodes 角色编码集合
     */
    public void clearCache(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return;
        }

        for (String roleCode : roleCodes) {
            clearCache(roleCode);
        }
    }

    /**
     * 清除所有权限缓存（谨慎使用）
     */
    public void clearAllCache() {
        try {
            Set<String> keys = RedisUtil.scanKeys(KEY_PREFIX + "*", 1000);
            if (!keys.isEmpty()) {
                RedisUtil.delete(keys);
                log.info("Cleared all permission caches, count: {}", keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to clear all permission caches", e);
        }
    }

    /**
     * 检查缓存是否存在
     *
     * @param roleCode 角色编码
     * @return true 如果缓存存在
     */
    public boolean hasCache(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return false;
        }
        return RedisUtil.hasKey(KEY_PREFIX + roleCode);
    }

    /**
     * 获取缓存过期时间
     *
     * @param roleCode 角色编码
     * @return 剩余秒数，-1 表示永不过期，-2 表示不存在
     */
    public long getTtl(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return -2;
        }
        return RedisUtil.getExpire(KEY_PREFIX + roleCode);
    }
}
