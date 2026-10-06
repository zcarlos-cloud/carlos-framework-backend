package com.carlos.org.service;

import cn.hutool.extra.spring.SpringUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.org.config.DbPermissionProvider;
import com.carlos.org.exception.*;
import com.carlos.org.manager.OrgRoleManager;
import com.carlos.org.pojo.dto.OrgRoleDTO;
import com.carlos.org.pojo.dto.OrgRoleUserDTO;
import com.carlos.org.pojo.enums.OrgDataScopeEnum;
import com.carlos.org.pojo.enums.OrgRoleStateEnum;
import com.carlos.org.pojo.enums.OrgRoleTypeEnum;
import com.carlos.org.pojo.param.OrgRoleAssignPermissionParam;
import com.carlos.org.pojo.param.OrgRoleCopyParam;
import com.carlos.org.pojo.param.OrgRolePageParam;
import com.carlos.org.pojo.param.OrgRoleSetDataScopeParam;
import com.carlos.security.permission.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrgRoleService 单元测试
 *
 * <p>验证：角色操作权限分配写入 org_role_operation 并触发缓存失效</p>
 */
@ExtendWith(MockitoExtension.class)
class OrgRoleServiceTest {

    @Mock
    private OrgRoleManager roleManager;

    @Mock
    private PermissionService permissionService;

    @Mock
    private DbPermissionProvider dbProvider;

    private MockedStatic<SpringUtil> springUtilMock;

    @InjectMocks
    private OrgRoleService service;

    private OrgRoleDTO adminRole;

    @BeforeEach
    void setUp() {
        springUtilMock = mockStatic(SpringUtil.class);
        springUtilMock.when(() -> SpringUtil.getBean(PermissionService.class))
                .thenReturn(permissionService);
        springUtilMock.when(() -> SpringUtil.getBean(DbPermissionProvider.class))
                .thenReturn(dbProvider);

        adminRole = new OrgRoleDTO();
        adminRole.setId(100L);
        adminRole.setRoleName("管理员");
        adminRole.setRoleCode("ADMIN");
        adminRole.setRoleType(OrgRoleTypeEnum.CUSTOM);
        adminRole.setDataScope(OrgDataScopeEnum.ALL);
        adminRole.setRoleStatus(OrgRoleStateEnum.ENABLE);
    }

    @AfterEach
    void tearDown() {
        if (springUtilMock != null) {
            springUtilMock.close();
        }
    }

    // ------------------------- addOrgRole -------------------------

    @Test
    void should_add_role_successfully() {
        OrgRoleDTO dto = new OrgRoleDTO();
        dto.setRoleName("测试角色");
        dto.setRoleCode("TEST");
        when(roleManager.add(any(OrgRoleDTO.class))).thenReturn(true);

        assertDoesNotThrow(() -> service.addOrgRole(dto));
        verify(roleManager).add(dto);
    }

    @Test
    void should_set_defaults_when_not_provided() {
        OrgRoleDTO dto = new OrgRoleDTO();
        dto.setRoleName("默认角色");
        when(roleManager.add(any(OrgRoleDTO.class))).thenReturn(true);

        service.addOrgRole(dto);

        assertEquals(OrgRoleTypeEnum.CUSTOM, dto.getRoleType());
        assertEquals(OrgRoleStateEnum.ENABLE, dto.getRoleStatus());
        assertEquals(OrgDataScopeEnum.SELF_ONLY, dto.getDataScope());
    }

    @Test
    void should_throw_when_role_code_exists() {
        OrgRoleDTO dto = new OrgRoleDTO();
        dto.setRoleCode("EXISTING");
        dto.setRoleName("重复");
        when(roleManager.getByRoleCode("EXISTING")).thenReturn(new OrgRoleDTO());

        assertThrows(RoleCodeExistsException.class, () -> service.addOrgRole(dto));
    }

    // ------------------------- deleteOrgRole -------------------------

    @Test
    void should_throw_when_role_not_found() {
        when(roleManager.getDtoById(999L)).thenReturn(null);

        assertThrows(RoleNotFoundException.class, () -> service.deleteOrgRole(Set.of(999L)));
    }

    @Test
    void should_throw_when_system_role() {
        adminRole.setRoleType(OrgRoleTypeEnum.SYSTEM);
        when(roleManager.getDtoById(100L)).thenReturn(adminRole);

        assertThrows(SystemRoleNotEditableException.class, () -> service.deleteOrgRole(Set.of(100L)));
    }

    @Test
    void should_throw_when_role_in_use() {
        when(roleManager.getDtoById(100L)).thenReturn(adminRole);
        when(roleManager.getUserCountByRoleId(100L)).thenReturn(5L);

        assertThrows(RoleInUseException.class, () -> service.deleteOrgRole(Set.of(100L)));
    }

    @Test
    void should_delete_role_when_valid() {
        when(roleManager.getDtoById(100L)).thenReturn(adminRole);
        when(roleManager.getUserCountByRoleId(100L)).thenReturn(0L);
        when(roleManager.removeById(100L)).thenReturn(true);

        assertDoesNotThrow(() -> service.deleteOrgRole(Set.of(100L)));
    }

    // ------------------------- assignPermissions (operationIds) -------------------------

    @Test
    void should_throw_when_role_not_found_on_assign() {
        when(roleManager.getDtoById(100L)).thenReturn(null);

        OrgRoleAssignPermissionParam param = new OrgRoleAssignPermissionParam();
        param.setRoleId(100L);
        param.setOperationIds(List.of(1L, 2L));

        assertThrows(RoleNotFoundException.class, () -> service.assignPermissions(param));
    }

    @Test
    void should_assign_operations_and_write_through_permission_cache() {
        when(roleManager.getDtoById(100L)).thenReturn(adminRole);
        when(roleManager.assignOperations(eq(100L), anyList())).thenReturn(true);

        OrgRoleAssignPermissionParam param = new OrgRoleAssignPermissionParam();
        param.setRoleId(100L);
        param.setOperationIds(List.of(1L, 2L, 3L));

        assertDoesNotThrow(() -> service.assignPermissions(param));

        verify(roleManager).assignOperations(eq(100L), anyList());
        // 授权后需把该角色最新的权限码写通到权限缓存（Redis），并广播失效
        verify(dbProvider).getPermissions(adminRole.getRoleCode());
        verify(permissionService).updateRolePermissions(eq(adminRole.getRoleCode()), anySet(), eq(true));
    }

    @Test
    void should_assign_operations_with_empty_list() {
        when(roleManager.getDtoById(100L)).thenReturn(adminRole);
        when(roleManager.assignOperations(eq(100L), anyList())).thenReturn(true);

        OrgRoleAssignPermissionParam param = new OrgRoleAssignPermissionParam();
        param.setRoleId(100L);
        param.setOperationIds(List.of());

        assertDoesNotThrow(() -> service.assignPermissions(param));
        verify(permissionService).updateRolePermissions(eq(adminRole.getRoleCode()), anySet(), eq(true));
    }

    @Test
    void should_not_fail_when_cache_write_through_fails() {
        // 权限缓存写通失败不应阻断授权本身
        when(roleManager.getDtoById(100L)).thenReturn(adminRole);
        when(roleManager.assignOperations(eq(100L), anyList())).thenReturn(true);
        // 模拟 Spring 上下文不可用
        springUtilMock.when(() -> SpringUtil.getBean(PermissionService.class))
                .thenThrow(new RuntimeException("Spring context unavailable"));

        OrgRoleAssignPermissionParam param = new OrgRoleAssignPermissionParam();
        param.setRoleId(100L);
        param.setOperationIds(List.of(1L));

        assertDoesNotThrow(() -> service.assignPermissions(param));
    }

    // ------------------------- copyRole -------------------------

    @Test
    void should_throw_when_source_role_not_found_on_copy() {
        when(roleManager.getDtoById(100L)).thenReturn(null);

        OrgRoleCopyParam param = new OrgRoleCopyParam();
        param.setSourceRoleId(100L);
        param.setNewRoleName("复制角色");

        assertThrows(RoleNotFoundException.class, () -> service.copyRole(param));
    }

    @Test
    void should_copy_role_and_operations() {
        when(roleManager.getDtoById(100L)).thenReturn(adminRole);
        when(roleManager.getByRoleCode("COPY_ADMIN")).thenReturn(null);
        // Simulate add() setting the new role ID (as real OrgRoleManagerImpl does)
        doAnswer(invocation -> {
            OrgRoleDTO dto = invocation.getArgument(0);
            dto.setId(200L);
            return true;
        }).when(roleManager).add(any(OrgRoleDTO.class));
        when(roleManager.getOperationIdsByRoleId(100L)).thenReturn(List.of(1L, 2L));
        when(roleManager.assignOperations(eq(200L), anyList())).thenReturn(true);

        OrgRoleCopyParam param = new OrgRoleCopyParam();
        param.setSourceRoleId(100L);
        param.setNewRoleName("复制管理员");
        param.setNewRoleCode("COPY_ADMIN");

        assertDoesNotThrow(() -> service.copyRole(param));

        verify(roleManager).add(any(OrgRoleDTO.class));
        verify(roleManager).getOperationIdsByRoleId(100L);
        verify(roleManager).assignOperations(eq(200L), anyList());
    }

    @Test
    void should_copy_role_without_operations_when_source_has_none() {
        when(roleManager.getDtoById(100L)).thenReturn(adminRole);
        when(roleManager.getByRoleCode("COPY_ADMIN")).thenReturn(null);
        doAnswer(invocation -> {
            OrgRoleDTO dto = invocation.getArgument(0);
            dto.setId(200L);
            return true;
        }).when(roleManager).add(any(OrgRoleDTO.class));
        when(roleManager.getOperationIdsByRoleId(100L)).thenReturn(List.of());

        OrgRoleCopyParam param = new OrgRoleCopyParam();
        param.setSourceRoleId(100L);
        param.setNewRoleName("复制管理员");
        param.setNewRoleCode("COPY_ADMIN");

        assertDoesNotThrow(() -> service.copyRole(param));

        verify(roleManager, never()).assignOperations(any(Serializable.class), anyList());
    }

    // ------------------------- changeState -------------------------

    @Test
    void should_change_role_state() {
        when(roleManager.getDtoById(100L)).thenReturn(adminRole);
        when(roleManager.modify(any(OrgRoleDTO.class))).thenReturn(true);

        assertDoesNotThrow(() -> service.changeState(100L, 0));

        assertEquals(OrgRoleStateEnum.DISABLE, adminRole.getRoleStatus());
    }

    @Test
    void should_throw_when_invalid_state() {
        assertThrows(OrgModuleException.class, () -> service.changeState(100L, 2));
        assertThrows(OrgModuleException.class, () -> service.changeState(100L, null));
    }

    // ------------------------- setDataScope -------------------------

    @Test
    void should_set_data_scope() {
        when(roleManager.getDtoById(100L)).thenReturn(adminRole);
        when(roleManager.modify(any(OrgRoleDTO.class))).thenReturn(true);

        OrgRoleSetDataScopeParam param = new OrgRoleSetDataScopeParam();
        param.setRoleId(100L);
        param.setDataScope(2);

        assertDoesNotThrow(() -> service.setDataScope(param));
        assertEquals(OrgDataScopeEnum.DEPT_AND_CHILD, adminRole.getDataScope());
    }

    @Test
    void should_throw_when_invalid_data_scope() {
        when(roleManager.getDtoById(100L)).thenReturn(adminRole);

        OrgRoleSetDataScopeParam param = new OrgRoleSetDataScopeParam();
        param.setRoleId(100L);
        param.setDataScope(99);

        assertThrows(OrgModuleException.class, () -> service.setDataScope(param));
    }

    // ------------------------- getRoleUsers -------------------------

    @Test
    void should_return_role_users() {
        when(roleManager.getDtoById(100L)).thenReturn(adminRole);
        Paging<OrgRoleUserDTO> page = new Paging<>();
        page.setRecords(List.of());
        when(roleManager.getRoleUsers(eq(100L), any(OrgRolePageParam.class))).thenReturn(page);

        Paging<OrgRoleUserDTO> result = service.getRoleUsers(100L, new OrgRolePageParam());

        assertNotNull(result);
    }
}
