package com.carlos.gateway.filter;

import com.carlos.core.util.PathMatchUtil;
import com.carlos.gateway.config.GatewayProperties;
import com.carlos.gateway.whitelist.UnifiedWhitelistProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * <p>
 * 网关统一前缀剥离 WebFilter
 * <br>
 * 在 Spring Cloud Gateway 路由匹配之前执行，提前剥离 {@code /manage-api} 等统一前缀，
 * 使路由 predicate 可以基于剥离后的路径编写（如 {@code /auth/**}），实现前缀一处定义。
 * </p>
 * <p>
 * 执行顺序说明：
 * <ul>
 *   <li>WebFilter 在 {@code DispatcherHandler} / {@code RoutePredicateHandlerMapping} 之前执行</li>
 *   <li>因此比所有 {@code GlobalFilter}（包括原来的 PathPrefixFilter、UnifiedWhitelistFilter）都早</li>
 *   <li>路由匹配时看到的路径已经是剥离前缀后的路径</li>
 * </ul>
 * </p>
 * <p>
 * 白名单路径（如 actuator、swagger 资源）跳过前缀剥离，保持原有访问方式不变。
 * </p>
 *
 * @author carlos
 * @date 2026/06/13
 * @see GatewayProperties
 * @see UnifiedWhitelistProperties
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class PrefixWebFilter implements WebFilter {

    private final GatewayProperties gatewayProperties;
    private final UnifiedWhitelistProperties whitelistProperties;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getRawPath();

        // 白名单路径跳过前缀剥离（保持原有访问方式）
        // if (isWhitelisted(path)) {
        //     log.debug("PrefixWebFilter: path [{}] is whitelisted, skip prefix strip", path);
        //     return chain.filter(exchange);
        // }

        String apiPrefix = gatewayProperties.getPrefix();
        if (!path.startsWith(apiPrefix)) {
            log.warn("PrefixWebFilter: request path [{}] does not start with required prefix [{}]", path, apiPrefix);
            return chain.filter(exchange);
        }

        // 剥离前缀
        String newPath = path.substring(apiPrefix.length());
        if (newPath.isEmpty()) {
            newPath = "/";
        } else if (!newPath.startsWith("/")) {
            newPath = "/" + newPath;
        }

        log.debug("PrefixWebFilter: {} -> {}", path, newPath);

        ServerHttpRequest newRequest = request.mutate()
            .path(newPath)
            .build();

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    /**
     * 检查路径是否在白名单中
     * <p>
     * 优先使用统一白名单中的 prefix-strip 路径，再回退到网关全局白名单。
     * </p>
     *
     * @param path 请求路径
     * @return true 如果路径在白名单中
     */
    private boolean isWhitelisted(String path) {
        // 1. 统一白名单中的 prefix-strip 路径（默认继承全局白名单，含 swagger、actuator 等）
        Set<String> prefixStripPaths = whitelistProperties.getPrefixStripPaths();
        if (prefixStripPaths != null && !prefixStripPaths.isEmpty()) {
            if (PathMatchUtil.antMatchAny(prefixStripPaths, path)) {
                return true;
            }
        }

        // 2. 网关全局白名单
        Set<String> whitelist = gatewayProperties.getWhitelist();
        if (whitelist != null && !whitelist.isEmpty()) {
            return PathMatchUtil.antMatchAny(whitelist, path);
        }

        return false;
    }
}
