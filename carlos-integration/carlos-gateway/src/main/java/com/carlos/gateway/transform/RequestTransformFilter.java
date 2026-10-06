package com.carlos.gateway.transform;

import com.carlos.core.constant.HttpHeadersConstant;
import com.carlos.gateway.config.GlobalFilterOrder;
import com.carlos.gateway.constant.GatewayHeaderConstants;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * 请求转换过滤器 - 性能优化版
 * 支持：API 版本控制、路径重写、Header 转换
 * </p>
 * <p>
 * 优化点：
 * 1. 预编译 Pattern 缓存，避免重复编译正则表达式
 * 2. 合并多次 request.mutate() 调用，减少对象创建
 * 3. 使用 IdUtils.fastSimpleUUID() 替代 UUID.randomUUID()（更快、非阻塞）
 * 4. 延迟日志参数计算
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 * @updated 2026/4/10 性能优化：Pattern 缓存 + 合并 mutate
 */
@Slf4j
public class RequestTransformFilter implements GlobalFilter, Ordered {

    private final TransformProperties properties;

    // API 版本模式匹配（预编译）
    private static final Pattern VERSION_PATTERN = Pattern.compile("/v(\\d+)/");

    // 路径重写规则缓存（预编译 Pattern）
    private List<PathRewriteRule> pathRewriteRules;

    public RequestTransformFilter(TransformProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        // 预编译路径重写规则
        if (properties.getPathRewrites() != null && !properties.getPathRewrites().isEmpty()) {
            pathRewriteRules = new ArrayList<>(properties.getPathRewrites().size());
            for (Map.Entry<String, String> entry : properties.getPathRewrites().entrySet()) {
                try {
                    Pattern pattern = Pattern.compile(entry.getKey());
                    pathRewriteRules.add(new PathRewriteRule(pattern, entry.getValue()));
                } catch (Exception e) {
                    log.error("Invalid path rewrite pattern: {}", entry.getKey(), e);
                }
            }
            if (log.isInfoEnabled()) {
                log.info("Compiled {} path rewrite rules", pathRewriteRules.size());
            }
        } else {
            pathRewriteRules = List.of();
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();

        // 使用 Builder 模式合并所有修改
        ServerHttpRequest.Builder builder = request.mutate();
        boolean modified = false;

        // 1. API 版本控制
        if (properties.isApiVersionEnabled()) {
            modified = handleApiVersion(request, builder) || modified;
        }

        // 2. 路径重写
        if (!pathRewriteRules.isEmpty()) {
            modified = rewritePath(request, builder) || modified;
        }

        // 3. Header 转换
        if (properties.getHeaderTransforms() != null && !properties.getHeaderTransforms().isEmpty()) {
            modified = transformHeaders(request, builder) || modified;
        }

        // 4. 添加标准 Header（始终执行）
        addStandardHeaders(request, builder);
        modified = true;

        // 如果请求被修改，创建新的 exchange
        if (modified) {
            exchange = exchange.mutate().request(builder.build()).build();
        }

        return chain.filter(exchange);
    }

    /**
     * 处理 API 版本（优化版）
     */
    private boolean handleApiVersion(ServerHttpRequest request, ServerHttpRequest.Builder builder) {
        String path = request.getURI().getPath();

        // 从 Header 中获取版本
        String versionHeader = request.getHeaders().getFirst(HttpHeadersConstant.X_API_VERSION);
        if (versionHeader != null) {
            builder.header(HttpHeadersConstant.X_ROUTED_VERSION, versionHeader);
            return true;
        }

        // 从 URL 路径中提取版本（复用 Matcher）
        Matcher matcher = VERSION_PATTERN.matcher(path);
        if (matcher.find()) {
            String version = matcher.group(1);
            builder.header(HttpHeadersConstant.X_ROUTED_VERSION, version);
            return true;
        }

        return false;
    }

    /**
     * 重写路径（优化版 - 使用预编译 Pattern）
     */
    private boolean rewritePath(ServerHttpRequest request, ServerHttpRequest.Builder builder) {
        String path = request.getURI().getPath();
        String originalPath = path;

        for (PathRewriteRule rule : pathRewriteRules) {
            Matcher matcher = rule.pattern.matcher(path);
            if (matcher.find()) {
                String newPath = matcher.replaceAll(rule.replacement);
                if (!newPath.equals(originalPath)) {
                    log.debug("Path rewritten: {} -> {}", originalPath, newPath);
                    builder.path(newPath);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 转换 Header（优化版）
     */
    private boolean transformHeaders(ServerHttpRequest request, ServerHttpRequest.Builder builder) {
        boolean modified = false;

        for (TransformProperties.HeaderTransform transform : properties.getHeaderTransforms()) {
            String value = request.getHeaders().getFirst(transform.getSource());
            if (value != null) {
                switch (transform.getOperation()) {
                    case ADD:
                        builder.header(transform.getTarget(), value);
                        modified = true;
                        break;
                    case REPLACE:
                        String newValue = transformValue(value, transform);
                        builder.header(transform.getTarget(), newValue);
                        if (!transform.getSource().equals(transform.getTarget())) {
                            // 移除原 Header
                            builder.headers(headers -> headers.remove(transform.getSource()));
                        }
                        modified = true;
                        break;
                    case REMOVE:
                        builder.headers(headers -> headers.remove(transform.getSource()));
                        modified = true;
                        break;
                }
            }
        }

        return modified;
    }

    /**
     * 转换值
     */
    private String transformValue(String value, TransformProperties.HeaderTransform transform) {
        String result = value;
        if (transform.getPrefix() != null) {
            result = transform.getPrefix() + result;
        }
        if (transform.getSuffix() != null) {
            result = result + transform.getSuffix();
        }
        if (transform.getMapping() != null && transform.getMapping().containsKey(result)) {
            result = transform.getMapping().get(result);
        }
        return result;
    }

    /**
     * 添加标准 Header（优化版）
     */
    private void addStandardHeaders(ServerHttpRequest request, ServerHttpRequest.Builder builder) {
        // 添加网关标识
        builder.header(GatewayHeaderConstants.X_GATEWAY_ID, properties.getGatewayId());

        // 添加转发时间戳
        builder.header(GatewayHeaderConstants.X_GATEWAY_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
    }

    /**
     * 路径重写规则（内部类）
     */
    private static class PathRewriteRule {
        final Pattern pattern;
        final String replacement;

        PathRewriteRule(Pattern pattern, String replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }
    }

    @Override
    public int getOrder() {
        // 在路径前缀处理之后执行，完成路径重写和 Header 转换
        return GlobalFilterOrder.REQUEST_TRANSFORM_ORDER;
    }
}
