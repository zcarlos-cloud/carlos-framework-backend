package com.carlos.org.apiimpl;

import com.carlos.core.response.Result;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.manager.OrgDepartmentManager;
import com.carlos.org.manager.OrgRoleManager;
import com.carlos.org.manager.OrgUserManager;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.position.apiimpl.ApiOrgUserPositionImpl;
import com.carlos.org.position.manager.OrgUserPositionManager;
import com.carlos.org.position.pojo.dto.OrgUserPositionDTO;
import com.carlos.org.service.OrgUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 显式租户（{@code ...AndTenant}）Feign 端点的 apiimpl 单元测试
 *
 * <p>无 {@code HttpServletRequest} 上下文的调用方（carlos-job 调度线程等）无法透传租户，
 * 因此这些端点在 {@code tenantId == null} 时必须快速失败（不查库），
 * 并把 tenantId 一路传到 Manager 的显式租户方法，而不是回落到平台租户。</p>
 *
 * @author Carlos
 */
class ApiOrgTenantLookupImplTest {

    // ------------------------- ApiOrgUserImpl -------------------------

    @Test
    @DisplayName("getUserByIdAndTenant: tenantId 为空时返回 data=null 且不查库")
    void should_return_null_data_when_user_id_or_tenant_missing() {
        OrgUserManager userManager = mock(OrgUserManager.class);
        ApiOrgUserImpl impl = new ApiOrgUserImpl(userManager, mock(OrgUserService.class));

        Result<OrgUserAO> byNullTenant = impl.getUserByIdAndTenant(1001L, null);
        Result<OrgUserAO> byNullId = impl.getUserByIdAndTenant(null, 7L);

        assertTrue(byNullTenant.isSuccess());
        assertNull(byNullTenant.getData());
        assertNull(byNullId.getData());
        verifyNoInteractions(userManager);
    }

    @Test
    @DisplayName("getUserByIdAndTenant: 把 tenantId 透传给 Manager 的显式租户方法")
    void should_delegate_user_lookup_with_tenant() {
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(userManager.getDtoByIdAndTenant(1001L, 7L)).thenReturn(new OrgUserDTO().setId(1001L));
        ApiOrgUserImpl impl = new ApiOrgUserImpl(userManager, mock(OrgUserService.class));

        Result<OrgUserAO> result = impl.getUserByIdAndTenant(1001L, 7L);

        assertTrue(result.isSuccess());
        assertEquals(1001L, result.getData().getId());
        verify(userManager).getDtoByIdAndTenant(1001L, 7L);
    }

    @Test
    @DisplayName("listUsersByIdsAndTenant: 空集合或空 tenantId 返回空列表且不查库")
    void should_return_empty_list_when_ids_empty_or_tenant_missing() {
        OrgUserManager userManager = mock(OrgUserManager.class);
        ApiOrgUserImpl impl = new ApiOrgUserImpl(userManager, mock(OrgUserService.class));

        assertTrue(impl.listUsersByIdsAndTenant(List.of(), 7L).getData().isEmpty());
        assertTrue(impl.listUsersByIdsAndTenant(List.of(1001L), null).getData().isEmpty());

        verifyNoInteractions(userManager);
    }

    // ------------------------- ApiOrgDepartmentImpl -------------------------

    @Test
    @DisplayName("listUsersByDeptIdAndTenant: tenantId 为空时返回空列表且不查库")
    void should_return_empty_list_when_dept_id_or_tenant_missing() {
        OrgDepartmentManager departmentManager = mock(OrgDepartmentManager.class);
        ApiOrgDepartmentImpl impl = new ApiOrgDepartmentImpl(departmentManager);

        assertTrue(impl.listUsersByDeptIdAndTenant(2002L, null).getData().isEmpty());
        assertTrue(impl.listUsersByDeptIdAndTenant(null, 7L).getData().isEmpty());

        verifyNoInteractions(departmentManager);
    }

    @Test
    @DisplayName("listUsersByDeptIdAndTenant: 把 tenantId 透传给 Manager 的显式租户方法")
    void should_delegate_department_users_with_tenant() {
        OrgDepartmentManager departmentManager = mock(OrgDepartmentManager.class);
        when(departmentManager.getUsersByDeptIdAndTenant(2002L, 7L))
            .thenReturn(List.of(new OrgUserDTO().setId(1001L)));
        ApiOrgDepartmentImpl impl = new ApiOrgDepartmentImpl(departmentManager);

        Result<List<OrgUserAO>> result = impl.listUsersByDeptIdAndTenant(2002L, 7L);

        assertEquals(1, result.getData().size());
        verify(departmentManager).getUsersByDeptIdAndTenant(2002L, 7L);
    }

    @Test
    @DisplayName("listUsersByDeptIdRecursiveAndTenant: tenantId 为空时返回空列表且不查库")
    void should_return_empty_list_when_recursive_dept_id_or_tenant_missing() {
        OrgDepartmentManager departmentManager = mock(OrgDepartmentManager.class);
        ApiOrgDepartmentImpl impl = new ApiOrgDepartmentImpl(departmentManager);

        assertTrue(impl.listUsersByDeptIdRecursiveAndTenant(2002L, null).getData().isEmpty());
        assertTrue(impl.listUsersByDeptIdRecursiveAndTenant(null, 7L).getData().isEmpty());

        verifyNoInteractions(departmentManager);
    }

    @Test
    @DisplayName("listUsersByDeptIdRecursiveAndTenant: 必须走递归的显式租户方法，不能退化为非递归")
    void should_delegate_recursive_department_users_with_tenant() {
        OrgDepartmentManager departmentManager = mock(OrgDepartmentManager.class);
        when(departmentManager.getUsersByDeptIdRecursiveAndTenant(2002L, 7L))
            .thenReturn(List.of(new OrgUserDTO().setId(1001L)));
        ApiOrgDepartmentImpl impl = new ApiOrgDepartmentImpl(departmentManager);

        Result<List<OrgUserAO>> result = impl.listUsersByDeptIdRecursiveAndTenant(2002L, 7L);

        assertEquals(1, result.getData().size());
        verify(departmentManager).getUsersByDeptIdRecursiveAndTenant(2002L, 7L);
        verify(departmentManager, never()).getUsersByDeptIdAndTenant(any(), any());
    }

    // ------------------------- ApiOrgRoleImpl -------------------------

    @Test
    @DisplayName("listUsersByRoleIdAndTenant: tenantId 为空时返回空列表且不查库")
    void should_return_empty_list_when_role_id_or_tenant_missing() {
        OrgRoleManager roleManager = mock(OrgRoleManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        ApiOrgRoleImpl impl = new ApiOrgRoleImpl(roleManager, userManager);

        assertTrue(impl.listUsersByRoleIdAndTenant(3003L, null).getData().isEmpty());
        assertTrue(impl.listUsersByRoleIdAndTenant(null, 7L).getData().isEmpty());

        verifyNoInteractions(roleManager);
        verifyNoInteractions(userManager);
    }

    @Test
    @DisplayName("listUsersByRoleIdAndTenant: 角色用户ID与用户查询都必须限定同一租户")
    void should_delegate_role_users_with_tenant() {
        OrgRoleManager roleManager = mock(OrgRoleManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(roleManager.listUserIdsByRoleIdAndTenant(3003L, 7L)).thenReturn(List.of(1001L));
        when(userManager.listDtoByIdsAndTenant(List.of(1001L), 7L))
            .thenReturn(List.of(new OrgUserDTO().setId(1001L)));
        ApiOrgRoleImpl impl = new ApiOrgRoleImpl(roleManager, userManager);

        Result<List<OrgUserAO>> result = impl.listUsersByRoleIdAndTenant(3003L, 7L);

        assertEquals(1, result.getData().size());
        verify(roleManager).listUserIdsByRoleIdAndTenant(3003L, 7L);
        verify(userManager).listDtoByIdsAndTenant(List.of(1001L), 7L);
    }

    // ------------------------- ApiOrgUserPositionImpl -------------------------

    @Test
    @DisplayName("listSubordinatesByUserIdAndTenant: tenantId 为空时返回空列表且不查库")
    void should_return_empty_list_when_user_id_or_tenant_missing() {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        ApiOrgUserPositionImpl impl = new ApiOrgUserPositionImpl(positionManager, userManager);

        assertTrue(impl.listSubordinatesByUserIdAndTenant(1001L, null).getData().isEmpty());
        assertTrue(impl.listSubordinatesByUserIdAndTenant(null, 7L).getData().isEmpty());

        verifyNoInteractions(positionManager);
        verifyNoInteractions(userManager);
    }

    @Test
    @DisplayName("listSubordinatesByUserIdAndTenant: 任职记录与用户查询都必须限定同一租户")
    void should_delegate_subordinates_with_tenant() {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        OrgUserPositionDTO relation = new OrgUserPositionDTO();
        relation.setUserId(1002L);
        when(positionManager.listByReportToAndTenant(1001L, 7L)).thenReturn(List.of(relation));
        when(userManager.listDtoByIdsAndTenant(List.of(1002L), 7L))
            .thenReturn(List.of(new OrgUserDTO().setId(1002L)));
        ApiOrgUserPositionImpl impl = new ApiOrgUserPositionImpl(positionManager, userManager);

        Result<List<OrgUserAO>> result = impl.listSubordinatesByUserIdAndTenant(1001L, 7L);

        assertEquals(1, result.getData().size());
        verify(positionManager).listByReportToAndTenant(1001L, 7L);
        verify(userManager).listDtoByIdsAndTenant(List.of(1002L), 7L);
        verify(userManager, never()).listDtoByIds(any());
    }

    @Test
    @DisplayName("getSuperiorByUserIdAndTenant: tenantId 为空时返回 data=null 且不查库")
    void should_return_null_superior_when_user_id_or_tenant_missing() {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        ApiOrgUserPositionImpl impl = new ApiOrgUserPositionImpl(positionManager, userManager);

        Result<OrgUserAO> byNullTenant = impl.getSuperiorByUserIdAndTenant(1001L, null);
        Result<OrgUserAO> byNullUser = impl.getSuperiorByUserIdAndTenant(null, 7L);

        assertTrue(byNullTenant.isSuccess());
        assertNull(byNullTenant.getData());
        assertNull(byNullUser.getData());
        verifyNoInteractions(positionManager);
        verifyNoInteractions(userManager);
    }

    @Test
    @DisplayName("getSuperiorByUserIdAndTenant: 任职记录与用户查询都必须限定同一租户")
    void should_delegate_superior_with_tenant() {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(positionManager.getReportToByUserIdAndTenant(1001L, 7L)).thenReturn(1002L);
        when(userManager.getDtoByIdAndTenant(1002L, 7L)).thenReturn(new OrgUserDTO().setId(1002L));
        ApiOrgUserPositionImpl impl = new ApiOrgUserPositionImpl(positionManager, userManager);

        Result<OrgUserAO> result = impl.getSuperiorByUserIdAndTenant(1001L, 7L);

        assertEquals(1002L, result.getData().getId());
        verify(positionManager).getReportToByUserIdAndTenant(1001L, 7L);
        verify(userManager).getDtoByIdAndTenant(1002L, 7L);
        verify(userManager, never()).getDtoById(any());
    }

    @Test
    @DisplayName("getSuperiorByUserIdAndTenant: 没有汇报对象时 data=null（空值不是错误）")
    void should_return_null_data_when_superior_absent_in_tenant() {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(positionManager.getReportToByUserIdAndTenant(1001L, 7L)).thenReturn(null);
        ApiOrgUserPositionImpl impl = new ApiOrgUserPositionImpl(positionManager, userManager);

        Result<OrgUserAO> result = impl.getSuperiorByUserIdAndTenant(1001L, 7L);

        assertTrue(result.isSuccess());
        assertNull(result.getData());
        verifyNoInteractions(userManager);
    }

    @Test
    @DisplayName("listUsersByPositionIdAndTenant: tenantId 为空时返回空列表且不查库")
    void should_return_empty_list_when_position_id_or_tenant_missing() {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        ApiOrgUserPositionImpl impl = new ApiOrgUserPositionImpl(positionManager, userManager);

        assertTrue(impl.listUsersByPositionIdAndTenant(3003L, null).getData().isEmpty());
        assertTrue(impl.listUsersByPositionIdAndTenant(null, 7L).getData().isEmpty());

        verifyNoInteractions(positionManager);
        verifyNoInteractions(userManager);
    }

    @Test
    @DisplayName("listUsersByPositionIdAndTenant: 岗位用户ID与用户查询都必须限定同一租户")
    void should_delegate_position_users_with_tenant() {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(positionManager.listUserIdsByPositionIdAndTenant(3003L, 7L)).thenReturn(List.of(1001L));
        when(userManager.listDtoByIdsAndTenant(List.of(1001L), 7L))
            .thenReturn(List.of(new OrgUserDTO().setId(1001L)));
        ApiOrgUserPositionImpl impl = new ApiOrgUserPositionImpl(positionManager, userManager);

        Result<List<OrgUserAO>> result = impl.listUsersByPositionIdAndTenant(3003L, 7L);

        assertEquals(1, result.getData().size());
        verify(positionManager).listUserIdsByPositionIdAndTenant(3003L, 7L);
        verify(userManager).listDtoByIdsAndTenant(List.of(1001L), 7L);
        verify(userManager, never()).listDtoByIds(any());
    }
}
