package com.carlos.security.permission;

import com.carlos.security.auth.UserContext;
import com.carlos.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * 多维鉴权表达式 Bean（SpEL 名 {@code ps}）
 * </p>
 *
 * <p>供 {@code @PreAuthorize} 从多个维度做鉴权。SpEL 里用 {@code @beanName.method(...)}
 * 引用容器 Bean，本类即名为 {@code ps} 的 Bean（与若依的 {@code @ss} 是同一机制）。</p>
 *
 * <h3>五个维度</h3>
 * <ul>
 *   <li><b>功能权限</b>：{@link #hasPermission} / {@link #hasAnyPermissions} / {@link #hasAllPermissions}</li>
 *   <li><b>角色</b>：{@link #hasRole} / {@link #hasAnyRoles} / {@link #hasAllRoles}</li>
 *   <li><b>身份</b>：{@link #isSuperAdmin} / {@link #isAuthenticated}</li>
 *   <li><b>租户</b>：{@link #sameTenant} / {@link #hasTenant}</li>
 *   <li><b>数据（行级）</b>：{@link #isSelf} / {@link #canAccessData} / {@link #canAccess} /
 *       {@link #hasScope} / {@link #scopeAtLeast}</li>
 * </ul>
 *
 * <h3>用法</h3>
 * <pre>
 * // 任一权限
 * &#64;PreAuthorize("@ps.hasAnyPermissions('org:user:create', 'org:user:update')")
 *
 * // 功能权限 + 数据维度：本人 或 有管理权限
 * &#64;PreAuthorize("@ps.isSelf(#userId) or @ps.hasPermission('org:user:update')")
 *
 * // 对实体做行级判定（实体实现 ScopedResource）
 * &#64;PreAuthorize("@ps.canAccess(#order) and @ps.hasPermission('order:update')")
 *
 * // 租户隔离
 * &#64;PreAuthorize("@ps.sameTenant(#tenantId)")
 * </pre>
 *
 * <p><b>数据维度依赖 {@link DataScopeProvider}</b>：未提供实现（如未引入 org 模块的轻量服务）时，
 * 数据维度方法一律保守拒绝，不会误放行。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-09-29
 */
@Slf4j
@RequiredArgsConstructor
public class PermissionChecker {

    /**
     * 数据权限提供者（可选）。未装配时数据维度一律拒绝。
     */
    private final ObjectProvider<DataScopeProvider> dataScopeProvider;

    // ==================== 功能权限维度 ====================

    /**
     * 是否拥有指定功能权限。
     *
     * @param permission 权限码，如 {@code org:user:create}；支持 {@code *} 与多级通配 {@code org:user:*}
     * @return 是否拥有
     */
    public boolean hasPermission(String permission) {
        return SecurityUtils.hasPermission(permission);
    }

    /**
     * 是否拥有**任意一个**功能权限（OR）。
     *
     * @param permissions 权限码数组
     * @return 任一命中即 true；入参为空返回 false
     */
    public boolean hasAnyPermissions(String... permissions) {
        return SecurityUtils.hasAnyPermission(permissions);
    }

    /**
     * 是否**全部拥有**指定功能权限（AND）。
     *
     * @param permissions 权限码数组
     * @return 全部命中才 true；入参为空返回 false
     */
    public boolean hasAllPermissions(String... permissions) {
        return SecurityUtils.hasAllPermissions(permissions);
    }

    // ==================== 角色维度 ====================

    /**
     * 是否拥有指定角色。
     *
     * @param roleCode 角色编码，如 {@code SUPER_ADMIN}
     * @return 是否拥有
     */
    public boolean hasRole(String roleCode) {
        return SecurityUtils.hasRole(roleCode);
    }

    /**
     * 是否拥有**任意一个**角色（OR）。
     *
     * @param roleCodes 角色编码数组
     * @return 任一命中即 true
     */
    public boolean hasAnyRoles(String... roleCodes) {
        return SecurityUtils.hasAnyRole(roleCodes);
    }

    /**
     * 是否**全部拥有**指定角色（AND）。
     *
     * @param roleCodes 角色编码数组
     * @return 全部命中才 true
     */
    public boolean hasAllRoles(String... roleCodes) {
        if (roleCodes == null || roleCodes.length == 0) {
            return false;
        }
        Set<String> current = SecurityUtils.getCurrentUserRoles();
        return current.containsAll(Arrays.asList(roleCodes));
    }

    // ==================== 身份维度 ====================

    /**
     * 是否超级管理员（拥有通配权限 {@code *} 或持有超管角色）。
     *
     * @return 是否超管
     */
    public boolean isSuperAdmin() {
        return SecurityUtils.isSuperAdmin();
    }

    /**
     * 是否已认证（有登录人）。
     *
     * @return 是否已认证
     */
    public boolean isAuthenticated() {
        return SecurityUtils.isAuthenticated();
    }

    /**
     * 登录人 ID 是否等于给定主体（「本人」身份判定）。
     *
     * @param userId 待比对用户 ID
     * @return 相等则 true；登录人或入参为空返回 false
     */
    public boolean isSelf(Long userId) {
        if (userId == null) {
            return false;
        }
        Long current = SecurityUtils.getCurrentUserId();
        return current != null && current.equals(userId);
    }

    // ==================== 租户维度 ====================

    /**
     * 登录人是否绑定了租户。
     *
     * @return 是否绑定租户
     */
    public boolean hasTenant() {
        UserContext user = currentUserOrNull();
        return user != null && user.getTenantId() != null;
    }

    /**
     * 给定租户是否与登录人同租户（租户隔离判定）。
     *
     * <p>登录人无租户时保守返回 false；数据无租户（入参 null）时视为公共数据，放行。</p>
     *
     * @param tenantId 数据归属租户 ID
     * @return 是否同租户
     */
    public boolean sameTenant(Long tenantId) {
        if (tenantId == null) {
            return true;
        }
        UserContext user = currentUserOrNull();
        return user != null && tenantId.equals(user.getTenantId());
    }

    // ==================== 数据（行级）维度 ====================

    /**
     * 能否访问指定归属人的数据（仅本人维度）。
     *
     * @param ownerId 数据归属人 ID
     * @return 可访问则 true
     */
    public boolean isSelfData(Long ownerId) {
        return canAccessData(ownerId, null);
    }

    /**
     * 能否访问「归属 ownerId / 部门 deptId / 租户 tenantId」的数据（按当前用户数据范围判定）。
     *
     * @param ownerId  归属人 ID，可为 null
     * @param deptId   归属部门 ID，可为 null
     * @param tenantId 归属租户 ID，可为 null
     * @return 可访问则 true；无 {@link DataScopeProvider} 时保守拒绝
     */
    public boolean canAccessData(Long ownerId, Long deptId, Long tenantId) {
        if (SecurityUtils.isSuperAdmin()) {
            return true;
        }
        DataScopeProvider provider = dataScopeProvider.getIfAvailable();
        if (provider == null) {
            log.debug("DataScopeProvider 未装配，数据维度鉴权保守拒绝");
            return false;
        }
        return provider.canAccessData(ownerId, deptId, tenantId);
    }

    /**
     * 能否访问「归属 ownerId / 部门 deptId」的数据（不含租户维度）。
     *
     * @param ownerId 归属人 ID，可为 null
     * @param deptId  归属部门 ID，可为 null
     * @return 可访问则 true
     */
    public boolean canAccessData(Long ownerId, Long deptId) {
        return canAccessData(ownerId, deptId, null);
    }

    /**
     * 能否访问某条数据（实体需实现 {@link ScopedResource}）。
     *
     * @param resource 数据实体
     * @return 可访问则 true；resource 为 null 时保守拒绝
     */
    public boolean canAccess(ScopedResource resource) {
        if (resource == null) {
            return false;
        }
        return canAccessData(resource.getOwnerId(), resource.getDeptId(), resource.getTenantId());
    }

    /**
     * 当前用户的有效数据范围是否**正好等于**给定编码。
     *
     * @param scopeCode 数据范围编码（1=全部, 2=本部门及子部门, 3=仅本部门, 4=仅本人, 5=自定义）
     * @return 相等则 true
     */
    public boolean hasScope(int scopeCode) {
        Integer current = currentScopeCode();
        return current != null && current == scopeCode;
    }

    /**
     * 当前用户的数据范围是否**至少和给定范围一样宽**（越小越宽）。
     *
     * <p>例：{@code scopeAtLeast(3)} 在范围为 1/2/3 时为 true，为 4/5 时为 false。</p>
     *
     * @param scopeCode 参照范围编码
     * @return 至少一样宽则 true
     */
    public boolean scopeAtLeast(int scopeCode) {
        Integer current = currentScopeCode();
        return current != null && current <= scopeCode;
    }

    /**
     * 当前用户的有效数据范围编码。
     *
     * @return 范围编码；无法判定返回 null
     */
    public Integer currentScopeCode() {
        if (SecurityUtils.isSuperAdmin()) {
            return 1;
        }
        DataScopeProvider provider = dataScopeProvider.getIfAvailable();
        return provider == null ? null : provider.currentScopeCode();
    }

    // ==================== 组合维度 ====================

    /**
     * 是否「拥有指定权限 或 是该数据归属人」——常见的「本人 or 管理员」组合。
     *
     * @param permission 功能权限码
     * @param ownerId    数据归属人 ID
     * @return 满足其一即 true
     */
    public boolean hasPermissionOrSelf(String permission, Long ownerId) {
        return hasPermission(permission) || isSelf(ownerId);
    }

    // ==================== 内部 ====================

    private UserContext currentUserOrNull() {
        return SecurityUtils.getCurrentUserOpt().orElse(null);
    }
}
