package com.carlos.gateway.identity;

import com.carlos.core.constant.HttpHeadersConstant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * IdentityHeaderSanitizeFilter 单元测试
 *
 * <p>核心断言两条：客户端自带的身份头必须被剔除；非身份头（如链路追踪头）必须保留，
 * 否则会误伤追踪与灰度。</p>
 */
class IdentityHeaderSanitizeFilterTest {

    private final IdentityHeaderSanitizeFilter filter = new IdentityHeaderSanitizeFilter();

    private ServerWebExchange run(MockServerHttpRequest.BaseBuilder<?> requestBuilder) {
        ServerWebExchange exchange = MockServerWebExchange.from(requestBuilder.build());
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            captured.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerWebExchange result = captured.get();
        assertNotNull(result, "过滤器必须放行到下游");
        return result;
    }

    @Test
    @DisplayName("剔除客户端自带的全部身份头（含断言本身）")
    void should_strip_client_supplied_identity_headers() {
        ServerWebExchange result = run(MockServerHttpRequest.get("/sys/route/getUserRoutes")
            .header(HttpHeadersConstant.X_USER_ID, "2076141414136696832")
            .header(HttpHeadersConstant.X_USER_NAME, "admin")
            .header(HttpHeadersConstant.X_USER_ROLES, "SUPER_ADMIN")
            .header(HttpHeadersConstant.X_TENANT_ID, "1")
            .header(HttpHeadersConstant.X_DEPT_ID, "1")
            .header(HttpHeadersConstant.X_USER_CONTEXT, "v1.forged.signature"));

        HttpHeaders headers = result.getRequest().getHeaders();

        assertNull(headers.getFirst(HttpHeadersConstant.X_USER_ID));
        assertNull(headers.getFirst(HttpHeadersConstant.X_USER_NAME));
        assertNull(headers.getFirst(HttpHeadersConstant.X_USER_ROLES));
        assertNull(headers.getFirst(HttpHeadersConstant.X_TENANT_ID));
        assertNull(headers.getFirst(HttpHeadersConstant.X_DEPT_ID));
        assertNull(headers.getFirst(HttpHeadersConstant.X_USER_CONTEXT), "客户端自带的断言必须被剔除");
    }

    @Test
    @DisplayName("保留非身份头（追踪/灰度/业务头不受影响）")
    void should_keep_unrelated_headers() {
        ServerWebExchange result = run(MockServerHttpRequest.get("/sys/route/getUserRoutes")
            .header(HttpHeadersConstant.X_TRACE_ID, "trace-123")
            .header(HttpHeadersConstant.X_REQUEST_ID, "req-123")
            .header(HttpHeadersConstant.X_GRAY_VERSION, "2.0.0")
            .header("X-Business-Header", "keep-me")
            .header(HttpHeadersConstant.AUTHORIZATION, "Bearer opaque-token")
            .header(HttpHeadersConstant.X_USER_ID, "forged"));

        HttpHeaders headers = result.getRequest().getHeaders();

        assertEquals("trace-123", headers.getFirst(HttpHeadersConstant.X_TRACE_ID));
        assertEquals("req-123", headers.getFirst(HttpHeadersConstant.X_REQUEST_ID));
        assertEquals("2.0.0", headers.getFirst(HttpHeadersConstant.X_GRAY_VERSION));
        assertEquals("keep-me", headers.getFirst("X-Business-Header"));
        assertEquals("Bearer opaque-token", headers.getFirst(HttpHeadersConstant.AUTHORIZATION),
            "Authorization 是客户端凭证，不属于身份头，不能误删");
        assertNull(headers.getFirst(HttpHeadersConstant.X_USER_ID));
    }

    @Test
    @DisplayName("无身份头时正常放行")
    void should_pass_through_without_identity_headers() {
        ServerWebExchange result = run(MockServerHttpRequest.get("/sys/i18n/messages"));

        assertEquals("/sys/i18n/messages", result.getRequest().getURI().getPath());
    }

}
