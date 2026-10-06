package com.carlos.core.constant;

/**
 * <p>
 * 跨模块共用的 Redis Key 常量
 * </p>
 *
 * <p>只放「两个以上 Maven 模块必须口径一致」的 key —— 模块私有的 key 一律留在各模块自己的常量类里
 * （carlos-auth 的 {@code AuthRedisKey}、carlos-monitor 的 {@code CacheKeyConstant} 等）。</p>
 *
 * <p>放在 core 而不是某个业务模块，是因为 carlos-gateway 依赖 core 但<b>不依赖</b> carlos-auth，
 * 而下面这些 key 同时被「写方」和「读方」跨模块使用，散落硬编码迟早会改漏一处。</p>
 *
 * <p>命名遵循 {@code {模块}:{业务}:{标识}}（见 carlos-framework-standard skill）。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public interface RedisKeys {

    /**
     * 角色/用户权限集合缓存前缀
     *
     * <p><b>⚠️ 历史遗留语义冲突</b>：本前缀当前被两种语义共用 ——
     * {@code carlos-spring-boot-starter-security} 的 {@code RedisPermissionProvider}
     * 按 <b>roleCode</b> 写入，而 {@code carlos-gateway} 的 {@code DefaultPermissionProvider}
     * 按 <b>userId</b> 读取。本次只统一前缀，语义冲突待单独立项收敛。</p>
     */
    String AUTH_PERMISSIONS = "auth:permissions:";

    /**
     * 权限变更广播频道（Redis Pub/Sub，用于跨节点清缓存）
     */
    String AUTH_PERMISSION_CHANGE_CHANNEL = "auth:permission:change";

    /**
     * 网关不透明 Token 内省结果缓存前缀
     *
     * <p>完整 key 为 {@code auth:token:introspection:{sha256(token)}}。写方为 carlos-auth
     * （踢下线时删除）、读方为 carlos-gateway（验证时回填），两端必须同值。</p>
     */
    String AUTH_TOKEN_INTROSPECTION = "auth:token:introspection:";
}
