package com.carlos.security.permission.evaluator;

import com.carlos.security.auth.UserContext;
import com.carlos.security.permission.DataScopeProvider;
import com.carlos.security.permission.ScopedResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;

/**
 * <p>
 * Spring Security 权限评估器
 * </p>
 *
 * <p>支持 SpEL 表达式的 hasPermission() 方法：</p>
 * <pre>
 * // Controller 层方法权限控制
 * @PreAuthorize("hasPermission('user', 'create')")
 * public ResponseEntity<?> createUser(...) { }
 *
 * @PreAuthorize("hasPermission('user', 'update')")
 * public ResponseEntity<?> updateUser(...) { }
 *
 * @PreAuthorize("hasPermission(#userId, 'user', 'delete')")
 * public ResponseEntity<?> deleteUser(@PathVariable Long userId) { }
 *
 * // 组合表达式
 * @PreAuthorize("hasRole('ADMIN') or hasPermission('user', 'read')")
 * public ResponseEntity<?> listUsers(...) { }
 * </pre>
 *
 * <h3>权限字符串格式：</h3>
 * <ul>
 *   <li><b>简单格式：</b>"resource:action"（如 "user:create"）</li>
 *   <li><b>通配符：</b>"resource:*"（拥有 resource 的所有权限）</li>
 *   <li><b>全通配：</b>"*"（超级管理员）</li>
 * </ul>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
@Slf4j
public class CarlosPermissionEvaluator implements PermissionEvaluator {

    /**
     * 数据权限提供者（可空）
     *
     * <p>为 null 时行级（{@link ScopedResource} 目标）校验保守拒绝，功能权限校验不受影响。</p>
     */
    private final DataScopeProvider dataScopeProvider;

    public CarlosPermissionEvaluator() {
        this(null);
    }

    public CarlosPermissionEvaluator(DataScopeProvider dataScopeProvider) {
        this.dataScopeProvider = dataScopeProvider;
    }

    /**
     * 检查是否有指定权限
     *
     * <p>两种目标形态：</p>
     * <ul>
     *   <li><b>字符串资源</b>（{@code hasPermission('org:user','create')}）：做功能权限校验，
     *       拼成 {@code org:user:create} 后查用户权限集合</li>
     *   <li><b>实体</b>（{@code hasPermission(#order,'update')}，实体实现 {@link ScopedResource}）：
     *       做行级数据权限校验（本人 / 部门 / 租户维度）</li>
     * </ul>
     *
     * @param authentication 当前认证信息
     * @param targetDomainObject 目标对象（资源类型字符串，或 {@link ScopedResource} 实体）
     * @param permission 权限操作（如 "create", "read"）
     * @return true 如果有权限
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Permission denied: not authenticated");
            return false;
        }

        if (targetDomainObject == null || permission == null) {
            log.debug("Permission denied: target or permission is null");
            return false;
        }

        // 从认证信息获取 UserContext
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserContext userContext)) {
            log.debug("Permission denied: principal is not UserContext");
            return false;
        }

        // ① 目标是数据实体：走行级数据权限（本人/部门/租户）
        if (targetDomainObject instanceof ScopedResource resource) {
            return checkDataScope(userContext, resource);
        }

        // ② 目标是资源字符串：走功能权限
        String requiredPermission = targetDomainObject.toString() + ":" + permission.toString();

        // 检查权限
        boolean hasPermission = userContext.hasPermission(requiredPermission);

        if (!hasPermission) {
            log.debug("Permission denied: userId={}, required={}", userContext.getUserId(), requiredPermission);
        }

        return hasPermission;
    }

    /**
     * 行级数据权限校验
     *
     * @param userContext 当前用户上下文
     * @param resource    数据实体
     * @return 是否可访问
     */
    private boolean checkDataScope(UserContext userContext, ScopedResource resource) {
        if (userContext.isSuperAdmin()) {
            return true;
        }
        if (dataScopeProvider == null) {
            log.warn("Row-level permission check skipped: no DataScopeProvider, denying. userId={}",
                userContext.getUserId());
            return false;
        }
        boolean allowed = dataScopeProvider.canAccessData(
            resource.getOwnerId(), resource.getDeptId(), resource.getTenantId());
        if (!allowed) {
            log.debug("Row-level permission denied: userId={}, ownerId={}, deptId={}",
                userContext.getUserId(), resource.getOwnerId(), resource.getDeptId());
        }
        return allowed;
    }

    /**
     * 检查是否有指定权限（带目标ID）
     * <p>用于数据级权限控制</p>
     *
     * @param authentication 当前认证信息
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @param permission 权限操作
     * @return true 如果有权限
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        // P1-005 修复：数据级权限（row-level）尚未实现，不能简单忽略 targetId。
        // 若调用方传入 targetId，默认拒绝，避免拥有类型权限的用户越权访问全部实例。
        if (targetId != null) {
            log.warn("Row-level permission check is not implemented yet, denying access: targetId={}, targetType={}",
                targetId, targetType);
            return false;
        }
        return hasPermission(authentication, targetType, permission);
    }
}
