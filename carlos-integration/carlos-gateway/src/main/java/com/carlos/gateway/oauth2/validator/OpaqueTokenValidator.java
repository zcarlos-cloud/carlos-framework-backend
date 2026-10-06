package com.carlos.gateway.oauth2.validator;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.carlos.core.auth.UserContext;
import com.carlos.core.constant.RedisKeys;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.gateway.oauth2.OAuth2GatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Opaque Token 验证器（参考微信/钉钉设计）
 * 通过调用认证服务器 introspection 端点验证 Token
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 */
@Slf4j
public class OpaqueTokenValidator implements TokenValidator {

    private final OAuth2GatewayProperties properties;
    private final WebClient webClient;
    private final ReactiveStringRedisTemplate redisTemplate;

    public OpaqueTokenValidator(OAuth2GatewayProperties properties,
                                WebClient.Builder webClientBuilder,
                                ReactiveStringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.webClient = webClientBuilder.build();
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<UserContext> validate(String token) {
        if (StrUtil.isBlank(token)) {
            return Mono.error(new BusinessException(CommonErrorCode.UNAUTHORIZED, "Token is empty"));
        }

        // 使用 SHA-256 哈希值作为缓存键，避免明文 Token 泄露到 Redis 日志
        // 前缀与 carlos-auth 的 AuthRedisKey.TOKEN_INTROSPECTION 同源，踢下线时才能对上
        String cacheKey = RedisKeys.AUTH_TOKEN_INTROSPECTION + sha256Hex(token);
        return redisTemplate.opsForValue()
            .get(cacheKey)
            .flatMap(cached -> {
                log.debug("Token introspection cache hit");
                return Mono.just(parseCachedContext(cached));
            })
            .switchIfEmpty(Mono.defer(() -> introspectToken(token, cacheKey)));
    }

    /**
     * 计算字符串的 SHA-256 哈希值（十六进制）
     *
     * @param input 输入字符串
     * @return 十六进制哈希值
     */
    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 Java 标准库必支持的算法，理论上不会抛此异常
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private Mono<UserContext> introspectToken(String token, String cacheKey) {
        log.debug("Introspecting token at: {}", properties.getIntrospectionUri());

        String basicAuth = Base64.getEncoder().encodeToString(
            (properties.getClientId() + ":" + properties.getClientSecret())
                .getBytes(StandardCharsets.UTF_8));

        return webClient.post()
            .uri(properties.getIntrospectionUri())
            .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("token", token))
            .retrieve()
            .bodyToMono(String.class)
            .flatMap(response -> {
                JSONObject json = JSONUtil.parseObj(response);

                // 检查 Token 是否有效
                Boolean active = json.getBool("active");
                if (active == null || !active) {
                    return Mono.error(new BusinessException(CommonErrorCode.UNAUTHORIZED, "Token is not active"));
                }

                UserContext context = buildUserContext(json);

                // 缓存验证结果
                return cacheIntrospectionResult(cacheKey, json, context)
                    .thenReturn(context);
            })
            .onErrorMap(e -> {
                if (e instanceof BusinessException) {
                    return e;
                }
                log.error("Token introspection failed", e);
                return new BusinessException(CommonErrorCode.UNAUTHORIZED, "Token validation failed");
            });
    }

    private Mono<Void> cacheIntrospectionResult(String cacheKey, JSONObject json, UserContext context) {
        // 计算缓存时间（取 expires_in 和配置的最小值）
        Long expiresIn = json.getLong("exp");
        Duration cacheDuration = properties.getIntrospectionCacheDuration();

        if (expiresIn != null) {
            long ttl = expiresIn - System.currentTimeMillis() / 1000;
            if (ttl > 0 && ttl < cacheDuration.getSeconds()) {
                cacheDuration = Duration.ofSeconds(ttl);
            }
        }

        return redisTemplate.opsForValue()
            .set(cacheKey, JSONUtil.toJsonStr(context), cacheDuration)
            .then();
    }

    private UserContext buildUserContext(JSONObject json) {
        UserContext context = new UserContext();
        context.setToken(json.getStr("jti"));
        // user_id 为认证服务器写入的数字用户ID，sub 为主登录名，优先取 user_id
        context.setUserId(json.getStr("user_id", json.getStr("sub")));
        context.setAccount(json.getStr("username", json.getStr("sub")));
        context.setRoleId(json.getLong("role_id"));
        // tenant_id: 兼容数字与字符串两种形态
        Object tenantIdObj = json.get("tenant_id");
        if (tenantIdObj instanceof Number num) {
            context.setTenantId(num.longValue());
        } else if (tenantIdObj != null) {
            context.setTenantId(tenantIdObj.toString());
        }
        context.setDepartmentId(json.getLong("dept_id"));
        context.setPhone(json.getStr("phone"));

        // 解析角色列表（认证服务器写入 role_ids，可能是数字角色ID或角色编码；兼容旧字段 roles）
        Object roles = json.get("role_ids");
        if (roles == null) {
            roles = json.get("roles");
        }
        if (roles instanceof List<?> roleList) {
            Set<Serializable> roleIds = roleList.stream()
                .map(Object::toString)
                .map(OpaqueTokenValidator::parseSerializableId)
                .collect(Collectors.toSet());
            context.setRoleIds(roleIds);
        }

        // 解析部门列表
        Object depts = json.get("depts");
        if (depts instanceof List<?> deptList) {
            Set<Serializable> deptIds = deptList.stream()
                .map(Object::toString)
                .map(OpaqueTokenValidator::parseSerializableId)
                .collect(Collectors.toSet());
            context.setDepartmentIds(deptIds);
        }

        return context;
    }

    /**
     * 数字字符串转 Long，非数字（如角色编码 SUPER_ADMIN）保留原字符串
     */
    private static Serializable parseSerializableId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private UserContext parseCachedContext(String cached) {
        return JSONUtil.toBean(cached, UserContext.class);
    }

    @Override
    public OAuth2GatewayProperties.TokenType getType() {
        return OAuth2GatewayProperties.TokenType.OPAQUE;
    }
}
