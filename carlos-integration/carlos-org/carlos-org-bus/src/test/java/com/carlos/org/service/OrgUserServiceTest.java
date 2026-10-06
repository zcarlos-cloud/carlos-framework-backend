package com.carlos.org.service;

import com.carlos.org.config.OrgProperties;
import com.carlos.org.encoder.PasswordEncoderProvider;
import com.carlos.org.exception.AccountExistsException;
import com.carlos.org.exception.PasswordException;
import com.carlos.org.exception.PhoneExistsException;
import com.carlos.org.exception.UserNotFoundException;
import com.carlos.org.manager.*;
import com.carlos.org.pojo.dto.OrgRoleDTO;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.pojo.dto.OrgUserDepartmentDTO;
import com.carlos.org.pojo.dto.OrgUserDeptDTO;
import com.carlos.org.pojo.dto.OrgUserRoleDTO;
import com.carlos.org.pojo.enums.OrgUserStateEnum;
import com.carlos.org.pojo.excel.OrgUserExcel;
import com.carlos.org.pojo.param.OrgUserAssignDeptParam;
import com.carlos.org.pojo.param.OrgUserAssignRoleParam;
import com.carlos.org.position.manager.OrgPositionLevelManager;
import com.carlos.org.position.manager.OrgPositionManager;
import com.carlos.org.position.manager.OrgUserPositionManager;
import com.carlos.org.position.pojo.dto.OrgUserPositionDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OrgUserService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class OrgUserServiceTest {

    @Mock
    private OrgUserManager userManager;

    @Mock
    private OrgUserDepartmentManager userDepartmentManager;

    @Mock
    private OrgUserRoleManager userRoleManager;

    @Mock
    private OrgUserPositionManager userPositionManager;

    @Mock
    private OrgDepartmentManager departmentManager;

    @Mock
    private OrgRoleManager roleManager;

    @Mock
    private OrgPositionManager positionManager;

    @Mock
    private OrgPositionLevelManager positionLevelManager;

    /**
     * 密码编码提供者
     *
     * `OrgUserService` 把它声明为 `final` 字段，走构造器注入，所以必须在这里给出 Mock ——
     * 否则它为 null，任何走到「编码密码」那一步的用例都会抛
     * 「密码编码提供者未配置」（此前 resetPassword 的用例就是这么挂的）。
     */
    @Mock
    private PasswordEncoderProvider passwordEncoderProvider;

    /**
     * 租户隔离配置
     *
     * addOrgUser 会读取 platformTenantId 回填租户ID，用真实对象（默认值即可）而非 Mock，
     * 否则 getTenant() 返回 null 触发 NPE。
     */
    @Spy
    private OrgProperties orgProperties = new OrgProperties();

    @InjectMocks
    private OrgUserService userService;

    @Test
    @DisplayName("新增用户成功当参数合法时")
    void should_create_user_successfully_when_params_valid() {
        // given
        OrgUserDTO dto = new OrgUserDTO();
        dto.setAccount("testuser");
        dto.setPhone("13800138000");

        when(userManager.getUserByAccount("testuser")).thenReturn(null);
        when(userManager.getUserByPhone("13800138000")).thenReturn(null);
        when(userManager.add(any(OrgUserDTO.class))).thenReturn(true);

        // when
        userService.addOrgUser(dto);

        // then
        assertEquals(OrgUserStateEnum.ENABLE, dto.getUserStatus());
        verify(userManager, times(1)).add(any(OrgUserDTO.class));
    }

    @Test
    @DisplayName("新增用户失败当账号已存在时")
    void should_throw_exception_when_account_exists() {
        // given
        OrgUserDTO dto = new OrgUserDTO();
        dto.setAccount("existing");

        OrgUserDTO exist = new OrgUserDTO();
        exist.setAccount("existing");
        when(userManager.getUserByAccount("existing")).thenReturn(exist);

        // when & then
        assertThrows(AccountExistsException.class, () -> userService.addOrgUser(dto));
        verify(userManager, never()).add(any());
    }

    @Test
    @DisplayName("新增用户失败当手机号已存在时")
    void should_throw_exception_when_phone_exists() {
        // given
        OrgUserDTO dto = new OrgUserDTO();
        dto.setAccount("newuser");
        dto.setPhone("13800138000");

        when(userManager.getUserByAccount("newuser")).thenReturn(null);
        when(userManager.getUserByPhone("13800138000")).thenReturn(new OrgUserDTO());

        // when & then
        assertThrows(PhoneExistsException.class, () -> userService.addOrgUser(dto));
    }

    @Test
    @DisplayName("删除用户成功")
    void should_delete_user_successfully() {
        // given
        Set<Long> ids = new HashSet<>(Arrays.asList(1L, 2L));
        when(userManager.removeById(1L)).thenReturn(true);
        when(userManager.removeById(2L)).thenReturn(true);
        when(userDepartmentManager.removeByUserId(1L)).thenReturn(true);
        when(userDepartmentManager.removeByUserId(2L)).thenReturn(true);
        when(userRoleManager.removeByUserId(1L)).thenReturn(true);
        when(userRoleManager.removeByUserId(2L)).thenReturn(true);
        when(userPositionManager.removeByUserId(1L)).thenReturn(true);
        when(userPositionManager.removeByUserId(2L)).thenReturn(true);

        // when
        userService.deleteOrgUser(ids);

        // then
        verify(userManager, times(1)).removeById(1L);
        verify(userManager, times(1)).removeById(2L);
        verify(userDepartmentManager, times(1)).removeByUserId(1L);
        verify(userDepartmentManager, times(1)).removeByUserId(2L);
        verify(userRoleManager, times(1)).removeByUserId(1L);
        verify(userRoleManager, times(1)).removeByUserId(2L);
        verify(userPositionManager, times(1)).removeByUserId(1L);
        verify(userPositionManager, times(1)).removeByUserId(2L);
    }

    @Test
    @DisplayName("用户分配部门成功")
    void should_assign_departments_successfully() {
        // given
        OrgUserAssignDeptParam param = new OrgUserAssignDeptParam();
        param.setUserId(1L);
        param.setDepartmentIds(Arrays.asList(10L, 20L));
        param.setMainDeptId(10L);

        OrgUserDTO user = new OrgUserDTO();
        user.setId(1L);
        when(userManager.getDtoById(1L)).thenReturn(user);
        when(userManager.modify(any(OrgUserDTO.class))).thenReturn(true);

        // when
        userService.assignDepartments(param);

        // then
        verify(userDepartmentManager, times(1)).removeByUserId(1L);
        verify(userDepartmentManager, times(2)).add(any(OrgUserDepartmentDTO.class));
        verify(userManager, times(1)).modify(any(OrgUserDTO.class));
    }

    @Test
    @DisplayName("用户分配角色成功")
    void should_assign_roles_successfully() {
        // given
        OrgUserAssignRoleParam param = new OrgUserAssignRoleParam();
        param.setUserId(1L);
        param.setRoleIds(Arrays.asList(100L, 200L));

        OrgUserDTO user = new OrgUserDTO();
        user.setId(1L);
        when(userManager.getDtoById(1L)).thenReturn(user);

        // when
        userService.assignRoles(param);

        // then
        verify(userRoleManager, times(1)).removeByUserId(1L);
        verify(userRoleManager, times(2)).add(any(OrgUserRoleDTO.class));
    }

    @Test
    @DisplayName("分配角色携带失效时间时应透传到每条授权记录")
    void should_propagate_expire_time_when_assigning_temporary_roles() {
        // given
        LocalDateTime expireTime = LocalDateTime.now().plusDays(30);
        OrgUserAssignRoleParam param = new OrgUserAssignRoleParam();
        param.setUserId(1L);
        param.setRoleIds(Arrays.asList(100L, 200L));
        param.setExpireTime(expireTime);

        OrgUserDTO user = new OrgUserDTO();
        user.setId(1L);
        when(userManager.getDtoById(1L)).thenReturn(user);

        // when
        userService.assignRoles(param);

        // then
        ArgumentCaptor<OrgUserRoleDTO> captor = ArgumentCaptor.forClass(OrgUserRoleDTO.class);
        verify(userRoleManager, times(2)).add(captor.capture());
        for (OrgUserRoleDTO relation : captor.getAllValues()) {
            assertEquals(expireTime, relation.getExpireTime(), "临时授权的 expireTime 应写入每条用户角色关联");
        }
    }

    @Test
    @DisplayName("分配部门失败当用户不存在时")
    void should_throw_exception_when_user_not_found_for_dept_assignment() {
        // given
        OrgUserAssignDeptParam param = new OrgUserAssignDeptParam();
        param.setUserId(999L);
        when(userManager.getDtoById(999L)).thenReturn(null);

        // when & then
        assertThrows(UserNotFoundException.class, () -> userService.assignDepartments(param));
    }

    @Test
    @DisplayName("密码复杂度校验通过当密码符合规则时")
    void should_pass_password_complexity_when_valid() {
        // given
        String validPassword = "Abc123!@#";

        // when & then
        assertTrue(userService.checkPasswordComplexity(validPassword));
    }

    @Test
    @DisplayName("密码复杂度校验失败当密码长度不足时")
    void should_fail_password_complexity_when_too_short() {
        // given
        String shortPassword = "Ab1!";

        // when & then
        assertFalse(userService.checkPasswordComplexity(shortPassword));
    }

    @Test
    @DisplayName("密码复杂度校验失败当字符类型不足3种时")
    void should_fail_password_complexity_when_type_count_insufficient() {
        // given
        String onlyTwoTypes = "abcdefgh"; // 只有小写
        String onlyTwoTypes2 = "Abcdefgh"; // 只有大小写

        // when & then
        assertFalse(userService.checkPasswordComplexity(onlyTwoTypes));
        assertFalse(userService.checkPasswordComplexity(onlyTwoTypes2));
    }

    @Test
    @DisplayName("重置密码失败当密码复杂度不符合要求时")
    void should_throw_exception_when_reset_password_complexity_invalid() {
        // given
        OrgUserDTO user = new OrgUserDTO();
        user.setId(1L);
        when(userManager.getDtoById(1L)).thenReturn(user);

        // when & then
        assertThrows(PasswordException.class, () -> userService.resetPassword(1L, "12345678"));
    }

    @Test
    @DisplayName("重置密码成功当密码复杂度符合要求时")
    void should_reset_password_successfully_when_complexity_valid() {
        // given
        OrgUserDTO user = new OrgUserDTO();
        user.setId(1L);
        when(userManager.getDtoById(1L)).thenReturn(user);
        when(userManager.modify(any(OrgUserDTO.class))).thenReturn(true);
        when(passwordEncoderProvider.encode(any())).thenReturn("encoded-pwd");

        // when
        userService.resetPassword(1L, "Abc123!@");

        // then
        verify(userManager, times(1)).modify(any(OrgUserDTO.class));
    }

    @Test
    @DisplayName("获取用户部门列表成功")
    void should_get_user_departments_successfully() {
        // given
        OrgUserDepartmentDTO dept1 = new OrgUserDepartmentDTO();
        dept1.setDeptId(10L);
        dept1.setMainDept(true);

        when(userDepartmentManager.listByUserId(1L)).thenReturn(Arrays.asList(dept1));

        // when
        List<OrgUserDeptDTO> result = userService.getUserDepartments(1L);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取用户角色列表成功")
    void should_get_user_roles_successfully() {
        // given
        OrgUserRoleDTO role1 = new OrgUserRoleDTO();
        role1.setRoleId(100L);

        when(userRoleManager.listAllByUserId(1L)).thenReturn(Arrays.asList(role1));

        // when
        List<OrgUserRoleDTO> result = userService.getUserRoles(1L);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * 关联表只存 role_id，而 `OrgUserRoleVO` 声明了 roleName / roleCode ——
     * 不补全的话接口恒返回 null，前端「用户角色」一列只能渲染出一堆空标签。
     *
     * 上面那个用例只断言了 size，所以这个缺陷此前一直没被发现。
     */
    @Test
    @DisplayName("获取用户角色列表时补全角色名称与编码")
    void should_fill_role_name_and_code_when_getting_user_roles() {
        // given
        OrgUserRoleDTO relation = new OrgUserRoleDTO();
        relation.setRoleId(100L);

        OrgRoleDTO role = new OrgRoleDTO();
        role.setRoleName("超级管理员");
        role.setRoleCode("ROLE_SUPER_ADMIN");

        when(userRoleManager.listAllByUserId(1L)).thenReturn(Arrays.asList(relation));
        when(roleManager.getDtoById(100L)).thenReturn(role);

        // when
        List<OrgUserRoleDTO> result = userService.getUserRoles(1L);

        // then
        assertEquals(1, result.size());
        assertEquals("超级管理员", result.get(0).getRoleName());
        assertEquals("ROLE_SUPER_ADMIN", result.get(0).getRoleCode());
    }

    @Test
    @DisplayName("角色已被删除时返回空名称而不是抛异常")
    void should_not_fail_when_role_missing() {
        // given
        OrgUserRoleDTO relation = new OrgUserRoleDTO();
        relation.setRoleId(100L);

        when(userRoleManager.listAllByUserId(1L)).thenReturn(Arrays.asList(relation));
        when(roleManager.getDtoById(100L)).thenReturn(null);

        // when
        List<OrgUserRoleDTO> result = userService.getUserRoles(1L);

        // then
        assertEquals(1, result.size());
        assertNull(result.get(0).getRoleName());
    }

    @Test
    @DisplayName("获取用户岗位列表返回空当无关联时")
    void should_return_empty_positions_when_no_relation() {
        // given
        when(userPositionManager.listByUserId(1L)).thenReturn(Collections.emptyList());

        // when
        List<OrgUserPositionDTO> result = userService.getUserPositions(1L);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ------------------------------------------------------------------
    // 两阶段导入的「确认写入」契约
    //
    // 这一段是前端无法覆盖到的：UI 侧的预览、错误列、确认按钮禁用都已实测，
    // 但「有通过校验的行时到底写不写、写哪些」只能在服务层验证
    // （导出文件里只会有已存在的账号，必然全部报错，构造不出「通过」的样例行）。
    // ------------------------------------------------------------------

    @Test
    @DisplayName("导入确认：跳过带 error 的行，只写入通过校验的行")
    void should_skip_errored_rows_when_confirming_import() {
        // given
        OrgUserExcel errored = new OrgUserExcel();
        errored.setAccount("dup");
        errored.setError("账号已存在；手机号已存在");

        OrgUserExcel valid = new OrgUserExcel();
        valid.setAccount("fresh");
        valid.setPhone("13800138001");

        // 模拟真实新增：落库后回填雪花 id
        when(userManager.add(any(OrgUserDTO.class))).thenAnswer(invocation -> {
            OrgUserDTO dto = invocation.getArgument(0);
            dto.setId(100L);

            return true;
        });

        // when
        List<Serializable> ids = userService.confirmImportUser(Arrays.asList(errored, valid));

        // then
        ArgumentCaptor<OrgUserDTO> captor = ArgumentCaptor.forClass(OrgUserDTO.class);
        verify(userManager, times(1)).add(captor.capture());
        assertEquals("fresh", captor.getValue().getAccount(), "只有通过校验的行才应被写入");
        assertEquals(1, ids.size());
        assertEquals(100L, ids.get(0));
    }

    @Test
    @DisplayName("导入确认：跳过账号为空的行（兜底校验）")
    void should_skip_rows_with_blank_account_when_confirming_import() {
        // given：没有 error，但账号为空 —— 走的是另一条 continue 分支
        OrgUserExcel blankAccount = new OrgUserExcel();
        blankAccount.setNickname("没有账号");

        // when
        List<Serializable> ids = userService.confirmImportUser(List.of(blankAccount));

        // then
        assertTrue(ids.isEmpty());
        verify(userManager, never()).add(any(OrgUserDTO.class));
    }

    @Test
    @DisplayName("导入确认：昵称缺省时回退为账号")
    void should_fallback_nickname_to_account_when_confirming_import() {
        // given
        OrgUserExcel row = new OrgUserExcel();
        row.setAccount("noname");

        when(userManager.add(any(OrgUserDTO.class))).thenReturn(true);

        // when
        userService.confirmImportUser(List.of(row));

        // then
        ArgumentCaptor<OrgUserDTO> captor = ArgumentCaptor.forClass(OrgUserDTO.class);
        verify(userManager).add(captor.capture());
        assertEquals("noname", captor.getValue().getNickname());
        assertEquals(OrgUserStateEnum.ENABLE, captor.getValue().getUserStatus(), "导入的用户应为启用态");
    }

    @Test
    @DisplayName("导入确认：空列表直接返回空，不触碰数据层")
    void should_not_touch_data_layer_when_confirming_import_with_empty_list() {
        // when & then
        assertTrue(userService.confirmImportUser(null).isEmpty());
        assertTrue(userService.confirmImportUser(Collections.emptyList()).isEmpty());
        verifyNoInteractions(userManager);
    }

}
