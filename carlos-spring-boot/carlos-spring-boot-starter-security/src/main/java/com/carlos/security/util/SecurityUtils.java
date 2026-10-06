package com.carlos.security.util;

import com.carlos.security.auth.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * 安全工具类
 * </p>
 *
 * <p>提供获取当前登录用户信息的便捷方法。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 1. 获取当前用户上下文
 * UserContext user = SecurityUtils.getCurrentUser();
 *
 * // 2. 获取当前用户ID
 * Long userId = SecurityUtils.getCurrentUserId();
 *
 * // 3. 检查是否有权限
 * if (SecurityUtils.hasPermission("user:update")) {
 *     // 执行业务逻辑
 * }
 *
 * // 4. 使用 Optional 避免空值判断
 * SecurityUtils.getCurrentUserOpt().ifPresent(user -> {
 *     log.info("Current user: {}", user.getUsername());
 * });
 * }</pre>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see UserContext
 */
@Slf4j
public final class SecurityUtils {

    private SecurityUtils() {
        // 工具类禁止实例化
    }

    /**
     * 获取当前 SecurityContext
     *
     * @return SecurityContext，可能为 null
     */
    public static SecurityContext getContext() {
        return SecurityContextHolder.getContext();
    }

    /**
     * 获取当前认证信息
     *
     * @return Authentication，可能为 null
     */
    public static Authentication getAuthentication() {
        SecurityContext context = getContext();
        if (context == null) {
            return null;
        }
        return context.getAuthentication();
    }

    /**
     * 获取当前用户上下文（UserContext）
     *
     * @return UserContext，如果未登录返回 null
     */
    public static UserContext getCurrentUser() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserContext) {
            return (UserContext) principal;
        }

        return null;
    }

    /**
     * 获取当前用户上下文（Optional 包装）
     *
     * @return Optional<UserContext>
     */
    public static Optional<UserContext> getCurrentUserOpt() {
        return Optional.ofNullable(getCurrentUser());
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，如果未登录返回 null
     */
    public static Long getCurrentUserId() {
        UserContext user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前用户ID（long 类型，未登录时返回 0）
     *
     * @return 用户ID，如果未登录返回 0
     */
    public static long getCurrentUserIdOrDefault() {
        Long userId = getCurrentUserId();
        return userId != null ? userId : 0L;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，如果未登录返回 null
     */
    public static String getCurrentUsername() {
        UserContext user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 获取当前用户权限集合
     *
     * @return 权限集合，如果未登录返回空集合
     */
    public static Set<String> getCurrentUserPermissions() {
        UserContext user = getCurrentUser();
        if (user == null || user.getPermissions() == null) {
            return Collections.emptySet();
        }
        return user.getPermissions();
    }

    /**
     * 获取当前用户角色编码集合
     *
     * @return 角色编码集合，如果未登录返回空集合
     */
    public static Set<String> getCurrentUserRoles() {
        UserContext user = getCurrentUser();
        if (user == null || user.getRoleCodes() == null) {
            return Collections.emptySet();
        }
        return user.getRoleCodes();
    }

    /**
     * 检查当前用户是否有指定权限
     *
     * @param permission 权限标识（如 "user:create"）
     * @return true 如果有权限
     */
    public static boolean hasPermission(String permission) {
        if (permission == null || permission.isEmpty()) {
            return false;
        }

        UserContext user = getCurrentUser();
        if (user == null) {
            return false;
        }

        return user.hasPermission(permission);
    }

    /**
     * 检查当前用户是否有任意一个权限
     *
     * @param permissions 权限列表
     * @return true 如果有任意一个权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }

        UserContext user = getCurrentUser();
        if (user == null) {
            return false;
        }

        for (String permission : permissions) {
            if (user.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查当前用户是否有所有权限
     *
     * @param permissions 权限列表
     * @return true 如果有所有权限
     */
    public static boolean hasAllPermissions(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }

        UserContext user = getCurrentUser();
        if (user == null) {
            return false;
        }

        for (String permission : permissions) {
            if (!user.hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查当前用户是否有指定角色
     *
     * @param roleCode 角色编码（如 "SUPER_ADMIN"）
     * @return true 如果有该角色
     */
    public static boolean hasRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return false;
        }

        UserContext user = getCurrentUser();
        if (user == null) {
            return false;
        }

        return user.hasRole(roleCode);
    }

    /**
     * 检查当前用户是否有任意一个角色
     *
     * @param roleCodes 角色编码列表
     * @return true 如果有任意一个角色
     */
    public static boolean hasAnyRole(String... roleCodes) {
        if (roleCodes == null || roleCodes.length == 0) {
            return false;
        }

        UserContext user = getCurrentUser();
        if (user == null) {
            return false;
        }

        for (String roleCode : roleCodes) {
            if (user.hasRole(roleCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查当前用户是否是超级管理员
     *
     * @return true 如果是超级管理员
     */
    public static boolean isSuperAdmin() {
        UserContext user = getCurrentUser();
        if (user == null) {
            return false;
        }

        return user.isSuperAdmin();
    }

    /**
     * 检查当前用户是否已登录
     *
     * @return true 如果已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof UserContext;
    }

    /**
     * 检查当前用户是否未登录（匿名用户）
     *
     * @return true 如果未登录
     */
    public static boolean isAnonymous() {
        return !isAuthenticated();
    }

    /**
     * 清除当前 SecurityContext
     * <p>通常在登出时使用</p>
     */
    public static void clearContext() {
        SecurityContextHolder.clearContext();
        log.debug("Security context cleared");
    }
}
