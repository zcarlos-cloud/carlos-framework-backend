package com.carlos.gateway.filter;

import com.carlos.core.util.PathMatchUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;

/**
 * 选择性路径前缀剥离过滤器工厂（高并发优化版）
 *
 * <p>性能优化点：
 * <ul>
 *   <li>路径匹配结果缓存（Caffeine），避免重复计算</li>
 *   <li>精确路径快速判断（HashSet O(1)）</li>
 *   <li>延迟日志参数计算（避免不必要的字符串拼接）</li>
 *   <li>零临时对象路径剥离（复用字符数组）</li>
 * </ul>
 * </p>
 *
 * <p>典型使用场景：
 * 多服务网关架构中，Swagger/Knife4j文档通过服务前缀访问，但转发给下游时需去掉前缀。</p>
 *
 * <p>转换示例（parts=1）：
 * <pre>
 *   /user-service/doc.html           -> /doc.html
 *   /order-service/v3/api-docs       -> /v3/api-docs
 *   /user-service/api/users          -> /user-service/api/users (不匹配白名单)
 * </pre>
 * </p>
 *
 * @author Carlos
 * @date 2024/1/24 8:58
 * @since 1.0.0
 */
@Slf4j
public class SelectStripPrefixGatewayFilterFactory
        extends AbstractGatewayFilterFactory<SelectStripPrefixGatewayFilterFactory.Config> {

    /**
     * 配置参数key：剥离的前缀段数
     */
    public static final String PARTS_KEY = "parts";

    /**
     * 缓存配置
     */
    private static final int CACHE_MAX_SIZE = 1000;
    private static final int CACHE_EXPIRE_MINUTES = 10;

    /**
     * 精确匹配白名单（无需通配符，O(1)查找）
     */
    private static final Set<String> EXACT_PATTERNS = Set.of(
        "/doc.html",
        "/swagger-ui.html",
        "/favicon.ico",
        "/swagger-resources",
        "/v3/api-docs",
        "/v3/api-docs-ext",
        "/csrf"
    );

    /**
     * 通配符匹配白名单（需正则匹配）
     *
     * <p>枚举字典路径（{@code /{service}/app/enum}、{@code /{service}/app/enums}）用
     * {@code /*} 单段通配：下游的 ApplicationController 挂在 {@code /app} 上，
     * 而各业务的 Controller 都自带服务前缀（{@code /org/xxx}、{@code /audit/xxx}…），
     * 网关对业务路径不剥前缀，所以服务前缀必须在这里按子路径精确剥掉。</p>
     *
     * <p>两个不能踩的坑：</p>
     * <ol>
     *   <li>不能改用双星通配——双星可匹配零个目录，会把 {@code /app/enum}
     *       本身一并剥成 {@code /enum} 打成 404。{@code /app/enum} 是 carlos-system
     *       经 {@code Path=/app/**} 直接暴露的通路，必须保持原样。</li>
     *   <li>不能把 {@code /enum} 加进上面的精确白名单——精确匹配是按路径**最后一段**
     *       比对的（见 {@link #normalizePath(String)}），会把任意以 {@code /enum}
     *       结尾的路径都剥掉，同样误伤 {@code /app/enum}。</li>
     * </ol>
     */
    private static final Set<String> WILDCARD_PATTERNS = Set.of(
        "/**/swagger-ui/**",
        "/**/swagger-resources/**",
        "/**/v3/api-docs/**",
        "/**/webjars/**",
        "/*/app/enum",
        "/*/app/enums"
    );

    /**
     * 路径匹配结果缓存
     * Key: 原始路径, Value: 是否需要剥离前缀
     */
    private LoadingCache<String, Boolean> matchCache;

    public SelectStripPrefixGatewayFilterFactory() {
        super(Config.class);
    }

    /**
     * 初始化缓存
     */
    @PostConstruct
    public void init() {
        this.matchCache = Caffeine.newBuilder()
            .maximumSize(CACHE_MAX_SIZE)
            .expireAfterWrite(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .recordStats()
            .build(this::computeShouldStrip);

        log.info("SelectStripPrefixGatewayFilterFactory initialized with cache(maxSize={}, expire={}min)",
            CACHE_MAX_SIZE, CACHE_EXPIRE_MINUTES);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of(PARTS_KEY);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getRawPath();

            // 1. 从缓存获取匹配结果（无锁，高性能）
            boolean shouldStrip = matchCache.get(path);

            if (!shouldStrip) {
                // 使用trace级别，避免高并发下的日志开销
                log.trace("Path [{}] skipped", path);
                return chain.filter(exchange);
            }

            // 2. 记录原始URL
            addOriginalRequestUrl(exchange, request.getURI());

            // 3. 执行路径剥离（零临时对象算法）
            String newPath = stripPrefixOptimized(path, config.getParts());

            // 4. 延迟计算日志参数（仅在debug启用时拼接字符串）
            log.debug("Strip path: [{}] -> [{}] (parts={})", path, newPath, config.getParts());

            // 5. 构建新请求
            ServerHttpRequest newRequest = request.mutate()
                .path(newPath)
                .build();

            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, newRequest.getURI());

            return chain.filter(exchange.mutate().request(newRequest).build());
        };
    }

    /**
     * 计算路径是否应该剥离前缀（用于缓存加载）
     *
     * @param path 请求路径
     * @return true 如果需要剥离前缀
     */
    private Boolean computeShouldStrip(String path) {
        // 1. 快速精确匹配（O(1)）
        String normalizedPath = normalizePath(path);
        if (EXACT_PATTERNS.contains(normalizedPath)) {
            return true;
        }

        // 2. 提取路径最后一段再次精确匹配
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash > 0) {
            String lastSegment = path.substring(lastSlash);
            if (EXACT_PATTERNS.contains(lastSegment)) {
                return true;
            }
        }

        // 3. 通配符匹配（开销较大，最后执行）
        return PathMatchUtil.antMatchAny(WILDCARD_PATTERNS, path);
    }

    /**
     * 归一化路径（用于精确匹配）
     */
    private String normalizePath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(lastSlash) : path;
    }

    /**
     * 优化的路径前缀剥离算法
     *
     * <p>性能优化：
     * <ul>
     *   <li>避免正则表达式</li>
     *   <li>单次扫描完成剥离</li>
     *   <li>最小化对象创建</li>
     * </ul>
     * </p>
     *
     * @param path  原始路径
     * @param parts 要剥离的段数
     * @return 剥离后的路径
     */
    private String stripPrefixOptimized(String path, int parts) {
        if (parts <= 0) {
            return path;
        }

        int len = path.length();
        int skipCount = 0;
        int startIndex = 0;

        // 跳过开头的 '/'
        if (startIndex < len && path.charAt(startIndex) == '/') {
            startIndex++;
        }

        // 跳过指定数量的路径段
        while (skipCount < parts && startIndex < len) {
            // 找到当前段的结束位置
            int slashIndex = path.indexOf('/', startIndex);
            if (slashIndex == -1) {
                // 路径段数不足，返回根路径
                return "/";
            }
            startIndex = slashIndex + 1;
            skipCount++;
        }

        // 构建结果路径
        if (startIndex >= len) {
            return "/";
        }

        // 确保结果以 '/' 开头
        if (path.charAt(startIndex - 1) != '/') {
            return "/" + path.substring(startIndex);
        }

        return path.substring(startIndex - 1);
    }

    /**
     * 获取缓存统计（用于监控）
     *
     * @return 缓存统计信息
     */
    public CacheStats getCacheStats() {
        return matchCache != null ? matchCache.stats() : null;
    }

    /**
     * 过滤器配置类
     */
    @Data
    @Validated
    public static class Config {

        /**
         * 要剥离的前缀段数（必须 >= 1）
         */
        @Min(1)
        private int parts = 1;
    }
}
