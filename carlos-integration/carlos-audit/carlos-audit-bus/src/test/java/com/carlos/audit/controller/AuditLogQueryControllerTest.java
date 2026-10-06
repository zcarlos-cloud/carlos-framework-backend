package com.carlos.audit.controller;

import com.carlos.audit.pojo.param.AuditLogQueryPageParam;
import com.carlos.audit.service.AuditLogQueryService;
import com.carlos.security.auth.UserContext;
import com.carlos.security.auth.UserContextAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;

/**
 * AuditLogQueryController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuditLogQueryControllerTest {

    @Mock
    private AuditLogQueryService queryService;

    @InjectMocks
    private AuditLogQueryController controller;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("page 方法应该从请求上下文获取租户ID")
    void should_use_tenant_id_for_page_query() {
        setTenantId(42L);

        controller.page(newQueryParam());

        ArgumentCaptor<String> tenantIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryService).pageQuery(tenantIdCaptor.capture(), any(AuditLogQueryPageParam.class));
        assertEquals("42", tenantIdCaptor.getValue());
    }

    @Test
    @DisplayName("getRealtimeStats 方法应该从请求上下文获取租户ID")
    void should_use_tenant_id_for_realtime_stats() {
        setTenantId(42L);

        controller.getRealtimeStats(LocalDate.now());

        ArgumentCaptor<String> tenantIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryService).getRealtimeStats(tenantIdCaptor.capture(), any(LocalDate.class));
        assertEquals("42", tenantIdCaptor.getValue());
    }

    @Test
    @DisplayName("listRiskEvents 方法应该从请求上下文获取租户ID")
    void should_use_tenant_id_for_risk_events() {
        setTenantId(42L);

        controller.listRiskEvents(50, null, null);

        ArgumentCaptor<String> tenantIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryService).listRiskEvents(tenantIdCaptor.capture(), anyInt(), any(), any());
        assertEquals("42", tenantIdCaptor.getValue());
    }

    @Test
    @DisplayName("请求上下文无租户ID时应回退到默认租户0")
    void should_fallback_to_default_tenant_id_when_not_present() {
        UserContext userContext = new UserContext();
        setUserContext(userContext);

        controller.page(newQueryParam());

        ArgumentCaptor<String> tenantIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryService).pageQuery(tenantIdCaptor.capture(), any(AuditLogQueryPageParam.class));
        assertEquals("0", tenantIdCaptor.getValue());
    }

    @Test
    @DisplayName("请求上下文不存在时应回退到默认租户0")
    void should_fallback_to_default_tenant_id_when_no_request_context() {
        controller.page(newQueryParam());

        ArgumentCaptor<String> tenantIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(queryService).pageQuery(tenantIdCaptor.capture(), any(AuditLogQueryPageParam.class));
        assertEquals("0", tenantIdCaptor.getValue());
    }

    private AuditLogQueryPageParam newQueryParam() {
        AuditLogQueryPageParam param = new AuditLogQueryPageParam();
        param.setCurrent(1);
        param.setSize(20);
        return param;
    }

    private void setTenantId(Long tenantId) {
        UserContext userContext = new UserContext();
        userContext.setTenantId(tenantId);
        setUserContext(userContext);
    }

    private void setUserContext(UserContext userContext) {
        UserContextAuthenticationToken authentication =
            new UserContextAuthenticationToken(userContext, userContext.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
