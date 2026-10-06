package com.carlos.security.permission;

import com.carlos.security.permission.provider.CachedPermissionProvider;
import com.carlos.security.permission.provider.RedisPermissionProvider;

import java.util.Set;

/**
 * <p>
 * 权限提供者接口
 * </p>
 *
 * <p>定义角色权限加载的统一抽象，支持多种实现：</p>
 * <ul>
 *   <li>{@link RedisPermissionProvider} - Redis 缓存实现</li>
 *   <li>{@link CachedPermissionProvider} - 本地缓存包装</li>
 *   <li>自定义实现 - 从数据库或其他数据源加载</li>
 * </ul>
 *
 * <p><b>角色标识一律使用角色编码（roleCode），不使用角色 ID。</b></p>
 * <p>原因：网关 OpaqueTokenValidator 从 introspection 响应的 role_ids 中读取角色标识并原样透传到
 * X-User-Roles 头，其取值是角色<b>编码</b>（如 {@code SUPER_ADMIN}）。若此处以数字角色 ID 为契约，
 * 就需要在调用链上再插一次「编码 → ID」的翻译，而这次翻译既无权威来源（下游拿不到 introspection
 * 响应）、又没有类型约束，历史上正是它导致了 {code Long.valueOf("SUPER_ADMIN")} 抛异常、
 * 身份静默丢失。角色编码在 org_role.role_code 上有唯一约束，可直接作为缓存 key 与 @PreAuthorize
 * 的 hasRole() 取值。实现方若底层数据以 ID 存储（如 org_role_operation），应在实现内部自行完成
 * 「编码 → ID」解析，不要把这个翻译泄漏到接口契约上。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
public interface PermissionProvider {

    /**
     * 获取指定角色的权限集合
     *
     * @param roleCode 角色编码（如 "SUPER_ADMIN"）
     * @return 权限标识集合（如：["user:create", "order:read"]），
     *         如果角色无权限或不存在返回空集合（不是 null）
     */
    Set<String> getPermissions(String roleCode);

    /**
     * 批量获取多个角色的权限集合
     * <p>合并所有角色的权限，自动去重</p>
     *
     * @param roleCodes 角色编码集合
     * @return 合并后的权限集合
     */
    Set<String> getPermissions(Set<String> roleCodes);

    /**
     * 保存权限到缓存
     *
     * @param roleCode    角色编码
     * @param permissions 权限集合
     */
    void savePermissions(String roleCode, Set<String> permissions);

    /**
     * 保存权限到缓存（指定过期时间）
     *
     * @param roleCode    角色编码
     * @param permissions 权限集合
     * @param ttl         过期时间（秒）
     */
    void savePermissions(String roleCode, Set<String> permissions, long ttl);

    /**
     * 清除指定角色的缓存
     *
     * @param roleCode 角色编码
     */
    void clearCache(String roleCode);
}
