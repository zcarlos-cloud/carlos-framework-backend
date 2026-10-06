package com.carlos.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * SelectStripPrefixGatewayFilterFactory 单元测试
 *
 * <p>覆盖两类路径：Swagger/Knife4j 文档路径（原有能力，此处作回归保护）与枚举字典路径
 * {@code /{service}/app/enum(s)}（新增）。</p>
 *
 * <p>最关键的是回归断言 {@code /app/enum} 本身**不被剥**：它是 carlos-system 经
 * {@code Path=/app/**} 直接暴露的通路，一旦被剥成 {@code /enum} 就会打成 404。
 * 这条断言钉住的是「不能用双星通配、也不能把 {@code /enum} 放进精确白名单」这个约束。</p>
 */
class SelectStripPrefixGatewayFilterFactoryTest {

    private SelectStripPrefixGatewayFilterFactory factory;

    @BeforeEach
    void setUp() {
        factory = new SelectStripPrefixGatewayFilterFactory();
        // matchCache 是 @PostConstruct 建的，单测不会触发容器回调，必须手动调
        factory.init();
    }

    /**
     * 跑一遍过滤器，返回下游实际收到的路径
     */
    private String run(String path) {
        GatewayFilter filter = factory.apply(new SelectStripPrefixGatewayFilterFactory.Config());
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            captured.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerWebExchange result = captured.get();
        assertNotNull(result, "过滤器必须放行到下游");
        return result.getRequest().getURI().getRawPath();
    }

    @Test
    @DisplayName("枚举路径应剥掉服务前缀：/{service}/app/enum(s) -> /app/enum(s)")
    void should_strip_service_prefix_for_enum_path() {
        assertEquals("/app/enum", run("/sys/app/enum"));
        assertEquals("/app/enum", run("/org/app/enum"));
        assertEquals("/app/enum", run("/auth/app/enum"));
        assertEquals("/app/enum", run("/message/app/enum"));
        assertEquals("/app/enum", run("/audit/app/enum"));
        assertEquals("/app/enum", run("/job/app/enum"));
        assertEquals("/app/enum", run("/workflow/app/enum"));

        assertEquals("/app/enums", run("/org/app/enums"));
        assertEquals("/app/enums", run("/audit/app/enums"));
    }

    @Test
    @DisplayName("回归：/app/enum 本身不能被剥（carlos-system 的 Path=/app/** 通路）")
    void should_not_strip_bare_app_enum_path() {
        assertEquals("/app/enum", run("/app/enum"));
        assertEquals("/app/enums", run("/app/enums"));
    }

    @Test
    @DisplayName("回归：普通业务路径不能被剥")
    void should_not_strip_business_paths() {
        assertEquals("/org/user/page", run("/org/user/page"));
        assertEquals("/sys/config/detail", run("/sys/config/detail"));
        assertEquals("/audit/log/page", run("/audit/log/page"));
        assertEquals("/message/record/page", run("/message/record/page"));
        // 形似但不是枚举端点：中间多了一层，不应被剥
        assertEquals("/sys/app/enum/detail", run("/sys/app/enum/detail"));
    }

    @Test
    @DisplayName("回归：Swagger/Knife4j 文档路径原有行为不变")
    void should_keep_stripping_doc_paths() {
        assertEquals("/v3/api-docs", run("/sys/v3/api-docs"));
        assertEquals("/doc.html", run("/sys/doc.html"));
        assertEquals("/swagger-ui/index.html", run("/sys/swagger-ui/index.html"));
        assertEquals("/webjars/springfox.js", run("/org/webjars/springfox.js"));
    }
}
