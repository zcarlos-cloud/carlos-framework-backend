package com.carlos.org.apiimpl;

import com.carlos.core.response.Result;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.manager.OrgDepartmentManager;
import com.carlos.org.manager.OrgUserManager;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.position.apiimpl.ApiOrgUserPositionImpl;
import com.carlos.org.position.manager.OrgUserPositionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 本次新增的三个远程查询端点的 apiimpl 单元测试与 HTTP 路径绑定测试
 *
 * <p>覆盖「直属上级」「部门子树用户」「岗位用户」的普通变体（租户由
 * {@code OrgTenantLineHandler} 从当前登录上下文隐式取得；显式租户变体见
 * {@link ApiOrgTenantLookupImplTest}）。</p>
 *
 * <p>无真实 org 服务与数据库，因此全部协作者为 mock，断言只针对
 * 「怎么调 Manager」与「Spring MVC 实际映射出的路径与参数名」。</p>
 *
 * @author Carlos
 */
class ApiOrgLookupImplTest {

    // ------------------------- 直属上级 -------------------------

    @Test
    @DisplayName("getSuperiorByUserId: 读该用户自己的 report_to 后按 id 取上级")
    void should_resolve_superior_by_own_report_to() {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(positionManager.getReportToByUserId(1001L)).thenReturn(1002L);
        when(userManager.getDtoById(1002L)).thenReturn(new OrgUserDTO().setId(1002L));
        ApiOrgUserPositionImpl impl = new ApiOrgUserPositionImpl(positionManager, userManager);

        Result<OrgUserAO> result = impl.getSuperiorByUserId(1001L);

        assertTrue(result.isSuccess());
        assertEquals(1002L, result.getData().getId());
        verify(positionManager).getReportToByUserId(1001L);
        verify(userManager).getDtoById(1002L);
        // 方向不能反：查上级绝不能走「以下级为条件的 report_to 查询」
        verify(positionManager, never()).listByReportTo(any());
    }

    @Test
    @DisplayName("getSuperiorByUserId: 空 userId、或汇报线上没有上级时 data=null 且不报错")
    void should_return_null_data_when_superior_absent() {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(positionManager.getReportToByUserId(1001L)).thenReturn(null);
        ApiOrgUserPositionImpl impl = new ApiOrgUserPositionImpl(positionManager, userManager);

        Result<OrgUserAO> byNullUser = impl.getSuperiorByUserId(null);
        Result<OrgUserAO> noSuperior = impl.getSuperiorByUserId(1001L);

        assertTrue(byNullUser.isSuccess());
        assertNull(byNullUser.getData());
        assertTrue(noSuperior.isSuccess());
        assertNull(noSuperior.getData());
        verify(positionManager, never()).getReportToByUserId(null);
        verifyNoInteractions(userManager);
    }

    // ------------------------- 部门子树用户 -------------------------

    @Test
    @DisplayName("listUsersByDeptIdRecursive: 一次远程往返委托给递归 Manager 方法")
    void should_delegate_recursive_department_users() {
        OrgDepartmentManager departmentManager = mock(OrgDepartmentManager.class);
        when(departmentManager.getUsersByDeptIdRecursive(2002L))
            .thenReturn(List.of(new OrgUserDTO().setId(1001L), new OrgUserDTO().setId(1002L)));
        ApiOrgDepartmentImpl impl = new ApiOrgDepartmentImpl(departmentManager);

        Result<List<OrgUserAO>> result = impl.listUsersByDeptIdRecursive(2002L);

        assertEquals(2, result.getData().size());
        verify(departmentManager).getUsersByDeptIdRecursive(2002L);
        // 非递归方法不得被顺带调用（语义不同，不能混用）
        verify(departmentManager, never()).getUsersByDeptId(any());
    }

    @Test
    @DisplayName("listUsersByDeptIdRecursive: 空 deptId 返回空列表且不查库")
    void should_return_empty_list_for_null_recursive_dept_id() {
        OrgDepartmentManager departmentManager = mock(OrgDepartmentManager.class);
        ApiOrgDepartmentImpl impl = new ApiOrgDepartmentImpl(departmentManager);

        Result<List<OrgUserAO>> result = impl.listUsersByDeptIdRecursive(null);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEmpty());
        verifyNoInteractions(departmentManager);
    }

    // ------------------------- 岗位用户 -------------------------

    @Test
    @DisplayName("listUsersByPositionId: 先取岗位上的用户ID再批量取用户（无 N+1）")
    void should_delegate_position_users() {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(positionManager.listUserIdsByPositionId(3003L)).thenReturn(List.of(1001L, 1002L));
        when(userManager.listDtoByIds(List.of(1001L, 1002L)))
            .thenReturn(List.of(new OrgUserDTO().setId(1001L), new OrgUserDTO().setId(1002L)));
        ApiOrgUserPositionImpl impl = new ApiOrgUserPositionImpl(positionManager, userManager);

        Result<List<OrgUserAO>> result = impl.listUsersByPositionId(3003L);

        assertEquals(2, result.getData().size());
        verify(positionManager).listUserIdsByPositionId(3003L);
        verify(userManager).listDtoByIds(List.of(1001L, 1002L));
    }

    @Test
    @DisplayName("listUsersByPositionId: 空 positionId 返回空列表且不查库")
    void should_return_empty_list_for_null_position_id() {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        ApiOrgUserPositionImpl impl = new ApiOrgUserPositionImpl(positionManager, userManager);

        Result<List<OrgUserAO>> result = impl.listUsersByPositionId(null);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEmpty());
        verifyNoInteractions(positionManager);
        verifyNoInteractions(userManager);
    }

    // ------------------------- HTTP 路径与参数绑定 -------------------------

    @Test
    @DisplayName("GET /api/org/user/position/getSuperiorByUserId 应绑定 userId")
    void should_expose_superior_path() throws Exception {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(positionManager.getReportToByUserId(1001L)).thenReturn(null);
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ApiOrgUserPositionImpl(positionManager, userManager))
            .build();

        mockMvc.perform(get("/api/org/user/position/getSuperiorByUserId").param("userId", "1001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/org/user/position/getSuperiorByUserIdAndTenant 应绑定 userId 与 tenantId")
    void should_expose_superior_and_tenant_path() throws Exception {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(positionManager.getReportToByUserIdAndTenant(1001L, 7L)).thenReturn(null);
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ApiOrgUserPositionImpl(positionManager, userManager))
            .build();

        mockMvc.perform(get("/api/org/user/position/getSuperiorByUserIdAndTenant")
                .param("userId", "1001")
                .param("tenantId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/org/user/position/listUsersByPositionId 应绑定 positionId")
    void should_expose_position_users_path() throws Exception {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(positionManager.listUserIdsByPositionId(3003L)).thenReturn(List.of());
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ApiOrgUserPositionImpl(positionManager, userManager))
            .build();

        mockMvc.perform(get("/api/org/user/position/listUsersByPositionId").param("positionId", "3003"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/org/user/position/listUsersByPositionIdAndTenant 应绑定 positionId 与 tenantId")
    void should_expose_position_users_and_tenant_path() throws Exception {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(positionManager.listUserIdsByPositionIdAndTenant(3003L, 7L)).thenReturn(List.of());
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ApiOrgUserPositionImpl(positionManager, userManager))
            .build();

        mockMvc.perform(get("/api/org/user/position/listUsersByPositionIdAndTenant")
                .param("positionId", "3003")
                .param("tenantId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/org/department/listUsersByDeptIdRecursive 应绑定 deptId")
    void should_expose_recursive_department_users_path() throws Exception {
        OrgDepartmentManager departmentManager = mock(OrgDepartmentManager.class);
        when(departmentManager.getUsersByDeptIdRecursive(2002L)).thenReturn(List.of());
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ApiOrgDepartmentImpl(departmentManager))
            .build();

        mockMvc.perform(get("/api/org/department/listUsersByDeptIdRecursive").param("deptId", "2002"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/org/department/listUsersByDeptIdRecursiveAndTenant 应绑定 deptId 与 tenantId")
    void should_expose_recursive_department_users_and_tenant_path() throws Exception {
        OrgDepartmentManager departmentManager = mock(OrgDepartmentManager.class);
        when(departmentManager.getUsersByDeptIdRecursiveAndTenant(2002L, 7L)).thenReturn(List.of());
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ApiOrgDepartmentImpl(departmentManager))
            .build();

        mockMvc.perform(get("/api/org/department/listUsersByDeptIdRecursiveAndTenant")
                .param("deptId", "2002")
                .param("tenantId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
