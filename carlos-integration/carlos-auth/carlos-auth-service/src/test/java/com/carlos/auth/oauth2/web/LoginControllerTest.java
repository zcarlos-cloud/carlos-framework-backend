package com.carlos.auth.oauth2.web;

import com.carlos.auth.config.OAuth2Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 登录控制器单元测试
 *
 * @author Carlos
 * @since 3.0.0
 */
@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @InjectMocks
    private LoginController loginController;

    @Mock
    private OAuth2Properties oauth2Properties;

    @Mock
    private LoginMethodValidator methodValidator;

    @BeforeEach
    void setUp() {
        when(oauth2Properties.getLogin()).thenReturn(new OAuth2Properties.Login());
    }

    @Test
    @DisplayName("应该正常渲染登录页面当无错误参数时")
    void should_render_login_page_without_error() {
        // given
        Model model = new ConcurrentModel();

        // when
        String viewName = loginController.login(null, model);

        // then
        assertEquals("login", viewName);
        assertNull(model.getAttribute("error"));
    }

    @Test
    @DisplayName("应该渲染登录页面并携带错误消息当认证失败时")
    void should_render_login_page_with_error_message() {
        // given
        Model model = new ConcurrentModel();

        // when
        String viewName = loginController.login("invalid_credentials", model);

        // then
        assertEquals("login", viewName);
        assertNotNull(model.getAttribute("error"));
        assertEquals("用户名或密码错误，请重试", model.getAttribute("error"));
    }
}
