package com.carlos.org.apiimpl;

import com.carlos.org.manager.OrgDepartmentManager;
import com.carlos.org.manager.OrgRoleManager;
import com.carlos.org.manager.OrgUserManager;
import com.carlos.org.position.apiimpl.ApiOrgUserPositionImpl;
import com.carlos.org.position.manager.OrgUserPositionManager;
import com.carlos.org.service.OrgUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 显式租户（{@code ...AndTenant}）Feign 端点的 HTTP 路径与参数绑定测试
 *
 * <p>用 standalone MockMvc 真正走一遍 Spring MVC 的映射与参数绑定，钉死：
 * 类级路径 + 方法路径拼出的实际地址（含 {@code ApiOrgUser} 历史上重复的 {@code /user/} 段，
 * 既有风格不修正），以及 {@code id/ids/deptId/roleId/userId} + {@code tenantId} 的参数名。</p>
 *
 * @author Carlos
 */
class ApiOrgTenantEndpointPathTest {

    @Test
    @DisplayName("GET /api/org/user/user/by-id-and-tenant 应绑定 id 与 tenantId")
    void should_expose_user_by_id_and_tenant_path() throws Exception {
        OrgUserManager userManager = mock(OrgUserManager.class);
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ApiOrgUserImpl(userManager, mock(OrgUserService.class)))
            .build();

        mockMvc.perform(get("/api/org/user/user/by-id-and-tenant")
                .param("id", "1001")
                .param("tenantId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/org/user/user/list-by-ids-and-tenant 应绑定 ids 与 tenantId")
    void should_expose_user_list_by_ids_and_tenant_path() throws Exception {
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(userManager.listDtoByIdsAndTenant(List.of(1001L, 1002L), 7L)).thenReturn(List.of());
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ApiOrgUserImpl(userManager, mock(OrgUserService.class)))
            .build();

        mockMvc.perform(get("/api/org/user/user/list-by-ids-and-tenant")
                .param("ids", "1001", "1002")
                .param("tenantId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/org/user/position/listSubordinatesByUserIdAndTenant 应绑定 userId 与 tenantId")
    void should_expose_subordinates_and_tenant_path() throws Exception {
        OrgUserPositionManager positionManager = mock(OrgUserPositionManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(positionManager.listByReportToAndTenant(1001L, 7L)).thenReturn(List.of());
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ApiOrgUserPositionImpl(positionManager, userManager))
            .build();

        mockMvc.perform(get("/api/org/user/position/listSubordinatesByUserIdAndTenant")
                .param("userId", "1001")
                .param("tenantId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/org/department/listUsersByDeptIdAndTenant 应绑定 deptId 与 tenantId")
    void should_expose_department_users_and_tenant_path() throws Exception {
        OrgDepartmentManager departmentManager = mock(OrgDepartmentManager.class);
        when(departmentManager.getUsersByDeptIdAndTenant(2002L, 7L)).thenReturn(List.of());
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ApiOrgDepartmentImpl(departmentManager))
            .build();

        mockMvc.perform(get("/api/org/department/listUsersByDeptIdAndTenant")
                .param("deptId", "2002")
                .param("tenantId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/org/role/listUsersByRoleIdAndTenant 应绑定 roleId 与 tenantId")
    void should_expose_role_users_and_tenant_path() throws Exception {
        OrgRoleManager roleManager = mock(OrgRoleManager.class);
        OrgUserManager userManager = mock(OrgUserManager.class);
        when(roleManager.listUserIdsByRoleIdAndTenant(3003L, 7L)).thenReturn(List.of());
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ApiOrgRoleImpl(roleManager, userManager))
            .build();

        mockMvc.perform(get("/api/org/role/listUsersByRoleIdAndTenant")
                .param("roleId", "3003")
                .param("tenantId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
