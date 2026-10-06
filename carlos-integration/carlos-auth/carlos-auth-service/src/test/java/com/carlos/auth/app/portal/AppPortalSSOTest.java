package com.carlos.auth.app.portal;

import com.carlos.auth.app.manager.AppCategoryManager;
import com.carlos.auth.app.manager.AppPortalManager;
import com.carlos.auth.app.manager.AppRoleAccessManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 应用门户 SSO 登录流程集成测试。
 *
 * <p>测试完整的 OAuth2 授权码流程：表单登录 → 授权 → Token 交换 → 应用门户 API 调用。</p>
 */
@SpringBootTest(
        classes = AppPortalTestApplication.class,
        webEnvironment = WebEnvironment.MOCK
)
@TestPropertySource(properties = {
        // Spring Boot 3.x: 允许测试 Bean 覆盖，避免与生产配置冲突
        "logging.level.org.springframework.security=DEBUG",
        "logging.level.org.springframework.security.oauth2=DEBUG",
        "logging.level.com.carlos.auth=DEBUG",
        "spring.main.allow-bean-definition-overriding=true",
        // ===== OAuth2 授权服务器配置 =====
        "carlos.auth.authorization-server.enabled=true",
        "carlos.auth.jwt.key-id=carlos-test-key",
        "carlos.auth.jwt.issuer=http://localhost:9511",
        "carlos.auth.jwt.include-user-info=true",
        "carlos.auth.jwt.key-size=2048",
        // ===== carlos-framework-portal 客户端（公共客户端，PKCE only） =====
        "carlos.auth.clients[0].client-id=carlos-framework-portal",
        "carlos.auth.clients[0].client-name=Carlos Portal",
        "carlos.auth.clients[0].authorization-grant-types=authorization_code,refresh_token",
        "carlos.auth.clients[0].redirect-uris=http://localhost:5200/oauth2/callback",
        "carlos.auth.clients[0].scopes=openid,profile",
        "carlos.auth.clients[0].require-authorization-consent=false",
        "carlos.auth.clients[0].require-proof-key=true",
        // ===== 雪花算法配置 =====
        "spring.application.name=carlos-auth-test",
        "carlos.snowflake.tag=auth-test",
        "carlos.snowflake.datacenter-id=1",
        "carlos.snowflake.worker-id=1",
        "carlos.snowflake.enabled=false",
        // ===== 加密配置（EncryptAutoConfiguration 需要） =====
        "carlos.encrypt.sm4.enabled=true",
        "carlos.encrypt.sm4.key=asdfghjklmnbvcxz",
        "carlos.encrypt.sm4.iv=mnbvcxzpoiuytre1",
        // ===== 禁用外部服务 =====
        "spring.redis.client-type=none",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        // ===== Spring Boot 4.x: 排除不需要的自动配置 =====
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,"
                + "org.springframework.cloud.openfeign.FeignAutoConfiguration,"
                + "org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration,"
                + "org.springframework.cloud.commons.util.SpringCloudAutoConfiguration,"
                + "com.alibaba.cloud.nacos.NacosConfigAutoConfiguration,"
                + "com.alibaba.cloud.nacos.NacosDiscoveryAutoConfiguration,"
                + "com.alibaba.cloud.nacos.endpoint.NacosConfigEndpointAutoConfiguration,"
                + "com.alibaba.cloud.nacos.discovery.NacosDiscoveryClientAutoConfiguration,"
                + "com.alibaba.cloud.nacos.loadbalancer.NacosDiscoveryClientAutoConfiguration,"
                + "org.springframework.cloud.openfeign.loadbalancer.FeignLoadBalancerAutoConfiguration,"
                + "com.carlos.snowflake.SnowflakeConfig,"
                + "com.carlos.snowflake.cache.SnowflakeRedisCacheAutoConfiguration,"
                + "com.carlos.snowflake.cache.SnowflakeLocalCacheAutoConfiguration,"
                + "com.carlos.redis.config.RedisCacheConfig,"
                + "org.springframework.boot.jdbc.autoconfigure.health.DataSourceHealthContributorAutoConfiguration",
        "spring.cloud.loadbalancer.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.refresh.enabled=false",
        "spring.cloud.compatibility-verifier.enabled=false",
})
@Import(AppPortalTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("应用门户 SSO 登录流程集成测试")
class AppPortalSSOTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private AppPortalManager appPortalManager;

    @Autowired
    private AppCategoryManager appCategoryManager;

    @Autowired
    private AppRoleAccessManager appRoleAccessManager;

    private MockMvc mockMvc;

    // PKCE 参数
    private static final String CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String CODE_CHALLENGE;

    static {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(CODE_VERIFIER.getBytes(StandardCharsets.US_ASCII));
            CODE_CHALLENGE = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .build();

        // 默认 mock 设置：返回空数据
        when(appPortalManager.listPortalVisibleApps()).thenReturn(Collections.emptyList());
        when(appCategoryManager.listAll()).thenReturn(Collections.emptyList());
        when(appRoleAccessManager.getAllRestrictedAppIds()).thenReturn(Collections.emptyList());
        when(appRoleAccessManager.getAccessibleAppIds(any())).thenReturn(Collections.emptyList());
    }

    // ======================== 测试 1: 表单登录 ========================

    @Test
    @DisplayName("测试 1: 表单登录 POST /login - 应返回 302 重定向")
    void should_redirect_and_set_session_cookie_when_login_success() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin")
                        .param("password", "123456")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // 验证登录成功后有重定向（302）
        String redirectUrl = result.getResponse().getRedirectedUrl();
        assertNotNull(redirectUrl, "登录成功应返回重定向 URL");
        // 登录成功后有 session（Spring MockMvc 内部维护 session，不通过 Set-Cookie 头暴露）
        assertNotNull(result.getRequest().getSession(false), "登录成功应创建 session");
        System.out.println("Test 1 PASSED: Login successful, redirect to " + redirectUrl);
    }

    @Test
    @DisplayName("测试 1-b: 密码错误时登录失败，重定向到 /login?error")
    void should_redirect_to_login_error_when_bad_credentials() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin")
                        .param("password", "wrong_password")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
        System.out.println("Test 1-b PASSED: Bad credentials redirected to login?error");
    }

    // ======================== 测试 2-5: 完整 OAuth2 流程 ========================

    @Test
    @DisplayName("测试 2-5: 完整 OAuth2 授权码流程（登录→授权→Token交换→应用门户API）")
    void should_complete_full_oauth2_flow_and_access_portal_apis() throws Exception {
        // ---- Step 1: 登录获取 session ----
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin")
                        .param("password", "123456")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // MockMvc 内部维护 session，通过 session() 传递
        org.springframework.mock.web.MockHttpSession session = (org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession();
        assertNotNull(session, "应获取到 session");
        System.out.println("  Step 1 PASSED: Login successful, got session");

        // ---- Step 2: 授权码请求 (测试 2) ----
        String authorizeUrl = "/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=carlos-framework-portal"
                + "&redirect_uri=http://localhost:5200/oauth2/callback"
                + "&scope=openid"
                + "&state=test-state-123"
                + "&code_challenge=" + CODE_CHALLENGE
                + "&code_challenge_method=S256";

        MvcResult authorizeResult = mockMvc.perform(get(authorizeUrl)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectUrl = authorizeResult.getResponse().getRedirectedUrl();
        assertNotNull(redirectUrl, "授权响应应包含重定向 URL");
        assertTrue(redirectUrl.startsWith("http://localhost:5200/oauth2/callback"),
                "应重定向到 carlos-framework-portal 的回调地址，实际: " + redirectUrl);
        System.out.println("  Step 2 PASSED: Authorization redirect to portal callback");

        // 提取授权码
        String authCode = extractParameter(redirectUrl, "code");
        assertNotNull(authCode, "重定向 URL 应包含 code 参数");
        System.out.println("  Extracted auth code: " + authCode.substring(0, 10) + "...");
        System.out.println("  Test 2 PASSED: OAuth2 authorization code obtained");

        // ---- Step 3: Token 交换 (测试 3) ----
        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", authCode)
                        .param("redirect_uri", "http://localhost:5200/oauth2/callback")
                        .param("client_id", "carlos-framework-portal")
                        .param("code_verifier", CODE_VERIFIER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andReturn();

        String responseBody = tokenResult.getResponse().getContentAsString();
        String accessToken = extractJsonValue(responseBody, "access_token");
        assertNotNull(accessToken, "应获取到 access_token");
        System.out.println("  Step 3 PASSED: Token exchange successful");
        System.out.println("  Test 3 PASSED: access_token obtained");

        // ---- Step 4: 应用门户 API - /auth/app/portal/apps (测试 4) ----
        mockMvc.perform(get("/auth/app/portal/apps")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray());
        System.out.println("  Test 4 PASSED: Portal apps API returned 200 with data array");

        // ---- Step 5: 应用门户 API - /auth/app/portal/categories (测试 5) ----
        mockMvc.perform(get("/auth/app/portal/categories")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isArray());
        System.out.println("  Test 5 PASSED: Portal categories API returned 200 with data array");
    }

    // ======================== 测试 3-b: 无授权码时 Token 交换应失败 ========================

    @Test
    @DisplayName("测试 3-b: 无效授权码时 Token 交换应返回 400")
    void should_return_bad_request_when_invalid_authorization_code() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", "invalid-code-12345")
                        .param("redirect_uri", "http://localhost:5200/oauth2/callback")
                        .param("client_id", "carlos-framework-portal")
                        .param("code_verifier", CODE_VERIFIER))
                .andExpect(status().isBadRequest());
        System.out.println("Test 3-b PASSED: Invalid code rejected with 400");
    }

    // ======================== 测试 4-b: 无 Token 访问应用门户 API 应返回 401 ========================

    @Test
    @DisplayName("测试 4-b: 无 Bearer Token 时访问应用门户 API 应返回 401")
    void should_return_unauthorized_when_no_bearer_token() throws Exception {
        mockMvc.perform(get("/auth/app/portal/apps"))
                .andExpect(status().isUnauthorized());
        System.out.println("Test 4-b PASSED: Unauthorized without Bearer token");
    }

    // ======================== 辅助方法 ========================

    private String extractCookie(MvcResult result, String cookieName) {
        List<String> cookies = result.getResponse().getHeaders("Set-Cookie");
        if (cookies == null) {
            return null;
        }
        for (String cookie : cookies) {
            if (cookie.startsWith(cookieName + "=")) {
                return cookie.substring(cookieName.length() + 1, cookie.indexOf(";"));
            }
        }
        return null;
    }

    private String extractParameter(String url, String paramName) {
        if (url == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("[?&]" + paramName + "=([^&]*)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
