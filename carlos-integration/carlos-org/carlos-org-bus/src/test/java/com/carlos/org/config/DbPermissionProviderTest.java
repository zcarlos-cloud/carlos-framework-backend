package com.carlos.org.config;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.carlos.org.manager.OrgRoleManager;
import com.carlos.org.manager.OrgRoleOperationManager;
import com.carlos.org.mapper.SysOperationPermMapper;
import com.carlos.org.pojo.dto.OrgRoleDTO;
import com.carlos.org.pojo.entity.SysOperationPerm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * DbPermissionProvider 单元测试
 *
 * <p>验证：角色编码 → 角色主键解析后，从 org_role_operation + sys_operation 表查询权限码。</p>
 */
@ExtendWith(MockitoExtension.class)
class DbPermissionProviderTest {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ORG_MANAGER = "ORG_MANAGER";
    private static final String DEPT_ADMIN = "DEPT_ADMIN";

    @Mock
    private OrgRoleOperationManager orgRoleOperationManager;

    @Mock
    private SysOperationPermMapper sysOperationPermMapper;

    @Mock
    private OrgRoleManager orgRoleManager;

    @InjectMocks
    private DbPermissionProvider provider;

    // ------------------------- getPermissions(String roleCode) -------------------------

    @Test
    void should_return_empty_when_roleCode_null() {
        Set<String> result = provider.getPermissions((String) null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void should_return_empty_when_roleCode_blank() {
        // 空/空白编码不得落到 org_role 查询，也不得拼出 auth:permissions: 这种共享空 key
        assertTrue(provider.getPermissions("   ").isEmpty());
        assertTrue(provider.getPermissions("").isEmpty());
    }

    @Test
    void should_return_empty_when_role_code_not_found() {
        when(orgRoleManager.getByRoleCode("NOPE")).thenReturn(null);

        Set<String> result = provider.getPermissions("NOPE");

        assertTrue(result.isEmpty());
    }

    @Test
    void should_return_empty_when_no_operations_for_role() {
        when(orgRoleManager.getByRoleCode(ORG_MANAGER)).thenReturn(buildRole(100L, ORG_MANAGER));
        when(orgRoleOperationManager.listOperationIdsByRoleId(100L)).thenReturn(List.of());

        Set<String> result = provider.getPermissions(ORG_MANAGER);

        assertTrue(result.isEmpty());
    }

    @Test
    void should_return_perm_codes_when_operations_exist() {
        when(orgRoleManager.getByRoleCode(ORG_MANAGER)).thenReturn(buildRole(100L, ORG_MANAGER));
        when(orgRoleOperationManager.listOperationIdsByRoleId(100L))
            .thenReturn(List.of(1L, 2L, 3L));

        List<SysOperationPerm> ops = List.of(
            buildOp(1L, "user:create"),
            buildOp(2L, "user:update"),
            buildOp(3L, "user:delete")
        );
        when(sysOperationPermMapper.selectList(any(AbstractWrapper.class))).thenReturn(ops);

        Set<String> result = provider.getPermissions(ORG_MANAGER);

        assertEquals(3, result.size());
        assertTrue(result.contains("user:create"));
        assertTrue(result.contains("user:update"));
        assertTrue(result.contains("user:delete"));
    }

    @Test
    void should_filter_out_disabled_operations() {
        when(orgRoleManager.getByRoleCode(ORG_MANAGER)).thenReturn(buildRole(100L, ORG_MANAGER));
        when(orgRoleOperationManager.listOperationIdsByRoleId(100L))
            .thenReturn(List.of(1L, 2L));

        // Only operation 1 has perm_status=1 (enabled)
        List<SysOperationPerm> ops = List.of(
            buildOp(1L, "user:create")  // permStatus=1, enabled
        );
        when(sysOperationPermMapper.selectList(any(AbstractWrapper.class))).thenReturn(ops);

        Set<String> result = provider.getPermissions(ORG_MANAGER);

        assertEquals(1, result.size());
        assertTrue(result.contains("user:create"));
    }

    @Test
    void should_skip_null_perm_codes() {
        when(orgRoleManager.getByRoleCode(ORG_MANAGER)).thenReturn(buildRole(100L, ORG_MANAGER));
        when(orgRoleOperationManager.listOperationIdsByRoleId(100L))
            .thenReturn(List.of(1L, 2L));

        SysOperationPerm op1 = new SysOperationPerm();
        op1.setId(1L);
        op1.setPermCode(null);
        op1.setPermStatus(1);
        SysOperationPerm op2 = new SysOperationPerm();
        op2.setId(2L);
        op2.setPermCode("user:read");
        op2.setPermStatus(1);

        when(sysOperationPermMapper.selectList(any(AbstractWrapper.class)))
            .thenReturn(List.of(op1, op2));

        Set<String> result = provider.getPermissions(ORG_MANAGER);

        assertEquals(1, result.size());
        assertTrue(result.contains("user:read"));
    }

    // ------------------------- getPermissions(Set<String> roleCodes) -------------------------

    @Test
    void should_return_empty_when_roleCodes_null() {
        Set<String> result = provider.getPermissions((Set<String>) null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void should_return_empty_when_roleCodes_empty() {
        Set<String> result = provider.getPermissions(new HashSet<>());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void should_return_empty_when_no_role_code_resolves() {
        when(orgRoleManager.getByRoleCode("A")).thenReturn(null);
        when(orgRoleManager.getByRoleCode("B")).thenReturn(null);

        Set<String> result = provider.getPermissions(Set.of("A", "B"));

        assertTrue(result.isEmpty());
    }

    @Test
    void should_skip_unresolvable_codes_and_query_the_rest() {
        when(orgRoleManager.getByRoleCode(ORG_MANAGER)).thenReturn(buildRole(100L, ORG_MANAGER));
        when(orgRoleManager.getByRoleCode("NOPE")).thenReturn(null);
        when(orgRoleOperationManager.listOperationIdsByRoleIds(Set.of(100L)))
            .thenReturn(List.of(1L));
        when(sysOperationPermMapper.selectList(any(AbstractWrapper.class)))
            .thenReturn(List.of(buildOp(1L, "user:create")));

        Set<String> result = provider.getPermissions(Set.of(ORG_MANAGER, "NOPE"));

        assertEquals(1, result.size());
        assertTrue(result.contains("user:create"));
    }

    @Test
    void should_merge_permissions_from_multiple_roles() {
        when(orgRoleManager.getByRoleCode(DEPT_ADMIN)).thenReturn(buildRole(100L, DEPT_ADMIN));
        when(orgRoleManager.getByRoleCode(ORG_MANAGER)).thenReturn(buildRole(200L, ORG_MANAGER));
        when(orgRoleOperationManager.listOperationIdsByRoleIds(Set.of(100L, 200L)))
            .thenReturn(List.of(1L, 2L, 3L));

        List<SysOperationPerm> ops = List.of(
            buildOp(1L, "user:create"),
            buildOp(2L, "role:create"),
            buildOp(3L, "dept:read")
        );
        when(sysOperationPermMapper.selectList(any(AbstractWrapper.class))).thenReturn(ops);

        Set<String> result = provider.getPermissions(Set.of(DEPT_ADMIN, ORG_MANAGER));

        assertEquals(3, result.size());
    }

    @Test
    void should_deduplicate_permissions_from_overlapping_roles() {
        when(orgRoleManager.getByRoleCode(DEPT_ADMIN)).thenReturn(buildRole(100L, DEPT_ADMIN));
        when(orgRoleManager.getByRoleCode(ORG_MANAGER)).thenReturn(buildRole(200L, ORG_MANAGER));
        // Both roles have same operation id (duplicate shown here)
        when(orgRoleOperationManager.listOperationIdsByRoleIds(Set.of(100L, 200L)))
            .thenReturn(List.of(1L, 2L));

        List<SysOperationPerm> ops = List.of(
            buildOp(1L, "user:create"),
            buildOp(2L, "user:read")
        );
        when(sysOperationPermMapper.selectList(any(AbstractWrapper.class))).thenReturn(ops);

        Set<String> result = provider.getPermissions(Set.of(DEPT_ADMIN, ORG_MANAGER));

        assertEquals(2, result.size());
    }

    // ------------------------- 超管短路 -------------------------

    @Test
    void should_return_all_enabled_perm_codes_for_single_super_admin() {
        // 超管不走 org_role_operation 授权查询，直接返回全部启用权限码
        List<SysOperationPerm> ops = List.of(
            buildOp(1L, "user:create"),
            buildOp(2L, "user:update"),
            buildOp(3L, "user:delete")
        );
        when(sysOperationPermMapper.selectList(any(AbstractWrapper.class))).thenReturn(ops);

        Set<String> result = provider.getPermissions(SUPER_ADMIN);

        assertEquals(3, result.size());
        assertTrue(result.contains("user:create"));
        assertTrue(result.contains("user:update"));
        assertTrue(result.contains("user:delete"));
    }

    @Test
    void should_return_all_enabled_perm_codes_when_role_set_contains_super_admin() {
        // 角色集合中任一角色为超管即短路
        List<SysOperationPerm> ops = List.of(
            buildOp(1L, "user:create"),
            buildOp(2L, "dept:read")
        );
        when(sysOperationPermMapper.selectList(any(AbstractWrapper.class))).thenReturn(ops);

        Set<String> result = provider.getPermissions(Set.of(SUPER_ADMIN, ORG_MANAGER));

        assertEquals(2, result.size());
        assertTrue(result.contains("user:create"));
        assertTrue(result.contains("dept:read"));
    }

    @Test
    void should_exclude_disabled_operations_for_super_admin() {
        // 停用操作（perm_status != 1）已被 perm_status = 1 条件过滤，不会出现在结果里
        when(sysOperationPermMapper.selectList(any(AbstractWrapper.class)))
            .thenReturn(List.of(buildOp(1L, "user:create")));

        Set<String> result = provider.getPermissions(SUPER_ADMIN);

        assertEquals(1, result.size());
        assertTrue(result.contains("user:create"));
        assertFalse(result.contains("user:delete"));
    }

    // ------------------------- save / clear (no-op) -------------------------

    @Test
    void should_not_throw_on_savePermissions() {
        // These are no-ops in DbPermissionProvider
        provider.savePermissions(SUPER_ADMIN, Set.of("user:create"));
        provider.savePermissions(SUPER_ADMIN, Set.of("user:create"), 3600L);
        provider.clearCache(SUPER_ADMIN);
        assertTrue(true, "No exception thrown");
    }

    private OrgRoleDTO buildRole(Long id, String roleCode) {
        OrgRoleDTO role = new OrgRoleDTO();
        role.setId(id);
        role.setRoleCode(roleCode);
        return role;
    }

    private SysOperationPerm buildOp(Long id, String permCode) {
        SysOperationPerm op = new SysOperationPerm();
        op.setId(id);
        op.setPermCode(permCode);
        op.setPermStatus(1);
        return op;
    }
}
