package com.carlos.security.auth;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户上下文
 * </p>
 *
 * <p>封装当前请求的用户信息和权限，存储在 SecurityContext 中。</p>
 * <p>数据来源于网关传递的 HTTP Header（定义在 {@link com.carlos.core.constant.HttpHeadersConstant}）：</p>
 * <pre>
 * X-User-Id: 10001
 * X-User-Name: admin
 * X-User-Roles: SUPER_ADMIN,ORG_MANAGER
 * </pre>
 *
 * <p>该对象作为 Spring Security Authentication 的 principal，支持：</p>
 * <ul>
 *   <li>{@link org.springframework.security.core.annotation.AuthenticationPrincipal} 注入</li>
 *   <li>{@code @PreAuthorize("hasPermission('user', 'read')")} 权限表达式</li>
 *   <li>{@code @PreAuthorize("@permissionEvaluator.isOwner(#id, 'user')")} 数据权限</li>
 * </ul>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
@Data
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 超级管理员角色编码的默认值
     *
     * <p>{@link com.carlos.security.config.ResourceServerProperties.AuthProperties#getSuperRoleCode()}
     * 的默认值与此一致。历史上 {@link #isSuperAdmin()} 判定的是「角色 ID 为 1」，但角色主键是雪花 ID
     * （库中真实的超级管理员 id 为 2076141407304175616），该条件永不成立，故改为按配置的角色编码判定。</p>
     */
    public static final String DEFAULT_SUPER_ROLE_CODE = "SUPER_ADMIN";

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门ID集合（多部门场景）
     */
    private Set<Long> deptIds = new HashSet<>();

    /**
     * 角色编码集合（如：["SUPER_ADMIN", "ORG_MANAGER"]）
     *
     * <p><b>这里承载的是角色编码，不是角色 ID。</b>网关 OpaqueTokenValidator 从 introspection 响应的
     * role_ids 中读取角色标识（数字 ID 或编码都可能出现），原样透传到 X-User-Roles 头，因此下游按
     * 「字符串标识」处理，不做数字解析。角色编码是唯一事实源 —— PermissionProvider 的缓存 key
     * （auth:permissions:{roleCode}）、{@link #getAuthorities()} 生成的 ROLE_ 前缀权限、
     * {@code @PreAuthorize("hasRole('XXX')")} 的取值，全部以此为准。</p>
     */
    private Set<String> roleCodes = new HashSet<>();

    /**
     * 超级管理员角色编码
     *
     * <p>由配置 {@code carlos.resource-server.auth.super-role-code} 指定
     * （默认 {@value #DEFAULT_SUPER_ROLE_CODE}），供 {@link #isSuperAdmin()} 判定。</p>
     */
    private String superRoleCode = DEFAULT_SUPER_ROLE_CODE;

    /**
     * 权限集合（如：["user:create", "order:read"]）
     */
    private Set<String> permissions = new HashSet<>();

    /**
     * 获取 Spring Security 需要的 GrantedAuthority 集合
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 从权限生成 GrantedAuthority
        Set<SimpleGrantedAuthority> authorities = permissions.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());

        // 角色权限（ROLE_ 前缀），取值为角色编码，与 Spring Security 的 hasRole('XXX') 约定一致
        roleCodes.forEach(roleCode ->
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode)));

        return authorities;
    }

    /**
     * 检查是否有指定权限
     *
     * @param permission 权限标识（如 "user:create"）
     * @return true 如果有权限
     */
    public boolean hasPermission(String permission) {
        if (permission == null) {
            return false;
        }

        // 超级管理员：拥有通配权限 "*" 或持有超管角色，一律放行
        if (isSuperAdmin()) {
            return true;
        }

        // 精确匹配
        if (permissions.contains(permission)) {
            return true;
        }

        // 通配符匹配：支持多级前缀。
        // 例：要求 perm = "org:user:create" 时，依次检查 "org:*"、"org:user:*"；
        //     要求 perm = "user:create" 时，检查 "user:*"（兼容两段式旧权限码）。
        String[] parts = permission.split(":");
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                prefix.append(':');
            }
            prefix.append(parts[i]);
            if (permissions.contains(prefix + ":*")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查是否有指定角色
     *
     * @param roleCode 角色编码（如 "SUPER_ADMIN"）
     * @return true 如果有该角色
     */
    public boolean hasRole(String roleCode) {
        return roleCode != null && roleCodes != null && roleCodes.contains(roleCode);
    }

    /**
     * 检查是否有任意一个角色
     *
     * @param roleCodes 角色编码集合
     * @return true 如果有任意一个角色
     */
    public boolean hasAnyRole(Set<String> roleCodes) {
        if (this.roleCodes == null || roleCodes == null) {
            return false;
        }
        return roleCodes.stream().anyMatch(this.roleCodes::contains);
    }

    /**
     * 检查是否是超级管理员
     *
     * <p>拥有通配权限 {@code "*"}，或持有配置的超级管理员角色编码
     * （{@link #superRoleCode}，默认 {@value #DEFAULT_SUPER_ROLE_CODE}）。</p>
     */
    public boolean isSuperAdmin() {
        return permissions.contains("*") || hasRole(superRoleCode);
    }
}
