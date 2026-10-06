package com.carlos.security.permission.evaluator;

import com.carlos.security.auth.UserContext;
import com.carlos.security.auth.UserContextAuthenticationToken;
import com.carlos.security.permission.DataScopeProvider;
import com.carlos.security.permission.ScopedResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link CarlosPermissionEvaluator} 单元测试
 *
 * <p>覆盖功能权限（资源字符串）与行级数据权限（{@link ScopedResource} 实体）两条路径。</p>
 *
 * @author carlos
 */
class CarlosPermissionEvaluatorTest {

    private Authentication auth(UserContext ctx) {
        // 两参构造（带权限）会置为「已认证」，评估器要求 isAuthenticated() 为真
        return new UserContextAuthenticationToken(ctx, ctx.getAuthorities());
    }

    private UserContext user(Set<String> permissions, Set<String> roles, Long userId) {
        UserContext ctx = new UserContext();
        ctx.setUserId(userId);
        ctx.setPermissions(new HashSet<>(permissions));
        ctx.setRoleCodes(new HashSet<>(roles));
        return ctx;
    }

    private ScopedResource resource(Long ownerId, Long deptId, Long tenantId) {
        return new ScopedResource() {
            @Override
            public Long getOwnerId() {
                return ownerId;
            }

            @Override
            public Long getDeptId() {
                return deptId;
            }

            @Override
            public Long getTenantId() {
                return tenantId;
            }
        };
    }

    @Test
    @DisplayName("功能权限：资源字符串拼成 resource:action 校验")
    void should_check_functional_permission() {
        CarlosPermissionEvaluator evaluator = new CarlosPermissionEvaluator();
        Authentication authentication = auth(user(Set.of("org:user:create"), Set.of("ORG_MANAGER"), 1L));

        assertTrue(evaluator.hasPermission(authentication, "org:user", "create"));
        assertFalse(evaluator.hasPermission(authentication, "org:user", "delete"));
    }

    @Test
    @DisplayName("功能权限：未认证直接拒绝")
    void should_deny_when_not_authenticated() {
        CarlosPermissionEvaluator evaluator = new CarlosPermissionEvaluator();
        assertFalse(evaluator.hasPermission(auth(user(Set.of(), Set.of(), null)), "org:user", "create"));
        assertFalse(evaluator.hasPermission(null, "org:user", "create"));
    }

    @Test
    @DisplayName("行级：实体实现 ScopedResource 时委托 DataScopeProvider")
    void should_check_row_level_via_provider() {
        DataScopeProvider provider = mock(DataScopeProvider.class);
        when(provider.canAccessData(7L, 20L, 0L)).thenReturn(true);
        when(provider.canAccessData(8L, 20L, 0L)).thenReturn(false);
        CarlosPermissionEvaluator evaluator = new CarlosPermissionEvaluator(provider);
        Authentication authentication = auth(user(Set.of(), Set.of("USER"), 1L));

        assertTrue(evaluator.hasPermission(authentication, resource(7L, 20L, 0L), "read"));
        assertFalse(evaluator.hasPermission(authentication, resource(8L, 20L, 0L), "read"));
    }

    @Test
    @DisplayName("行级：超管短路放行")
    void should_allow_row_level_for_super_admin() {
        DataScopeProvider provider = mock(DataScopeProvider.class);
        CarlosPermissionEvaluator evaluator = new CarlosPermissionEvaluator(provider);
        Authentication authentication = auth(user(Set.of(), Set.of("SUPER_ADMIN"), 1L));

        assertTrue(evaluator.hasPermission(authentication, resource(999L, 888L, 0L), "read"));
    }

    @Test
    @DisplayName("行级：未装配 DataScopeProvider 时保守拒绝")
    void should_deny_row_level_without_provider() {
        CarlosPermissionEvaluator evaluator = new CarlosPermissionEvaluator();
        Authentication authentication = auth(user(Set.of(), Set.of("USER"), 1L));

        assertFalse(evaluator.hasPermission(authentication, resource(7L, 20L, 0L), "read"));
    }

    @Test
    @DisplayName("数据级（targetId 重载）：仍未实现，传 targetId 一律拒绝")
    void should_deny_target_id_overload() {
        CarlosPermissionEvaluator evaluator = new CarlosPermissionEvaluator();
        Authentication authentication = auth(user(Set.of("org:user:create"), Set.of("USER"), 1L));

        assertFalse(evaluator.hasPermission(authentication, 100L, "org:user", "create"));
        // targetId 为 null 时退化为功能权限校验
        assertTrue(evaluator.hasPermission(authentication, null, "org:user", "create"));
    }
}
