package com.carlos.gateway.gray;

import com.carlos.core.constant.HttpHeadersConstant;
import com.carlos.gateway.config.GlobalFilterOrder;
import com.carlos.gateway.constant.GatewayHeaderConstants;
import com.carlos.gateway.util.MurmurHash3Util;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <p>
 * 灰度发布过滤器 - 性能优化版
 * 支持基于用户、IP、权重的灰度策略
 * </p>
 * <p>
 * 优化点：
 * 1. 使用 MurmurHash3 替代 MD5（哈希计算快 5-10 倍）
 * 2. 使用 ThreadLocalRandom 替代 Random（无锁、线程安全）
 * 3. Caffeine 缓存策略结果（带过期时间）
 * 4. 减少实例选择时的随机数创建
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 * @updated 2026/4/10 性能优化：MurmurHash + ThreadLocalRandom + Caffeine
 */
@Slf4j
public class GrayReleaseFilter implements GlobalFilter, Ordered {

    private final GrayReleaseProperties properties;
    private final ReactiveDiscoveryClient discoveryClient;

    // 灰度策略缓存（使用 Caffeine 替代 ConcurrentHashMap，支持过期）
    private final LoadingCache<String, GrayStrategy> strategyCache;

    // 随机数生成器（线程本地，无锁）
    private static final SplittableRandom RANDOM = new SplittableRandom();

    public GrayReleaseFilter(GrayReleaseProperties properties,
                             ReactiveDiscoveryClient discoveryClient) {
        this.properties = properties;
        this.discoveryClient = discoveryClient;

        // 初始化策略缓存（1小时过期，最大1000条）
        this.strategyCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .build(this::loadStrategy);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        String serviceName = exchange.getAttribute(
            ServerWebExchangeUtils.GATEWAY_PREDICATE_ROUTE_ATTR);
        if (serviceName == null) {
            return chain.filter(exchange);
        }

        // 从缓存获取策略
        GrayStrategy strategy = strategyCache.get(serviceName);
        if (strategy == null || !strategy.isEnabled()) {
            return chain.filter(exchange);
        }

        // 判断是否是灰度用户
        boolean isGrayUser = isGrayUser(exchange, strategy);

        // 设置灰度标记
        exchange.getAttributes().put(GatewayHeaderConstants.GRAY_RELEASE_ATTR, isGrayUser);

        if (isGrayUser) {
            log.debug("Gray release enabled for service: {}, request: {}",
                serviceName, exchange.getRequest().getURI());

            // 添加灰度标记头
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(HttpHeadersConstant.X_GRAY_RELEASE, "true")
                .build();
            ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

            // 选择灰度实例
            return selectGrayInstance(serviceName, strategy)
                .flatMap(instance -> {
                    if (instance != null) {
                        URI uri = URI.create("http://" + instance.getHost() + ":" + instance.getPort());
                        mutatedExchange.getAttributes().put(
                            ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, uri);
                    }
                    return chain.filter(mutatedExchange);
                })
                .switchIfEmpty(chain.filter(mutatedExchange));
        }

        return chain.filter(exchange);
    }

    /**
     * 从配置加载策略
     */
    private GrayStrategy loadStrategy(String serviceName) {
        return properties.getStrategies().get(serviceName);
    }

    /**
     * 判断是否是灰度用户（优化版）
     */
    private boolean isGrayUser(ServerWebExchange exchange, GrayStrategy strategy) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. 基于权重的灰度
        if (strategy.getWeight() > 0) {
            int hash = hashRequestOptimized(request, strategy);
            return (hash % 100) < strategy.getWeight();
        }

        // 2. 基于用户的灰度
        String userId = request.getHeaders().getFirst(HttpHeadersConstant.X_USER_ID);
        if (userId != null && strategy.getUserIds() != null) {
            return strategy.getUserIds().contains(userId);
        }

        // 3. 基于 IP 的灰度
        String ip = request.getRemoteAddress() != null
            ? request.getRemoteAddress().getAddress().getHostAddress()
            : null;
        if (ip != null && strategy.getIpRanges() != null) {
            return strategy.getIpRanges().stream().anyMatch(ip::startsWith);
        }

        // 4. 基于请求头的灰度
        if (strategy.getHeaders() != null) {
            for (Map.Entry<String, String> entry : strategy.getHeaders().entrySet()) {
                String headerValue = request.getHeaders().getFirst(entry.getKey());
                if (entry.getValue().equals(headerValue)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 计算请求的哈希值（优化版 - 使用 MurmurHash3）
     */
    private int hashRequestOptimized(ServerHttpRequest request, GrayStrategy strategy) {
        String key = switch (strategy.getHashKey()) {
            case USER -> request.getHeaders().getFirst(HttpHeadersConstant.X_USER_ID);
            case IP -> request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "0.0.0.0";
            case HEADER -> request.getHeaders().getFirst(strategy.getHeaderName());
            default -> generateRandomKey();
        };

        if (key == null) {
            // 使用 ThreadLocalRandom 生成随机哈希（避免 UUID 同步开销）
            return ThreadLocalRandom.current().nextInt();
        }

        // 使用 MurmurHash3（比 MD5 快 5-10 倍）
        return MurmurHash3Util.hash32(key);
    }

    /**
     * 选择灰度实例（优化版 - 使用 ThreadLocalRandom）
     */
    private Mono<ServiceInstance> selectGrayInstance(String serviceName, GrayStrategy strategy) {
        return discoveryClient.getInstances(serviceName)
            .collectList()
            .map(instances -> {
                if (instances.isEmpty()) {
                    return null;
                }

                // 优先选择带有灰度标签的实例
                List<ServiceInstance> grayInstances = instances.stream()
                    .filter(instance -> {
                        Map<String, String> metadata = instance.getMetadata();
                        return "true".equals(metadata.get(GatewayHeaderConstants.GRAY_METADATA_KEY)) ||
                            "true".equals(metadata.get(GatewayHeaderConstants.GRAY_METADATA_SHORT));
                    })
                    .toList();

                if (!grayInstances.isEmpty()) {
                    // 使用 ThreadLocalRandom（无锁、线程安全）
                    return grayInstances.get(ThreadLocalRandom.current().nextInt(grayInstances.size()));
                }

                // 如果没有灰度实例，使用版本匹配的实例
                if (strategy.getVersion() != null) {
                    List<ServiceInstance> versionInstances = instances.stream()
                        .filter(instance -> strategy.getVersion()
                            .equals(instance.getMetadata().get("version")))
                        .toList();
                    if (!versionInstances.isEmpty()) {
                        return versionInstances.get(ThreadLocalRandom.current().nextInt(versionInstances.size()));
                    }
                }

                return null;
            });
    }

    /**
     * 获取缓存统计信息（用于监控）
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
            "strategyCacheStats", strategyCache.stats().toString(),
            "strategyCacheSize", strategyCache.estimatedSize()
        );
    }

    /**
     * 生成随机键（用于默认情况的哈希）
     */
    private String generateRandomKey() {
        return java.util.UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        // 在路由决策之前执行，决定请求路由到灰度还是正式实例
        return GlobalFilterOrder.GRAY_RELEASE_ORDER;
    }
}
