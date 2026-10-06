package com.carlos.security.permission.cache;

import com.carlos.core.constant.RedisKeys;
import com.carlos.redis.util.RedisUtil;
import com.carlos.security.permission.provider.CachedPermissionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <p>
 * 权限缓存同步管理器
 * </p>
 *
 * <p>基于 Redis Pub/Sub 实现多实例间权限缓存的主动失效机制：</p>
 * <pre>
 * 场景：服务A修改了角色权限，需要通知服务B、C清除本地缓存
 *
 * 服务A                      Redis Channel                    服务B/C
 *   │                            │                              │
 *   │  publish(permission:change, roleId)                       │
 *   ├───────────────────────────▶│─────────────────────────────▶│
 *   │                            │                              │
 *   │                            │                    onMessage()
 *   │                            │                              │
 *   │                            │                    clearLocalCache(roleId)
 *   │                            │                              │
 * </pre>
 *
 * <h3>使用方式：</h3>
 * <pre>{@code
 * // 1. 权限变更时发布事件
 * @Service
 * public class PermissionService {
 *
 *     @Autowired
 *     private PermissionCacheSyncManager syncManager;
 *
 *     public void updateRolePermissions(Long roleId, Set<String> permissions) {
 *         // 1. 更新数据库
 *         permissionMapper.update(roleId, permissions);
 *
 *         // 2. 更新Redis缓存
 *         redisPermissionProvider.savePermissions(roleId, permissions);
 *
 *         // 3. 发布缓存失效事件（通知所有服务清除本地缓存）
 *         syncManager.publishPermissionChange(roleId);
 *     }
 * }
 * }</pre>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see CachedPermissionProvider
 */
@Slf4j
public class PermissionCacheSyncManager {

    /**
     * Redis Channel 名称（跨模块共用，见 {@link RedisKeys#AUTH_PERMISSION_CHANGE_CHANNEL}）
     */
    public static final String PERMISSION_CHANGE_CHANNEL = RedisKeys.AUTH_PERMISSION_CHANGE_CHANNEL;

    /**
     * 消息前缀（用于区分消息类型）
     */
    private static final String MSG_PREFIX_CLEAR = "CLEAR:";
    private static final String MSG_PREFIX_CLEAR_ALL = "CLEAR_ALL";

    /**
     * 缓存提供者列表
     */
    private List<CachedPermissionProvider> cachedProviders;

    /**
     * 创建同步管理器
     */
    public PermissionCacheSyncManager() {
    }

    /**
     * 创建同步管理器
     *
     * @param cachedProviders 缓存提供者列表
     */
    public PermissionCacheSyncManager(List<CachedPermissionProvider> cachedProviders) {
        this.cachedProviders = cachedProviders;
    }

    /**
     * 设置缓存提供者列表
     */
    public void setCachedProviders(List<CachedPermissionProvider> cachedProviders) {
        this.cachedProviders = cachedProviders;
    }

    /**
     * 发布权限变更事件（清除指定角色的缓存）
     *
     * @param roleCode 角色编码
     */
    public void publishPermissionChange(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return;
        }

        try {
            String message = MSG_PREFIX_CLEAR + roleCode;
            // 使用 RedisUtil 发送消息到频道
            RedisUtil.convertAndSend(PERMISSION_CHANGE_CHANNEL, message);
            log.info("Published permission change event for role: {}", roleCode);
        } catch (Exception e) {
            log.error("Failed to publish permission change event for role: {}", roleCode, e);
        }
    }

    /**
     * 发布清除所有缓存事件
     */
    public void publishClearAll() {
        try {
            RedisUtil.convertAndSend(PERMISSION_CHANGE_CHANNEL, MSG_PREFIX_CLEAR_ALL);
            log.info("Published clear all permission caches event");
        } catch (Exception e) {
            log.error("Failed to publish clear all event", e);
        }
    }

    /**
     * 批量发布权限变更事件
     *
     * @param roleCodes 角色编码列表
     */
    public void publishPermissionChanges(Iterable<String> roleCodes) {
        if (roleCodes == null) {
            return;
        }

        for (String roleCode : roleCodes) {
            publishPermissionChange(roleCode);
        }
    }

    /**
     * 接收 Redis 消息（由 MessageListener 调用）
     *
     * @param message 消息体
     */
    public void onMessage(byte[] message) {
        try {
            String msgBody = new String(message, StandardCharsets.UTF_8);
            log.debug("Received permission cache sync message: {}", msgBody);

            if (msgBody.startsWith(MSG_PREFIX_CLEAR)) {
                // 解析角色编码（角色编码是不含冒号的不透明字符串，去掉前缀即为原值）
                String roleCode = msgBody.substring(MSG_PREFIX_CLEAR.length());
                handleClearCache(roleCode);
            } else if (MSG_PREFIX_CLEAR_ALL.equals(msgBody)) {
                handleClearAllCache();
            }
        } catch (Exception e) {
            log.error("Failed to process permission cache sync message", e);
        }
    }

    /**
     * 处理清除指定角色缓存（Caffeine L1 + Redis L2）
     *
     * @param roleCode 角色编码
     */
    private void handleClearCache(String roleCode) {
        if (cachedProviders == null || cachedProviders.isEmpty()) {
            log.warn("No CachedPermissionProvider found to clear cache for role: {}", roleCode);
            return;
        }

        for (CachedPermissionProvider provider : cachedProviders) {
            try {
                // 调用 clearCache（清除 Caffeine L1 + Redis L2），而非仅 clearLocalCache
                // 确保权限变更后，Redis 中的旧数据也被同时清除
                provider.clearCache(roleCode);
                log.debug("Cleared L1+L2 cache for role: {} in provider: {}",
                    roleCode, provider.getName());
            } catch (Exception e) {
                log.error("Failed to clear cache for role: {} in provider: {}",
                    roleCode, provider.getName(), e);
            }
        }

        log.info("Successfully cleared L1+L2 cache for role: {} across all providers", roleCode);
    }

    /**
     * 处理清除所有缓存
     */
    private void handleClearAllCache() {
        if (cachedProviders == null || cachedProviders.isEmpty()) {
            log.warn("No CachedPermissionProvider found to clear all caches");
            return;
        }

        for (CachedPermissionProvider provider : cachedProviders) {
            try {
                provider.clearAllLocalCache();
                log.debug("Cleared all local caches in provider: {}", provider.getName());
            } catch (Exception e) {
                log.error("Failed to clear all caches in provider: {}", provider.getName(), e);
            }
        }

        log.info("Successfully cleared all local caches across all providers");
    }
}
