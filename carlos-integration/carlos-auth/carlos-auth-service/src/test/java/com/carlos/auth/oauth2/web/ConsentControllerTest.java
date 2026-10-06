package com.carlos.auth.oauth2.web;

import com.carlos.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 授权确认控制器单元测试
 *
 * @author Carlos
 * @since 3.0.0
 */
@ExtendWith(MockitoExtension.class)
class ConsentControllerTest {

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    @InjectMocks
    private ConsentController consentController;

    @Test
    @DisplayName("应该渲染授权确认页面并携带客户端和Scope信息")
    void should_render_consent_page_with_client_and_scopes() {
        // given
        String clientId = "test-client";
        String scope = "openid profile email";
        String state = "test-state-123";
        Model model = new ConcurrentModel();

        RegisteredClient client = RegisteredClient.withId("client-id")
            .clientId(clientId)
            .clientName("测试应用")
            .clientSecret("{noop}secret")
            .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/callback")
            .scope("openid")
            .scope("profile")
            .scope("email")
            .build();

        when(registeredClientRepository.findByClientId(clientId)).thenReturn(client);

        // when
        String viewName = consentController.consent(clientId, scope, state, model);

        // then
        assertEquals("consent", viewName);
        assertEquals(clientId, model.getAttribute("clientId"));
        assertEquals("测试应用", model.getAttribute("clientName"));
        assertEquals(state, model.getAttribute("state"));

        @SuppressWarnings("unchecked")
        Set<String> scopes = (Set<String>) model.getAttribute("scopes");
        assertNotNull(scopes);
        assertEquals(3, scopes.size());
        assertTrue(scopes.contains("openid"));
        assertTrue(scopes.contains("profile"));
        assertTrue(scopes.contains("email"));

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> descriptions = (java.util.Map<String, String>) model.getAttribute("scopeDescriptions");
        assertNotNull(descriptions);
        assertEquals("获取您的基本身份标识（用户唯一标识）", descriptions.get("openid"));

        verify(registeredClientRepository, times(1)).findByClientId(clientId);
    }

    @Test
    @DisplayName("应该使用clientId作为客户端名称当clientName为空时")
    void should_use_client_id_as_name_when_client_name_is_empty() {
        // given
        String clientId = "unnamed-client";
        String scope = "openid";
        String state = "state";
        Model model = new ConcurrentModel();

        RegisteredClient client = RegisteredClient.withId(clientId)
            .clientId(clientId)
            .clientSecret("{noop}secret")
            .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/callback")
            .scope("openid")
            .build();

        when(registeredClientRepository.findByClientId(clientId)).thenReturn(client);

        // when
        consentController.consent(clientId, scope, state, model);

        // then
        assertEquals(clientId, model.getAttribute("clientName"));
    }

    @Test
    @DisplayName("应该抛出业务异常当客户端不存在时")
    void should_throw_business_exception_when_client_not_found() {
        // given
        String clientId = "non-existent";
        String state = "state";
        Model model = new ConcurrentModel();

        when(registeredClientRepository.findByClientId(clientId)).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> consentController.consent(clientId, "openid", state, model)
        );
        assertTrue(exception.getMessage().contains("客户端不存在"));
    }

    @Test
    @DisplayName("应该处理空的scope参数")
    void should_handle_empty_scope() {
        // given
        String clientId = "test-client";
        String state = "state";
        Model model = new ConcurrentModel();

        RegisteredClient client = RegisteredClient.withId("id")
            .clientId(clientId)
            .clientName("测试应用")
            .clientSecret("{noop}secret")
            .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/callback")
            .build();

        when(registeredClientRepository.findByClientId(clientId)).thenReturn(client);

        // when
        String viewName = consentController.consent(clientId, "", state, model);

        // then
        assertEquals("consent", viewName);
        @SuppressWarnings("unchecked")
        Set<String> scopes = (Set<String>) model.getAttribute("scopes");
        assertNotNull(scopes);
        assertTrue(scopes.isEmpty());
    }

    @Test
    @DisplayName("应该过滤掉客户端未注册的scope")
    void should_filter_out_unregistered_scopes() {
        // given
        String clientId = "test-client";
        String scope = "openid profile email malicious_scope";
        String state = "state";
        Model model = new ConcurrentModel();

        RegisteredClient client = RegisteredClient.withId("id")
            .clientId(clientId)
            .clientName("测试应用")
            .clientSecret("{noop}secret")
            .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/callback")
            .scope("openid")
            .scope("profile")
            .scope("email")
            .build();

        when(registeredClientRepository.findByClientId(clientId)).thenReturn(client);

        // when
        String viewName = consentController.consent(clientId, scope, state, model);

        // then
        assertEquals("consent", viewName);
        @SuppressWarnings("unchecked")
        Set<String> scopes = (Set<String>) model.getAttribute("scopes");
        assertNotNull(scopes);
        assertEquals(3, scopes.size());
        assertTrue(scopes.contains("openid"));
        assertTrue(scopes.contains("profile"));
        assertTrue(scopes.contains("email"));
        assertFalse(scopes.contains("malicious_scope"));
    }

    @Test
    @DisplayName("应该抛出异常当clientId长度超过限制时")
    void should_throw_exception_when_client_id_too_long() {
        // given
        String clientId = "a".repeat(65);
        String state = "state";
        Model model = new ConcurrentModel();

        // when & then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> consentController.consent(clientId, "openid", state, model)
        );
        assertTrue(exception.getMessage().contains("客户端ID长度超过限制"));
    }
}
