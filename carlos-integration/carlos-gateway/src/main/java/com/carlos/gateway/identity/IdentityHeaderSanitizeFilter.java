package com.carlos.gateway.identity;

import com.carlos.core.constant.HttpHeadersConstant;
import com.carlos.gateway.config.GlobalFilterOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * <p>
 * 身份头净化过滤器
 * </p>
 *
 * <p><b>无条件剔除客户端自带的身份头</b>，确保下游见到的身份头只可能由网关自己写入。</p>
 *
 * <p>为什么必须有这一层：网关的 {@code OAuth2AuthenticationFilter} 在路径命中 oauth2 白名单时会
 * <b>整个短路</b>（既不认证、也不注入身份头），而 {@code ServerHttpRequest.Builder.header()} 只对
 * 它写入的头做覆盖 —— 于是客户端自带的伪造 {@code X-User-Id} 会原样穿透到下游。在下游启用
 * 「明文头 + 可信 IP」通道时，攻击者只要位于可信网段即可凭此冒充任意用户；改走签名断言后，
 * 客户端若自带 {@code X-User-Context}，也可能试图抢占这一通道。</p>
 *
 * <p>本过滤器不依赖任何开关：净化本身无副作用，任何情况下都应当执行。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-09-23
 */
@Slf4j
public class IdentityHeaderSanitizeFilter implements GlobalFilter, Ordered {

    /**
     * 需要剔除的身份头
     *
     * <p>只列「承载或曾经承载身份」的 6 个名字：</p>
     * <ul>
     *   <li>{@code X-User-Context} —— 唯一有效的身份载体。剥离它保证下游见到的断言只可能由
     *       网关自己签发；客户端自带的断言必然验签失败（既拿不到身份，反而会把自己的请求打成
     *       匿名），此处剥离同时留下可检测的伪造信号。</li>
     *   <li>{@code X-User-Id/Name/Roles}、{@code X-Tenant-Id}、{@code X-Dept-Id} —— 明文身份头
     *       时代的遗留。<b>当前已无任何读取方</b>，此处保留属纵深防御：它们是「曾经被下游信任过」
     *       的名字，一旦有人重新加回读取方，这里是唯一的兜底。</li>
     * </ul>
     *
     * <p>刻意<b>不</b>列入那些从未被任何代码读取过的幽灵头名（如历史上的 {@code X-Roles}）——
     * 剥离一个没人读的头只会让人误以为存在保护。写死的字面量同样有意为之：它们已不是「可配置的
     * 契约头」，而是安全名单，不该被别处的常量改动牵动。</p>
     */
    private static final List<String> IDENTITY_HEADERS = List.of(
        HttpHeadersConstant.X_USER_CONTEXT,
        HttpHeadersConstant.X_USER_ID,
        HttpHeadersConstant.X_USER_NAME,
        HttpHeadersConstant.X_USER_ROLES,
        HttpHeadersConstant.X_TENANT_ID,
        HttpHeadersConstant.X_DEPT_ID
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 客户端自带身份断言：浏览器不会发这个头，出现即视为伪造尝试，值得留痕
        if (request.getHeaders().getFirst(HttpHeadersConstant.X_USER_CONTEXT) != null) {
            log.warn("Client-supplied identity assertion detected and stripped, remoteAddr={}, path={}",
                request.getRemoteAddress(), request.getURI().getPath());
        }

        if (log.isDebugEnabled()) {
            List<String> present = IDENTITY_HEADERS.stream()
                .filter(name -> request.getHeaders().getFirst(name) != null)
                .toList();
            if (!present.isEmpty()) {
                log.debug("Stripping client-supplied identity headers: {}", present);
            }
        }

        ServerHttpRequest sanitized = request.mutate()
            .headers(headers -> IDENTITY_HEADERS.forEach(headers::remove))
            .build();

        return chain.filter(exchange.mutate().request(sanitized).build());
    }

    @Override
    public int getOrder() {
        return GlobalFilterOrder.IDENTITY_SANITIZE_ORDER;
    }

}
