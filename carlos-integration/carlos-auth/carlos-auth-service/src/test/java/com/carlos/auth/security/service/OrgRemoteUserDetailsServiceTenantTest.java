package com.carlos.auth.security.service;

import com.carlos.auth.provider.UserInfo;
import com.carlos.auth.provider.UserProvider;
import com.carlos.auth.security.TenantLoginChecker;
import com.carlos.auth.security.password.PasswordProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrgRemoteUserDetailsService 租户定位单元测试
 *
 * <p>S2 租户管理（代码评审 H2 回归）：租户定位只允许发生在 formLogin 登录处理请求
 * （POST /login）；令牌颁发（授权码交换）与刷新请求没有 tenantCode 表单字段，
 * 必须回落到非租户化加载，否则开启开关后非平台租户用户换发的令牌会丢失 claims。</p>
 *
 * @author Carlos
 */
class OrgRemoteUserDetailsServiceTenantTest {

    private UserProvider userProvider;
    private TenantLoginChecker tenantLoginChecker;
    private OrgRemoteUserDetailsService service;

    @BeforeEach
    void setUp() {
        userProvider = mock(UserProvider.class);
        PasswordProperties passwordProperties = new PasswordProperties();
        tenantLoginChecker = mock(TenantLoginChecker.class);
        service = new OrgRemoteUserDetailsService(userProvider, passwordProperties, tenantLoginChecker);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private UserInfo mockUser() {
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUserId()).thenReturn(100L);
        when(userInfo.getUsername()).thenReturn("admin");
        when(userProvider.getUserRoleCodes(100L)).thenReturn(java.util.List.of());
        return userInfo;
    }

    private void bindRequest(String uri, String tenantCode) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        if (tenantCode != null) {
            request.setParameter("tenantCode", tenantCode);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("formLogin 请求（/login）携带 tenantCode 时按租户定位用户")
    void should_load_user_by_tenant_when_login_request_with_tenant_code() {
        UserInfo userInfo = mockUser();
        bindRequest("/login", "ACME");
        when(tenantLoginChecker.resolveTenantId("ACME")).thenReturn(100L);
        when(userProvider.loadUserByIdentifierAndTenant("admin", 100L)).thenReturn(userInfo);

        assertNotNull(service.loadLoginUserInfo("admin"));

        verify(userProvider).loadUserByIdentifierAndTenant("admin", 100L);
        verify(userProvider, never()).loadUserByIdentifier(anyString());
    }

    @Test
    @DisplayName("令牌颁发请求（/oauth2/token）无 tenantCode 字段时走非租户化加载")
    void should_load_user_unscoped_when_token_request_without_tenant_code() {
        UserInfo userInfo = mockUser();
        bindRequest("/oauth2/token", null);
        when(userProvider.loadUserByIdentifier("admin")).thenReturn(userInfo);

        assertNotNull(service.loadLoginUserInfo("admin"));

        verify(userProvider).loadUserByIdentifier("admin");
        // 关键断言：令牌颁发时不得再解析租户（否则被默认租户 0 带偏导致 claims 丢失）
        verify(tenantLoginChecker, never()).resolveTenantId(anyString());
        verify(userProvider, never()).loadUserByIdentifierAndTenant(anyString(), anyLong());
    }

    @Test
    @DisplayName("无请求上下文（定时任务等）时走非租户化加载")
    void should_load_user_unscoped_when_no_request_context() {
        UserInfo userInfo = mockUser();
        when(userProvider.loadUserByIdentifier("admin")).thenReturn(userInfo);

        assertNotNull(service.loadLoginUserInfo("admin"));

        verify(userProvider).loadUserByIdentifier("admin");
        verify(tenantLoginChecker, never()).resolveTenantId(anyString());
    }

    @Test
    @DisplayName("登录请求未携带 tenantCode 时由 checker 回落默认租户")
    void should_delegate_blank_tenant_code_to_checker_on_login_request() {
        UserInfo userInfo = mockUser();
        bindRequest("/login", null);
        // checker 内部：开关开启 + 空编码 → defaultTenantId(0)
        when(tenantLoginChecker.resolveTenantId(null)).thenReturn(0L);
        when(userProvider.loadUserByIdentifierAndTenant("admin", 0L)).thenReturn(userInfo);

        assertNotNull(service.loadLoginUserInfo("admin"));

        verify(userProvider).loadUserByIdentifierAndTenant("admin", 0L);
    }

    @Test
    @DisplayName("checker 返回 null（开关关闭）时保持原有非租户化加载")
    void should_load_user_unscoped_when_checker_returns_null() {
        UserInfo userInfo = mockUser();
        bindRequest("/login", "ACME");
        when(tenantLoginChecker.resolveTenantId("ACME")).thenReturn(null);
        when(userProvider.loadUserByIdentifier("admin")).thenReturn(userInfo);

        assertEquals(100L, service.loadLoginUserInfo("admin").getId());

        verify(userProvider).loadUserByIdentifier("admin");
        verify(userProvider, never()).loadUserByIdentifierAndTenant(anyString(), anyLong());
    }
}
