package com.carlos.security.config;

import com.carlos.core.auth.IdentityAssertion;
import com.carlos.core.auth.IdentityAssertionCodec;
import com.carlos.core.auth.IdentityAssertionProperties;
import com.carlos.core.constant.HttpHeadersConstant;
import com.carlos.security.auth.IdentityAssertionVerifier;
import com.carlos.security.auth.UserContext;
import com.carlos.security.permission.PermissionProvider;
import com.carlos.security.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * UserContextAuthenticationFilter 行为测试
 *
 * <p>身份来源只有一个：网关签发的 {@code X-User-Context} 断言。本测试锁定三类行为 ——
 * 验签通过则建立身份、断言缺失则匿名、断言非法则匿名且<b>不存在任何更弱的回退通道</b>。</p>
 */
class UserContextAuthenticationFilterTest {

    private static final String SECRET = "unit-test-secret-key-32-bytes-long!!";
    private static final long USER_ID = 2076141414136696832L;

    private final PermissionProvider permissionProvider = new PermissionProvider() {
        @Override
        public Set<String> getPermissions(String roleCode) {
            return Set.of("user:read");
        }

        @Override
        public Set<String> getPermissions(Set<String> roleCodes) {
            return Set.of("user:read", "user:write");
        }

        @Override
        public void savePermissions(String roleCode, Set<String> permissions) {
            // no-op
        }

        @Override
        public void savePermissions(String roleCode, Set<String> permissions, long ttl) {
            // no-op
        }

        @Override
        public void clearCache(String roleCode) {
            // no-op
        }
    };

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private IdentityAssertionProperties properties(boolean verifyEnabled, String secret) {
        IdentityAssertionProperties properties = new IdentityAssertionProperties();
        properties.setVerifyEnabled(verifyEnabled);
        properties.setSecret(secret);
        properties.setClockSkew(Duration.ofSeconds(60));
        properties.setMaxLifetime(Duration.ofMinutes(10));
        return properties;
    }

    private UserContextAuthenticationFilter filter(IdentityAssertionProperties properties) {
        UserContextAuthenticationFilter filter = new UserContextAuthenticationFilter(permissionProvider);
        filter.setIdentityAssertionVerifier(new IdentityAssertionVerifier(properties));
        // 过滤器自身的 whitelistPaths 默认是空集合（生产由 ResourceServerAutoConfiguration 下发默认 6 条）
        filter.setWhitelistPaths(Set.of("/actuator/**"));
        return filter;
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr("192.168.117.1");
        return request;
    }

    private String validAssertion() {
        long now = Instant.now().getEpochSecond();
        return IdentityAssertionCodec.encode(new IdentityAssertion()
            .setUid(USER_ID)
            .setAcc("admin")
            .setRoles(List.of("SUPER_ADMIN"))
            .setDept(99L)
            .setTen(1L)
            .setIat(now)
            .setExp(now + 300), SECRET);
    }

    private void doFilter(UserContextAuthenticationFilter filter, MockHttpServletRequest request) throws Exception {
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    // ------------------------- 验签通过 -------------------------

    @Test
    @DisplayName("断言有效：认证成功，身份与权限来自断言")
    void should_authenticate_from_valid_assertion() throws Exception {
        MockHttpServletRequest request = request("/sys/route/getUserRoutes");
        request.addHeader(HttpHeadersConstant.X_USER_CONTEXT, validAssertion());

        doFilter(filter(properties(true, SECRET)), request);

        UserContext user = SecurityUtils.getCurrentUser();
        assertNotNull(user, "有效断言必须建立登录身份");
        assertEquals(USER_ID, user.getUserId());
        assertEquals("admin", user.getUsername());
        assertEquals(Set.of("SUPER_ADMIN"), user.getRoleCodes());
        assertEquals(99L, user.getDeptId());
        assertEquals(1L, user.getTenantId());
        assertEquals(Set.of("user:read", "user:write"), user.getPermissions());
    }

    // ------------------------- 断言缺失 / 非法 -------------------------

    @Test
    @DisplayName("断言缺失：降级为匿名（网关白名单路径本就不签发断言）")
    void should_be_anonymous_when_assertion_absent() throws Exception {
        MockHttpServletRequest request = request("/sys/route/getUserRoutes");

        doFilter(filter(properties(true, SECRET)), request);

        assertNull(SecurityUtils.getCurrentUser());
    }

    @Test
    @DisplayName("断言非法：匿名，且不给任何更弱的回退通道")
    void should_be_anonymous_when_assertion_invalid() throws Exception {
        MockHttpServletRequest request = request("/sys/route/getUserRoutes");
        request.addHeader(HttpHeadersConstant.X_USER_CONTEXT, validAssertion() + "tampered");
        // 同时塞入伪造的明文头：它们已不被任何通道采信
        request.addHeader(HttpHeadersConstant.X_USER_ID, String.valueOf(USER_ID));
        request.addHeader(HttpHeadersConstant.X_USER_ROLES, "SUPER_ADMIN");

        doFilter(filter(properties(true, SECRET)), request);

        assertNull(SecurityUtils.getCurrentUser());
    }

    @Test
    @DisplayName("明文身份头已彻底不被采信（X-User-* 通道已删除）")
    void should_ignore_plain_identity_headers() throws Exception {
        MockHttpServletRequest request = request("/sys/route/getUserRoutes");
        request.addHeader(HttpHeadersConstant.X_USER_ID, String.valueOf(USER_ID));
        request.addHeader(HttpHeadersConstant.X_USER_NAME, "admin");
        request.addHeader(HttpHeadersConstant.X_USER_ROLES, "SUPER_ADMIN");

        doFilter(filter(properties(true, SECRET)), request);

        assertNull(SecurityUtils.getCurrentUser(),
            "删掉 trusted-ip 后明文头再无信任锚，必须不被采信");
    }

    @Test
    @DisplayName("断言过期：降级为匿名")
    void should_be_anonymous_when_assertion_expired() throws Exception {
        long now = Instant.now().getEpochSecond();
        String expired = IdentityAssertionCodec.encode(new IdentityAssertion()
            .setUid(USER_ID).setRoles(List.of("SUPER_ADMIN"))
            .setIat(now - 3600).setExp(now - 1800), SECRET);

        MockHttpServletRequest request = request("/sys/route/getUserRoutes");
        request.addHeader(HttpHeadersConstant.X_USER_CONTEXT, expired);

        doFilter(filter(properties(true, SECRET)), request);

        assertNull(SecurityUtils.getCurrentUser());
    }

    @Test
    @DisplayName("断言非法 + strictHeaderAuth：返回 401")
    void should_return_401_when_assertion_invalid_and_strict() throws Exception {
        MockHttpServletRequest request = request("/sys/route/getUserRoutes");
        request.addHeader(HttpHeadersConstant.X_USER_CONTEXT, "v1.garbage.signature");

        MockHttpServletResponse response = new MockHttpServletResponse();
        UserContextAuthenticationFilter filter = filter(properties(true, SECRET));
        filter.setStrictHeaderAuth(true);
        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertNull(SecurityUtils.getCurrentUser());
    }

    @Test
    @DisplayName("验签通道关闭：断言被忽略，一律匿名")
    void should_skip_when_verify_disabled() throws Exception {
        MockHttpServletRequest request = request("/sys/route/getUserRoutes");
        request.addHeader(HttpHeadersConstant.X_USER_CONTEXT, validAssertion());

        doFilter(filter(properties(false, SECRET)), request);

        assertNull(SecurityUtils.getCurrentUser());
    }

    @Test
    @DisplayName("密钥未配置：断言无法验签，降级为匿名且不抛异常")
    void should_be_anonymous_when_secret_missing() throws Exception {
        MockHttpServletRequest request = request("/sys/route/getUserRoutes");
        request.addHeader(HttpHeadersConstant.X_USER_CONTEXT, validAssertion());

        doFilter(filter(properties(true, null)), request);

        assertNull(SecurityUtils.getCurrentUser());
    }

    // ------------------------- 白名单 -------------------------

    @Test
    @DisplayName("白名单路径完全跳过（即使带了合法断言也不建立身份）")
    void should_skip_whitelisted_path() throws Exception {
        MockHttpServletRequest request = request("/actuator/health");
        request.addHeader(HttpHeadersConstant.X_USER_CONTEXT, validAssertion());

        doFilter(filter(properties(true, SECRET)), request);

        assertNull(SecurityUtils.getCurrentUser());
    }

}
