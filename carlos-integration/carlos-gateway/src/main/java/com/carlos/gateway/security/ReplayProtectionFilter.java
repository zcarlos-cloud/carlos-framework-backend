package com.carlos.gateway.security;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.carlos.core.auth.SignatureUtil;
import com.carlos.core.constant.HttpHeadersConstant;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.gateway.config.GlobalFilterOrder;
import com.carlos.gateway.constant.GatewayHeaderConstants;
import com.carlos.gateway.exception.ErrorResponse;
import com.carlos.gateway.exception.ReplayAttackException;
import com.carlos.gateway.util.ClientIpUtil;
import com.carlos.gateway.whitelist.WhitelistContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * <p>
 * 防重放攻击过滤器 - 性能优化版
 * 基于请求签名和时间戳验证
 * </p>
 * <p>
 * 优化点：
 * 1. 优先使用统一白名单检查结果（UnifiedWhitelistFilter）
 * 2. 使用 HexFormat (JDK 17+) 替代手动十六进制转换
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 * @updated 2026/4/10 优化：统一白名单 + HexFormat
 * @updated 2026/6/6 移除 ThreadLocal Mac，每次创建新实例（Mac 非线程安全，WebFlux EventLoop 线程复用可能导致状态污染）
 */
@Slf4j
public class ReplayProtectionFilter implements GlobalFilter, Ordered {

    private final ReplayProtectionProperties properties;
    private final ReactiveStringRedisTemplate redisTemplate;

    // 时间戳允许的最大误差（秒）
    private static final long TIMESTAMP_TOLERANCE = 60; // 1分钟

    public ReplayProtectionFilter(ReplayProtectionProperties properties,
                                  ReactiveStringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String clientIp = getClientIp(request);

        // 1. 白名单快速检查（优先使用统一白名单检查结果）
        if (isWhitelisted(exchange, path)) {
            return chain.filter(exchange);
        }

        // 2. 获取必要参数
        String timestamp = request.getHeaders().getFirst(HttpHeadersConstant.X_TIMESTAMP);
        String nonce = request.getHeaders().getFirst(HttpHeadersConstant.X_NONCE);
        String signature = request.getHeaders().getFirst(HttpHeadersConstant.X_SIGNATURE);

        // 如果是 GET 请求且不需要签名验证，可以只检查时间戳
        if (request.getMethod() == HttpMethod.GET &&
            !properties.isStrictMode()) {
            if (timestamp == null) {
                return blockRequest(exchange, ReplayAttackException.AttackType.EXPIRED_TIMESTAMP,
                    nonce, "缺少时间戳请求头（" + HttpHeadersConstant.X_TIMESTAMP + "）");
            }
            return checkTimestamp(timestamp)
                .flatMap(valid -> {
                    if (Boolean.TRUE.equals(valid)) {
                        return chain.filter(exchange);
                    }
                    return blockRequest(exchange, ReplayAttackException.AttackType.EXPIRED_TIMESTAMP,
                        nonce, "请求时间戳过期或无效");
                });
        }

        // 严格模式：必须包含所有参数
        if (StrUtil.isBlank(timestamp) || StrUtil.isBlank(nonce) || StrUtil.isBlank(signature)) {
            return blockRequest(exchange, ReplayAttackException.AttackType.UNKNOWN, nonce,
                "缺少必要的安全请求头（" + HttpHeadersConstant.X_TIMESTAMP + ", " +
                    HttpHeadersConstant.X_NONCE + ", " + HttpHeadersConstant.X_SIGNATURE + "）");
        }

        // 3. 验证时间戳
        return checkTimestamp(timestamp)
            .flatMap(valid -> {
                if (!Boolean.TRUE.equals(valid)) {
                    if (log.isWarnEnabled()) {
                        log.warn("Replay protection: Expired timestamp from [{}] to [{}]", clientIp, path);
                    }
                    return blockRequest(exchange, ReplayAttackException.AttackType.EXPIRED_TIMESTAMP,
                        nonce, "请求时间戳过期");
                }
                return Mono.just(true);
            })
            // 4. 验证 Nonce（防止重放）
            .flatMap(v -> checkNonce(nonce))
            .flatMap(valid -> {
                if (!Boolean.TRUE.equals(valid)) {
                    if (log.isWarnEnabled()) {
                        log.warn("Replay protection: Duplicate nonce detected from [{}] to [{}]", clientIp, path);
                    }
                    return blockRequest(exchange, ReplayAttackException.AttackType.DUPLICATE_NONCE,
                        nonce, "检测到重放攻击（重复的请求标识）");
                }
                return Mono.just(true);
            })
            // 5. 验证签名
            .flatMap(v -> verifySignature(request, timestamp, nonce, signature))
            .flatMap(valid -> {
                if (!Boolean.TRUE.equals(valid)) {
                    if (log.isWarnEnabled()) {
                        log.warn("Replay protection: Invalid signature from [{}] to [{}]", clientIp, path);
                    }
                    return blockRequest(exchange, ReplayAttackException.AttackType.INVALID_SIGNATURE,
                        nonce, "请求签名验证失败");
                }
                return chain.filter(exchange);
            });
    }

    /**
     * 检查路径是否在白名单中（优化版）
     * <p>
     * 优先使用统一白名单检查结果，如果统一白名单未启用，则使用本地白名单
     *
     * @param exchange ServerWebExchange
     * @param path     请求路径
     * @return true 如果在白名单中
     */
    private boolean isWhitelisted(ServerWebExchange exchange, String path) {
        // 1. 优先使用统一白名单检查结果
        if (WhitelistContext.isReplayWhitelisted(exchange)) {
            return true;
        }

        // 2. 降级兼容：统一白名单未启用或结果不存在，使用本地白名单
        if (properties.getWhitelist() == null || properties.getWhitelist().isEmpty()) {
            return false;
        }
        // 使用快速前缀匹配
        for (String whitelistPath : properties.getWhitelist()) {
            if (path.startsWith(whitelistPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取客户端 IP（优化版）
     * <p>优先读取代理头（X-Forwarded-For 等）获取真实客户端 IP</p>
     */
    private String getClientIp(ServerHttpRequest request) {
        return ClientIpUtil.getClientIp(request);
    }

    /**
     * 检查时间戳
     */
    private Mono<Boolean> checkTimestamp(String timestamp) {
        return Mono.fromCallable(() -> {
            try {
                long ts = Long.parseLong(timestamp);
                long now = System.currentTimeMillis() / 1000;
                return Math.abs(now - ts) <= TIMESTAMP_TOLERANCE;
            } catch (NumberFormatException e) {
                return false;
            }
        });
    }

    /**
     * 检查 Nonce（确保唯一性）
     */
    private Mono<Boolean> checkNonce(String nonce) {
        String key = GatewayHeaderConstants.NONCE_CACHE_PREFIX + nonce;
        // setIfAbsent 是原子操作：原先 hasKey 再 set 的两步写法存在 TOCTOU ——
        // 并发下同一 nonce 会被两次都判定为「不存在」而双放行，防重放形同虚设
        return redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofSeconds(TIMESTAMP_TOLERANCE * 2))
            .map(Boolean.TRUE::equals)
            .defaultIfEmpty(false);
    }

    /**
     * 验证签名（优化版 - 使用 ThreadLocal Mac）
     */
    private Mono<Boolean> verifySignature(ServerHttpRequest request, String timestamp,
                                          String nonce, String signature) {
        return Mono.fromCallable(() -> {
            try {
                String method = request.getMethod().name();
                String path = request.getURI().getPath();
                String query = Optional.ofNullable(request.getURI().getQuery()).orElse("");

                // 构建签名字符串
                StringBuilder signStr = new StringBuilder(256);
                signStr.append(method).append("|")
                    .append(path).append("|")
                    .append(query).append("|")
                    .append(timestamp).append("|")
                    .append(nonce);

                // 必须有密钥才能进行签名验证
                if (StrUtil.isBlank(properties.getSecretKey())) {
                    log.warn("Replay protection secret key is not configured, rejecting request");
                    return false;
                }

                String computed = SignatureUtil.hmacSha256Hex(signStr.toString(), properties.getSecretKey());
                // 常量时间比较（避免计时侧信道逐字节爆破签名）；先归一化大小写，hex 大小写等价
                String actual = signature == null ? null : signature.toLowerCase(Locale.ROOT);
                return SignatureUtil.constantTimeEquals(computed, actual);
            } catch (Exception e) {
                log.error("Signature verification failed", e);
                return false;
            }
        });
    }

    /**
     * 拦截请求，返回统一格式的错误响应
     */
    private Mono<Void> blockRequest(ServerWebExchange exchange, ReplayAttackException.AttackType attackType,
                                    String requestId, String message) {
        String path = exchange.getRequest().getURI().getPath();
        String clientIp = getClientIp(exchange.getRequest());

        if (log.isWarnEnabled()) {
            log.warn("Replay protection blocked request from [{}] to [{}]: {} (type={})",
                clientIp, path, message, attackType.name());
        }

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 构建标准化的错误响应
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.FORBIDDEN.value())
            .code(CommonErrorCode.FORBIDDEN.getCode())
            .msg(message)
            .path(path)
            .method(exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "UNKNOWN")
            .extra(java.util.Map.of(
                "attackType", attackType.name(),
                "requestId", requestId != null ? requestId : "unknown",
                "clientIp", clientIp
            ))
            .build();

        return response.writeWith(Mono.fromSupplier(() -> {
            byte[] bytes = JSONUtil.toJsonStr(errorResponse).getBytes(StandardCharsets.UTF_8);
            return response.bufferFactory().wrap(bytes);
        }));
    }

    @Override
    public int getOrder() {
        // 在 WAF 之后执行，对通过防火墙的请求进行重放攻击验证
        return GlobalFilterOrder.REPLAY_PROTECTION_ORDER;
    }
}
