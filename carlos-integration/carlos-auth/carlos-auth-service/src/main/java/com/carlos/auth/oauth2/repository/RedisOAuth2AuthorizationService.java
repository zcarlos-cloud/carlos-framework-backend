package com.carlos.auth.oauth2.repository;

import cn.hutool.json.JSONUtil;
import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.auth.oauth2.codec.OAuth2AuthorizationCodec;
import com.carlos.boot.web.request.IpUtils;
import com.carlos.redis.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 自定义 OAuth2 授权服务 - Redis 实现
 * </p>
 *
 * <p>基于 Redis 存储 OAuth2 授权信息，支持：</p>
 * <ul>
 *   <li>授权码（Authorization Code）存储</li>
 *   <li>访问令牌（Access Token）存储</li>
 *   <li>刷新令牌（Refresh Token）存储</li>
 *   <li>状态码（State）存储</li>
 *   <li>自动 TTL 过期清理</li>
 *   <li>多类型 Token 查找</li>
 * </ul>
 *
 * <p>Redis Key 设计：</p>
 * <ul>
 *   <li>{@code auth:oauth2:state:{state}} - 状态码</li>
 *   <li>{@code auth:oauth2:code:{code}} - 授权码</li>
 *   <li>{@code auth:oauth2:access_token:{token}} - 访问令牌</li>
 *   <li>{@code auth:oauth2:refresh_token:{token}} - 刷新令牌</li>
 *   <li>{@code auth:oauth2:id:{id}} - 授权 ID 索引</li>
 * </ul>
 *
 * @author carlos
 * @date 2022/11/4 14:35
 * @since 3.0.0
 */
@Slf4j
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    /**
     * Redis Key 前缀（统一收口于 {@link com.carlos.auth.constant.AuthRedisKey}）
     */
    private static final String AUTHORIZATION_PREFIX = AuthRedisKey.AUTHORIZATION_PREFIX;
    private static final String STATE_PREFIX = OAuth2ParameterNames.STATE;
    private static final String CODE_PREFIX = OAuth2ParameterNames.CODE;
    private static final String ACCESS_TOKEN_PREFIX = OAuth2ParameterNames.ACCESS_TOKEN;
    private static final String REFRESH_TOKEN_PREFIX = OAuth2ParameterNames.REFRESH_TOKEN;
    private static final String ID_PREFIX = "id";
    private static final String USER_SESSIONS_PREFIX = "user_sessions";

    /**
     * 默认超时时间（分钟）- 用于 state
     */
    private static final Long DEFAULT_TIMEOUT = 10L;

    /**
     * 默认最大有效期（天）- 兜底 TTL
     */
    private static final Duration DEFAULT_MAX_TTL = Duration.ofDays(7);

    /**
     * OAuth2 授权信息编解码器
     */
    private final OAuth2AuthorizationCodec codec;

    public RedisOAuth2AuthorizationService() {
        this.codec = new OAuth2AuthorizationCodec();
    }

    /**
     * 保存 OAuth2 授权信息
     *
     * @param authorization OAuth2 授权信息
     */
    @Override
    public void save(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");
        log.debug("Saving OAuth2 authorization: id={}, clientId={}, principalName={}",
            authorization.getId(), authorization.getRegisteredClientId(), authorization.getPrincipalName());

        String json = serialize(authorization);
        Duration ttl = calculateTtl(authorization);

        // 保存授权 ID 索引（便于后续查找和清理）
        saveAuthorizationIdIndex(authorization, json, ttl);

        // 保存用户会话索引（支持全局登出）
        saveUserSessionIndex(authorization, ttl);

        // 保存 state 索引
        if (isState(authorization)) {
            String state = authorization.getAttribute(OAuth2ParameterNames.STATE);
            RedisUtil.setValue(buildKey(STATE_PREFIX, state), json, DEFAULT_TIMEOUT, TimeUnit.MINUTES);
            log.debug("Saved state index: state={}", state);
        }

        // 保存授权码索引
        if (isCode(authorization)) {
            OAuth2AuthorizationCode authorizationCode = getAuthorizationCode(authorization);
            if (authorizationCode != null) {
                long codeTtl = calculateTokenTtl(authorizationCode.getIssuedAt(), authorizationCode.getExpiresAt());
                RedisUtil.setValue(buildKey(CODE_PREFIX, authorizationCode.getTokenValue()),
                    json, codeTtl, TimeUnit.SECONDS);
                log.debug("Saved authorization code index: code={}", maskToken(authorizationCode.getTokenValue()));
            }
        }

        // 保存访问令牌索引
        if (isAccessToken(authorization)) {
            OAuth2AccessToken accessToken = authorization.getAccessToken().getToken();
            long accessTokenTtl = calculateTokenTtl(accessToken.getIssuedAt(), accessToken.getExpiresAt());
            RedisUtil.setValue(buildKey(ACCESS_TOKEN_PREFIX, accessToken.getTokenValue()),
                json, accessTokenTtl, TimeUnit.SECONDS);
            log.debug("Saved access token index: token={}", maskToken(accessToken.getTokenValue()));

            // 保存会话元数据（在线用户页展示 IP/UA 用，仅首次签发时写入）
            saveSessionMeta(authorization, ttl);
        }

        // 保存刷新令牌索引
        if (isRefreshToken(authorization)) {
            OAuth2RefreshToken refreshToken = authorization.getRefreshToken().getToken();
            long refreshTokenTtl = calculateTokenTtl(refreshToken.getIssuedAt(), refreshToken.getExpiresAt());
            RedisUtil.setValue(buildKey(REFRESH_TOKEN_PREFIX, refreshToken.getTokenValue()),
                json, refreshTokenTtl, TimeUnit.SECONDS);
            log.debug("Saved refresh token index: token={}", maskToken(refreshToken.getTokenValue()));
        }

        log.debug("Successfully saved OAuth2 authorization: id={}", authorization.getId());
    }

    /**
     * 根据 ID 移除 OAuth2 授权信息
     *
     * @param authorization OAuth2 授权信息
     */
    @Override
    public void remove(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");
        log.debug("Removing OAuth2 authorization: id={}", authorization.getId());

        List<String> keys = new ArrayList<>();

        // 添加授权 ID 索引
        keys.add(buildKey(ID_PREFIX, authorization.getId()));

        // 添加 state 索引
        if (isState(authorization)) {
            String state = authorization.getAttribute(OAuth2ParameterNames.STATE);
            keys.add(buildKey(STATE_PREFIX, state));
        }

        // 添加授权码索引
        if (isCode(authorization)) {
            OAuth2AuthorizationCode authorizationCode = getAuthorizationCode(authorization);
            if (authorizationCode != null) {
                keys.add(buildKey(CODE_PREFIX, authorizationCode.getTokenValue()));
            }
        }

        // 添加刷新令牌索引
        if (isRefreshToken(authorization)) {
            OAuth2RefreshToken refreshToken = authorization.getRefreshToken().getToken();
            keys.add(buildKey(REFRESH_TOKEN_PREFIX, refreshToken.getTokenValue()));
        }

        // 添加访问令牌索引
        if (isAccessToken(authorization)) {
            OAuth2AccessToken accessToken = authorization.getAccessToken().getToken();
            keys.add(buildKey(ACCESS_TOKEN_PREFIX, accessToken.getTokenValue()));
            // 网关内省缓存同步删除：授权删除后若不清理，网关在缓存周期内仍判定 token 有效
            // （踢下线/登出/会话挤线/令牌撤销全部走 remove，收口在此处一并处理）
            keys.add(AuthRedisKey.tokenIntrospection(accessToken.getTokenValue()));
        }

        // 清理用户会话索引
        removeUserSessionIndex(authorization);

        // 清理会话元数据
        keys.add(AuthRedisKey.sessionMeta(authorization.getId()));

        // 批量删除
        if (!keys.isEmpty()) {
            RedisUtil.delete(keys);
            log.debug("Deleted {} keys for authorization: id={}", keys.size(), authorization.getId());
        }

        log.debug("Successfully removed OAuth2 authorization: id={}", authorization.getId());
    }

    /**
     * 根据 ID 查找 OAuth2 授权信息
     *
     * @param id 授权 ID
     * @return OAuth2 授权信息，不存在返回 null
     */
    @Override
    @Nullable
    public OAuth2Authorization findById(String id) {
        Assert.hasText(id, "id cannot be empty");
        log.debug("Finding OAuth2 authorization by id: {}", id);

        String json = RedisUtil.getValue(buildKey(ID_PREFIX, id));
        if (json == null) {
            log.debug("OAuth2 authorization not found by id: {}", id);
            return null;
        }

        return deserialize(json);
    }

    /**
     * 根据 Token 查找 OAuth2 授权信息
     *
     * @param token     Token 值
     * @param tokenType Token 类型
     * @return OAuth2 授权信息，不存在返回 null
     */
    @Override
    @Nullable
    public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
        Assert.hasText(token, "token cannot be empty");
        log.debug("Finding OAuth2 authorization by token: tokenType={}", tokenType);

        // 如果 tokenType 为 null，尝试所有类型
        if (tokenType == null) {
            return findByTokenWithoutType(token);
        }

        String key = buildKey(tokenType.getValue(), token);
        String json = RedisUtil.getValue(key);

        if (json == null) {
            log.debug("OAuth2 authorization not found by token: tokenType={}", tokenType);
            return null;
        }

        return deserialize(json);
    }

    /**
     * 不指定 Token 类型，尝试所有类型查找
     *
     * @param token Token 值
     * @return OAuth2 授权信息
     */
    private OAuth2Authorization findByTokenWithoutType(String token) {
        // 按优先级尝试：access_token -> refresh_token -> code -> state
        String[] types = {ACCESS_TOKEN_PREFIX, REFRESH_TOKEN_PREFIX, CODE_PREFIX, STATE_PREFIX};

        for (String type : types) {
            String key = buildKey(type, token);
            String json = RedisUtil.getValue(key);
            if (json != null) {
                log.debug("Found OAuth2 authorization by token with type: {}", type);
                return deserialize(json);
            }
        }

        log.debug("OAuth2 authorization not found by token for any type");
        return null;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 保存授权 ID 索引
     */
    private void saveAuthorizationIdIndex(OAuth2Authorization authorization, String json, Duration ttl) {
        RedisUtil.setValue(buildKey(ID_PREFIX, authorization.getId()),
            json, ttl.getSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 保存用户会话索引（用于全局登出时查找该用户的所有授权）
     *
     * <p>Redis Key: {@code auth:oauth2:user_sessions:{principalName}}</p>
     * <p>数据结构: Set&lt;authorizationId&gt;</p>
     */
    private void saveUserSessionIndex(OAuth2Authorization authorization, Duration ttl) {
        String principalName = authorization.getPrincipalName();
        if (principalName == null || principalName.isBlank()) {
            return;
        }
        String key = buildKey(USER_SESSIONS_PREFIX, principalName);
        RedisUtil.addSet(key, authorization.getId());
        // 使用固定的最大 TTL 避免短期 Token 缩短整个索引的过期时间
        // 确保即使某个会话很短，索引也能保留足够长时间供全局登出使用
        Duration sessionIndexTtl = ttl.compareTo(DEFAULT_MAX_TTL) > 0 ? ttl : DEFAULT_MAX_TTL;
        RedisUtil.setExpire(key, sessionIndexTtl);
        log.debug("Saved user session index: principalName={}, authId={}", principalName, authorization.getId());
    }

    /**
     * 保存会话元数据（S4 在线用户：IP/User-Agent）
     *
     * <p>Redis Key: {@code auth:oauth2:session_meta:{authorizationId}}</p>
     * <p>仅在首次签发且有 Web 请求上下文时写入内容；刷新令牌续期触发的 save 只顺延 TTL，
     * 避免长会话的元数据先于会话过期。辅助数据写入失败只告警，不阻断签发。</p>
     */
    private void saveSessionMeta(OAuth2Authorization authorization, Duration ttl) {
        try {
            String metaKey = AuthRedisKey.sessionMeta(authorization.getId());
            if (Boolean.TRUE.equals(RedisUtil.hasKey(metaKey))) {
                // 刷新续期：内容不变，TTL 跟随会话剩余有效期顺延
                RedisUtil.setExpire(metaKey, ttl);
                return;
            }
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            Map<String, String> meta = new HashMap<>(4);
            meta.put(AuthRedisKey.SESSION_META_FIELD_IP, IpUtils.getRequestIp(request));
            meta.put(AuthRedisKey.SESSION_META_FIELD_UA, request.getHeader("User-Agent"));
            RedisUtil.setValue(metaKey, JSONUtil.toJsonStr(meta), ttl);
            log.debug("Saved session meta: authId={}, ip={}", authorization.getId(), meta.get(AuthRedisKey.SESSION_META_FIELD_IP));
        } catch (Exception e) {
            // 会话元数据是在线用户页的展示辅助数据，写失败不影响签发
            log.warn("保存会话元数据失败: authId={}", authorization.getId(), e);
        }
    }

    /**
     * 移除用户会话索引
     */
    private void removeUserSessionIndex(OAuth2Authorization authorization) {
        String principalName = authorization.getPrincipalName();
        if (principalName == null || principalName.isBlank()) {
            return;
        }
        String key = buildKey(USER_SESSIONS_PREFIX, principalName);
        RedisUtil.removeSet(key, authorization.getId());
        log.debug("Removed user session index: principalName={}, authId={}", principalName, authorization.getId());
    }

    /**
     * 根据主体名称查找所有授权 ID（全局登出使用）
     *
     * @param principalName 主体名称（用户名）
     * @return 授权 ID 集合
     */
    public Set<String> findAuthorizationIdsByPrincipalName(String principalName) {
        if (principalName == null || principalName.isBlank()) {
            return Collections.emptySet();
        }
        String key = buildKey(USER_SESSIONS_PREFIX, principalName);
        Set<String> ids = RedisUtil.getSet(key);
        return ids != null ? ids : Collections.emptySet();
    }

    /**
     * 计算授权信息的 TTL
     * 优先使用刷新令牌有效期，其次是访问令牌，最后是默认值
     */
    private Duration calculateTtl(OAuth2Authorization authorization) {
        // 优先使用刷新令牌的有效期
        if (isRefreshToken(authorization)) {
            OAuth2RefreshToken refreshToken = authorization.getRefreshToken().getToken();
            if (refreshToken.getExpiresAt() != null) {
                return Duration.ofSeconds(
                    ChronoUnit.SECONDS.between(java.time.Instant.now(), refreshToken.getExpiresAt())
                );
            }
        }

        // 其次使用访问令牌的有效期
        if (isAccessToken(authorization)) {
            OAuth2AccessToken accessToken = authorization.getAccessToken().getToken();
            if (accessToken.getExpiresAt() != null) {
                return Duration.ofSeconds(
                    ChronoUnit.SECONDS.between(java.time.Instant.now(), accessToken.getExpiresAt())
                );
            }
        }

        // 最后使用授权码的有效期
        if (isCode(authorization)) {
            OAuth2AuthorizationCode authorizationCode = getAuthorizationCode(authorization);
            if (authorizationCode != null && authorizationCode.getExpiresAt() != null) {
                return Duration.ofSeconds(
                    ChronoUnit.SECONDS.between(java.time.Instant.now(), authorizationCode.getExpiresAt())
                );
            }
        }

        // 兜底使用默认最大有效期
        return DEFAULT_MAX_TTL;
    }

    /**
     * 计算 Token 的 TTL（秒）
     */
    private long calculateTokenTtl(java.time.Instant issuedAt, java.time.Instant expiresAt) {
        if (expiresAt == null) {
            return DEFAULT_MAX_TTL.getSeconds();
        }
        long seconds = ChronoUnit.SECONDS.between(java.time.Instant.now(), expiresAt);
        return Math.max(seconds, 1); // 至少 1 秒
    }

    /**
     * 构建 Redis Key
     */
    private String buildKey(String type, String id) {
        return AuthRedisKey.oauth2Key(type, id);
    }

    /**
     * 序列化 OAuth2Authorization
     */
    private String serialize(OAuth2Authorization authorization) {
        return codec.serialize(authorization);
    }

    /**
     * 反序列化 OAuth2Authorization
     */
    private OAuth2Authorization deserialize(String json) {
        return codec.deserialize(json);
    }

    /**
     * 获取授权码
     */
    private OAuth2AuthorizationCode getAuthorizationCode(OAuth2Authorization authorization) {
        OAuth2Authorization.Token<OAuth2AuthorizationCode> token = authorization
            .getToken(OAuth2AuthorizationCode.class);
        return token != null ? token.getToken() : null;
    }

    // ==================== 判断方法 ====================

    private static boolean isState(OAuth2Authorization authorization) {
        return Objects.nonNull(authorization.getAttribute(OAuth2ParameterNames.STATE));
    }

    private static boolean isCode(OAuth2Authorization authorization) {
        return Objects.nonNull(authorization.getToken(OAuth2AuthorizationCode.class));
    }

    private static boolean isRefreshToken(OAuth2Authorization authorization) {
        return Objects.nonNull(authorization.getRefreshToken());
    }

    private static boolean isAccessToken(OAuth2Authorization authorization) {
        return Objects.nonNull(authorization.getAccessToken());
    }

    /**
     * 脱敏 Token（用于日志）
     */
    private static String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 6) + "****" + token.substring(token.length() - 4);
    }
}
