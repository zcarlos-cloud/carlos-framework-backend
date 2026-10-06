package com.carlos.auth.security;

import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgTenant;
import com.carlos.org.api.pojo.ao.OrgTenantAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TenantLoginChecker 单元测试
 *
 * <p>S2 租户管理：feature flag 关闭时零行为变化（一律返回 null）；
 * 开启后校验租户存在性 / 状态 / 有效期。</p>
 *
 * @author Carlos
 */
class TenantLoginCheckerTest {

    private TenantProperties tenantProperties;
    private ApiOrgTenant apiOrgTenant;
    private TenantLoginChecker checker;

    @BeforeEach
    void setUp() {
        tenantProperties = new TenantProperties();
        apiOrgTenant = mock(ApiOrgTenant.class);
        checker = new TenantLoginChecker(tenantProperties, apiOrgTenant);
    }

    @Test
    @DisplayName("功能关闭时无论是否携带租户编码都返回 null")
    void should_return_null_when_feature_disabled() {
        tenantProperties.setEnabled(false);

        assertNull(checker.resolveTenantId(null));
        assertNull(checker.resolveTenantId("PLATFORM"));
    }

    @Test
    @DisplayName("功能开启但未携带租户编码时返回默认租户ID")
    void should_return_default_tenant_when_code_blank() {
        tenantProperties.setEnabled(true);

        assertEquals(0L, checker.resolveTenantId(null));
        assertEquals(0L, checker.resolveTenantId("  "));
    }

    @Test
    @DisplayName("租户不存在时抛出 BadCredentialsException")
    void should_throw_when_tenant_not_found() {
        tenantProperties.setEnabled(true);
        when(apiOrgTenant.getByCode("NOPE")).thenReturn(Result.success(null));

        assertThrows(BadCredentialsException.class, () -> checker.resolveTenantId("NOPE"));
    }

    @Test
    @DisplayName("租户停用时抛出 DisabledException")
    void should_throw_when_tenant_disabled() {
        tenantProperties.setEnabled(true);
        OrgTenantAO tenant = new OrgTenantAO()
            .setId(100L)
            .setTenantCode("ACME")
            .setTenantStatus(2);
        when(apiOrgTenant.getByCode("ACME")).thenReturn(Result.success(tenant));

        assertThrows(DisabledException.class, () -> checker.resolveTenantId("ACME"));
    }

    @Test
    @DisplayName("租户过期时抛出 DisabledException")
    void should_throw_when_tenant_expired() {
        tenantProperties.setEnabled(true);
        OrgTenantAO tenant = new OrgTenantAO()
            .setId(100L)
            .setTenantCode("ACME")
            .setTenantStatus(1)
            .setExpireTime(LocalDateTime.now().minusDays(1));
        when(apiOrgTenant.getByCode("ACME")).thenReturn(Result.success(tenant));

        assertThrows(DisabledException.class, () -> checker.resolveTenantId("ACME"));
    }

    @Test
    @DisplayName("租户正常且未过期时返回租户ID")
    void should_return_tenant_id_when_tenant_normal() {
        tenantProperties.setEnabled(true);
        OrgTenantAO tenant = new OrgTenantAO()
            .setId(100L)
            .setTenantCode("ACME")
            .setTenantStatus(1)
            .setExpireTime(LocalDateTime.now().plusDays(30));
        when(apiOrgTenant.getByCode("ACME")).thenReturn(Result.success(tenant));

        assertEquals(100L, checker.resolveTenantId("ACME"));
    }

    @Test
    @DisplayName("租户正常且永不过期时返回租户ID")
    void should_return_tenant_id_when_tenant_never_expires() {
        tenantProperties.setEnabled(true);
        OrgTenantAO tenant = new OrgTenantAO()
            .setId(100L)
            .setTenantCode("ACME")
            .setTenantStatus(1);
        when(apiOrgTenant.getByCode("ACME")).thenReturn(Result.success(tenant));

        assertEquals(100L, checker.resolveTenantId("ACME"));
    }
}
