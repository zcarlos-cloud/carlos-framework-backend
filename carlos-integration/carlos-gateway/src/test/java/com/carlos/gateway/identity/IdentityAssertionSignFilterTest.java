package com.carlos.gateway.identity;

import com.carlos.core.auth.AuthConstant;
import com.carlos.core.auth.IdentityAssertion;
import com.carlos.core.auth.IdentityAssertionCodec;
import com.carlos.core.auth.IdentityAssertionProperties;
import com.carlos.core.auth.UserContext;
import com.carlos.core.constant.HttpHeadersConstant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IdentityAssertionSignFilter 单元测试
 *
 * <p>关注三件事：开关语义、断言内容正确、以及各种缺失场景下「降级不抛异常」。</p>
 */
class IdentityAssertionSignFilterTest {

    private static final String SECRET = "unit-test-secret-key-32-bytes-long!!";

    private IdentityAssertionProperties properties(boolean signEnabled, String secret) {
        IdentityAssertionProperties properties = new IdentityAssertionProperties();
        properties.setSignEnabled(signEnabled);
        properties.setSecret(secret);
        properties.setTtl(Duration.ofMinutes(5));
        return properties;
    }

    private UserContext userContext() {
        Set<Serializable> roleIds = new LinkedHashSet<>();
        roleIds.add("SUPER_ADMIN");

        UserContext userContext = new UserContext();
        userContext.setUserId("2076141414136696832");
        userContext.setAccount("admin");
        userContext.setRoleIds(roleIds);
        userContext.setDepartmentId(2076141404842119168L);
        userContext.setTenantId(1L);
        return userContext;
    }

    /**
     * @param userContext 为 null 表示 exchange 上没有身份（白名单路径/未认证）
     * @return 过滤链下游看到的断言头值（未签发时为 null）
     */
    private String run(IdentityAssertionProperties properties, UserContext userContext) {
        IdentityAssertionSignFilter filter = new IdentityAssertionSignFilter(properties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/sys/route/getUserRoutes").build());
        if (userContext != null) {
            exchange.getAttributes().put(AuthConstant.USER_CONTEXT, userContext);
        }

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            captured.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerWebExchange result = captured.get();
        assertNotNull(result, "过滤器必须放行到下游");
        return result.getRequest().getHeaders().getFirst(HttpHeadersConstant.X_USER_CONTEXT);
    }

    @Test
    @DisplayName("开启且有密钥时签发可验签的断言，内容与身份一致")
    void should_sign_verifiable_assertion() {
        String headerValue = run(properties(true, SECRET), userContext());

        assertNotNull(headerValue);
        assertTrue(headerValue.startsWith(IdentityAssertionCodec.VERSION + "."));

        IdentityAssertion assertion = IdentityAssertionCodec
            .verifyAndParse(headerValue, SECRET, null)
            .orElseThrow(() -> new AssertionError("签发的断言必须能被同一密钥验签"));

        assertEquals(2076141414136696832L, assertion.getUid());
        assertEquals("admin", assertion.getAcc());
        assertEquals(1, assertion.getRoles().size());
        assertTrue(assertion.getRoles().contains("SUPER_ADMIN"));
        assertEquals(2076141404842119168L, assertion.getDept());
        assertEquals(1L, assertion.getTen());
        assertNotNull(assertion.getIat());
        assertEquals(300L, assertion.getExp() - assertion.getIat(), "有效期应为配置的 ttl");
    }

    @Test
    @DisplayName("开关关闭时不签发断言（单侧回滚路径）")
    void should_not_sign_when_disabled() {
        assertNull(run(properties(false, SECRET), userContext()));
    }

    @Test
    @DisplayName("开关开启但密钥缺失时不签发，且不抛异常")
    void should_not_sign_when_secret_missing() {
        assertNull(run(properties(true, null), userContext()));
        assertNull(run(properties(true, "  "), userContext()));
    }

    @Test
    @DisplayName("exchange 上没有身份时不签发（白名单路径/未认证，下游降级为匿名）")
    void should_not_sign_without_user_context() {
        assertNull(run(properties(true, SECRET), null));
    }

    @Test
    @DisplayName("userId 非数字时不签发（降级而非抛异常）")
    void should_not_sign_when_user_id_not_numeric() {
        UserContext broken = userContext();
        broken.setUserId("not-a-number");

        assertNull(run(properties(true, SECRET), broken));
    }

    @Test
    @DisplayName("租户/部门非法时不影响签发（按空处理）")
    void should_tolerate_invalid_tenant_or_dept() {
        UserContext broken = userContext();
        broken.setTenantId("not-a-number");
        broken.setDepartmentId("also-bad");

        String headerValue = run(properties(true, SECRET), broken);

        assertNotNull(headerValue, "非法租户/部门不应导致整条身份链失败");
        IdentityAssertion assertion = IdentityAssertionCodec
            .verifyAndParse(headerValue, SECRET, null)
            .orElseThrow();
        assertNull(assertion.getTen());
        assertNull(assertion.getDept());
    }

}
