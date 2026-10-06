package com.carlos.security.permission;

import com.carlos.security.auth.UserContext;
import com.carlos.security.auth.UserContextAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * {@link PermissionChecker} 多维鉴权单元测试
 *
 * @author carlos
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionCheckerTest {

    @Mock
    private ObjectProvider<DataScopeProvider> dataScopeProviderProvider;

    @Mock
    private DataScopeProvider dataScopeProvider;

    private PermissionChecker checker;

    @BeforeEach
    void setUp() {
        checker = new PermissionChecker(dataScopeProviderProvider);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(Set<String> permissions, Set<String> roles, Long userId, Long deptId, Long tenantId) {
        UserContext ctx = new UserContext();
        ctx.setUserId(userId);
        ctx.setUsername("tester");
        ctx.setPermissions(new java.util.HashSet<>(permissions));
        ctx.setRoleCodes(new java.util.HashSet<>(roles));
        ctx.setDeptId(deptId);
        ctx.setTenantId(tenantId);
        SecurityContextHolder.getContext().setAuthentication(new UserContextAuthenticationToken(ctx));
    }

    @Test
    @DisplayName("功能维度：单权限 / 任一 / 全部")
    void should_check_functional_permissions() {
        login(Set.of("org:user:create", "org:user:update"), Set.of("ORG_MANAGER"), 1L, 10L, 0L);

        assertTrue(checker.hasPermission("org:user:create"));
        assertFalse(checker.hasPermission("org:user:delete"));
        assertTrue(checker.hasAnyPermissions("org:user:delete", "org:user:update"));
        assertFalse(checker.hasAnyPermissions("org:role:create", "org:user:delete"));
        assertTrue(checker.hasAllPermissions("org:user:create", "org:user:update"));
        assertFalse(checker.hasAllPermissions("org:user:create", "org:user:delete"));
    }

    @Test
    @DisplayName("功能维度：多级通配 org:user:*")
    void should_support_wildcard_permission() {
        login(Set.of("org:user:*"), Set.of("ORG_MANAGER"), 1L, 10L, 0L);

        assertTrue(checker.hasPermission("org:user:create"));
        assertFalse(checker.hasPermission("org:role:create"));
    }

    @Test
    @DisplayName("角色维度：单角色 / 任一 / 全部")
    void should_check_roles() {
        login(Set.of(), Set.of("ORG_MANAGER", "AUDITOR"), 1L, 10L, 0L);

        assertTrue(checker.hasRole("ORG_MANAGER"));
        assertFalse(checker.hasRole("SUPER_ADMIN"));
        assertTrue(checker.hasAnyRoles("SUPER_ADMIN", "AUDITOR"));
        assertTrue(checker.hasAllRoles("ORG_MANAGER", "AUDITOR"));
        assertFalse(checker.hasAllRoles("ORG_MANAGER", "SUPER_ADMIN"));
    }

    @Test
    @DisplayName("身份维度：超管短路 / 本人判定")
    void should_check_identity() {
        login(Set.of(), Set.of("SUPER_ADMIN"), 1L, 10L, 0L);
        assertTrue(checker.isSuperAdmin());
        assertTrue(checker.hasPermission("anything:at:all"));

        login(Set.of(), Set.of("USER"), 42L, 10L, 0L);
        assertFalse(checker.isSuperAdmin());
        assertTrue(checker.isSelf(42L));
        assertFalse(checker.isSelf(43L));
    }

    @Test
    @DisplayName("租户维度：同租户 / 数据无租户视为公共")
    void should_check_tenant() {
        login(Set.of(), Set.of("USER"), 1L, 10L, 0L);

        assertTrue(checker.hasTenant());
        assertTrue(checker.sameTenant(0L));
        assertFalse(checker.sameTenant(99L));
        assertTrue(checker.sameTenant(null), "数据无租户视为公共数据");
    }

    @Test
    @DisplayName("数据维度：委托给 DataScopeProvider")
    void should_delegate_data_scope() {
        when(dataScopeProviderProvider.getIfAvailable()).thenReturn(dataScopeProvider);
        when(dataScopeProvider.canAccessData(42L, 10L, null)).thenReturn(true);
        when(dataScopeProvider.currentScopeCode()).thenReturn(3);
        login(Set.of(), Set.of("USER"), 1L, 10L, 0L);

        assertTrue(checker.canAccessData(42L, 10L));
        assertTrue(checker.hasScope(3));
        assertTrue(checker.scopeAtLeast(3));
        assertFalse(checker.scopeAtLeast(2));
    }

    @Test
    @DisplayName("数据维度：未装配 DataScopeProvider 时保守拒绝")
    void should_deny_data_scope_without_provider() {
        when(dataScopeProviderProvider.getIfAvailable()).thenReturn(null);
        login(Set.of(), Set.of("USER"), 1L, 10L, 0L);

        assertFalse(checker.canAccessData(1L, 10L));
    }

    @Test
    @DisplayName("数据维度：对 ScopedResource 实体做行级判定")
    void should_check_scoped_resource() {
        when(dataScopeProviderProvider.getIfAvailable()).thenReturn(dataScopeProvider);
        when(dataScopeProvider.canAccessData(7L, 20L, 0L)).thenReturn(true);
        login(Set.of(), Set.of("USER"), 1L, 10L, 0L);

        ScopedResource resource = new ScopedResource() {
            @Override
            public Long getOwnerId() {
                return 7L;
            }

            @Override
            public Long getDeptId() {
                return 20L;
            }

            @Override
            public Long getTenantId() {
                return 0L;
            }
        };

        assertTrue(checker.canAccess(resource));
        assertFalse(checker.canAccess(null));
    }
}
