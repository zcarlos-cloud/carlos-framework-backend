package com.carlos.org.service;

import com.carlos.core.exception.BusinessException;
import com.carlos.org.config.OrgProperties;
import com.carlos.org.encoder.PasswordEncoderProvider;
import com.carlos.org.manager.OrgTenantManager;
import com.carlos.org.manager.OrgUserManager;
import com.carlos.org.pojo.dto.OrgTenantDTO;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.pojo.enums.OrgTenantStateEnum;
import com.carlos.org.pojo.param.OrgTenantInitAdminParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrgTenantService 单元测试
 *
 * <p>S2 租户管理：编码唯一性、平台租户保护、删除前有用户拦截、管理员初始化。</p>
 *
 * @author Carlos
 */
class OrgTenantServiceTest {

    private OrgTenantManager tenantManager;
    private OrgUserManager userManager;
    private OrgTenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantManager = mock(OrgTenantManager.class);
        userManager = mock(OrgUserManager.class);
        tenantService = new OrgTenantService(
            tenantManager,
            userManager,
            new OrgProperties(),
            mock(PasswordEncoderProvider.class)
        );
    }

    @Test
    @DisplayName("新增租户时编码重复应抛业务异常")
    void should_throw_when_tenant_code_exists() {
        OrgTenantDTO dto = new OrgTenantDTO().setTenantCode("ACME").setTenantName("Acme");
        when(tenantManager.getByTenantCode("ACME")).thenReturn(new OrgTenantDTO());

        assertThrows(BusinessException.class, () -> tenantService.addOrgTenant(dto));
        verify(tenantManager, never()).add(any());
    }

    @Test
    @DisplayName("新增租户默认状态为正常")
    void should_default_status_normal_when_add() {
        OrgTenantDTO dto = new OrgTenantDTO().setTenantCode("ACME").setTenantName("Acme");
        when(tenantManager.getByTenantCode("ACME")).thenReturn(null);
        when(tenantManager.add(any(OrgTenantDTO.class))).thenReturn(true);

        tenantService.addOrgTenant(dto);

        assertEquals(OrgTenantStateEnum.NORMAL, dto.getTenantStatus());
    }

    @Test
    @DisplayName("平台租户不允许删除")
    void should_reject_delete_platform_tenant() {
        OrgTenantDTO platform = new OrgTenantDTO().setId(0L).setTenantCode("PLATFORM");
        when(tenantManager.getDtoById(0L)).thenReturn(platform);

        assertThrows(BusinessException.class, () -> tenantService.deleteOrgTenant(Set.of(0L)));
        verify(tenantManager, never()).delete(any());
    }

    @Test
    @DisplayName("平台租户不允许停用")
    void should_reject_disable_platform_tenant() {
        OrgTenantDTO platform = new OrgTenantDTO().setId(0L).setTenantCode("PLATFORM");
        when(tenantManager.getDtoById(0L)).thenReturn(platform);

        assertThrows(BusinessException.class, () -> tenantService.changeStatus(0L, 2));
        verify(tenantManager, never()).modify(any());
    }

    @Test
    @DisplayName("租户下存在用户时不允许删除")
    void should_reject_delete_when_tenant_has_users() {
        OrgTenantDTO tenant = new OrgTenantDTO().setId(100L).setTenantCode("ACME");
        when(tenantManager.getDtoById(100L)).thenReturn(tenant);
        when(userManager.countByTenantId(100L)).thenReturn(3L);

        assertThrows(BusinessException.class, () -> tenantService.deleteOrgTenant(Set.of(100L)));
        verify(tenantManager, never()).delete(any());
    }

    @Test
    @DisplayName("初始化管理员时租户停用应抛业务异常")
    void should_reject_init_admin_when_tenant_disabled() {
        OrgTenantDTO tenant = new OrgTenantDTO()
            .setId(100L)
            .setTenantStatus(OrgTenantStateEnum.DISABLE);
        when(tenantManager.getDtoById(100L)).thenReturn(tenant);

        OrgTenantInitAdminParam param = new OrgTenantInitAdminParam()
            .setTenantId(100L)
            .setAccount("admin")
            .setNickname("管理员")
            .setPassword("Password1");

        assertThrows(BusinessException.class, () -> tenantService.initTenantAdmin(param));
        verify(userManager, never()).add(any(OrgUserDTO.class));
    }

    @Test
    @DisplayName("初始化管理员时账号在租户内已存在应抛业务异常")
    void should_reject_init_admin_when_account_exists_in_tenant() {
        OrgTenantDTO tenant = new OrgTenantDTO()
            .setId(100L)
            .setTenantStatus(OrgTenantStateEnum.NORMAL);
        when(tenantManager.getDtoById(100L)).thenReturn(tenant);
        when(userManager.getUserByAccountAndTenant("admin", 100L)).thenReturn(new OrgUserDTO());

        OrgTenantInitAdminParam param = new OrgTenantInitAdminParam()
            .setTenantId(100L)
            .setAccount("admin")
            .setNickname("管理员")
            .setPassword("Password1");

        assertThrows(BusinessException.class, () -> tenantService.initTenantAdmin(param));
        verify(userManager, never()).add(any(OrgUserDTO.class));
    }

    @Test
    @DisplayName("过期租户不允许初始化管理员")
    void should_reject_init_admin_when_tenant_expired() {
        OrgTenantDTO tenant = new OrgTenantDTO()
            .setId(100L)
            .setTenantStatus(OrgTenantStateEnum.NORMAL)
            .setExpireTime(LocalDateTime.now().minusDays(1));
        when(tenantManager.getDtoById(100L)).thenReturn(tenant);

        OrgTenantInitAdminParam param = new OrgTenantInitAdminParam()
            .setTenantId(100L)
            .setAccount("admin")
            .setNickname("管理员")
            .setPassword("Password1");

        assertThrows(BusinessException.class, () -> tenantService.initTenantAdmin(param));
    }

    @Test
    @DisplayName("初始化管理员成功时用户写入目标租户")
    void should_create_admin_in_target_tenant() {
        OrgTenantDTO tenant = new OrgTenantDTO()
            .setId(100L)
            .setTenantStatus(OrgTenantStateEnum.NORMAL);
        when(tenantManager.getDtoById(100L)).thenReturn(tenant);
        when(userManager.getUserByAccountAndTenant("admin", 100L)).thenReturn(null);
        when(userManager.add(any(OrgUserDTO.class))).thenAnswer(invocation -> {
            OrgUserDTO dto = invocation.getArgument(0);
            assertEquals(100L, dto.getTenantId());
            return true;
        });

        OrgTenantInitAdminParam param = new OrgTenantInitAdminParam()
            .setTenantId(100L)
            .setAccount("admin")
            .setNickname("管理员")
            .setPassword("Password1");

        assertDoesNotThrow(() -> tenantService.initTenantAdmin(param));
        verify(userManager).add(any(OrgUserDTO.class));
    }
}
