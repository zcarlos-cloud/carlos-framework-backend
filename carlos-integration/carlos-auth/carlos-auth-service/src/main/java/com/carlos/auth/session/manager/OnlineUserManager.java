package com.carlos.auth.session.manager;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.auth.oauth2.repository.RedisOAuth2AuthorizationService;
import com.carlos.auth.session.pojo.dto.OnlineSessionDTO;
import com.carlos.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 在线用户会话 数据封装层
 * </p>
 *
 * <p>数据源为 Redis 授权存储：{@code auth:oauth2:user_sessions:{username}} 用户会话索引（Set&lt;authId&gt;）
 * + {@code auth:oauth2:id:{authId}} 授权 JSON + {@code auth:oauth2:session_meta:{authId}} 会话元数据（IP/UA）。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlineUserManager {

    private final RedisOAuth2AuthorizationService authorizationService;

    /**
     * 枚举在线会话（索引中授权已过期/被删的条目会被跳过）
     *
     * @param usernameKeyword 用户名关键字（模糊），非空时用 Redis glob 先缩小索引扫描范围
     * @return 在线会话列表
     */
    public List<OnlineSessionDTO> listOnlineSessions(String usernameKeyword) {
        Set<String> indexKeys = RedisUtil.scanKeys(AuthRedisKey.userSessionPattern(usernameKeyword), 200);
        List<OnlineSessionDTO> sessions = new ArrayList<>();
        for (String indexKey : indexKeys) {
            String username = AuthRedisKey.extractUsername(indexKey);
            Set<Object> authIds = RedisUtil.getSet(indexKey);
            if (authIds == null || authIds.isEmpty()) {
                continue;
            }
            for (Object authId : authIds) {
                OAuth2Authorization authorization = authorizationService.findById(String.valueOf(authId));
                OnlineSessionDTO session = toSession(authorization, username);
                if (session != null) {
                    sessions.add(session);
                }
            }
        }
        return sessions;
    }

    /**
     * 查询某用户的全部会话ID（踢下线用）
     *
     * @param username 用户名
     * @return 会话ID集合
     */
    public Set<Object> listSessionIds(String username) {
        return RedisUtil.getSet(AuthRedisKey.userSessions(username));
    }

    /**
     * 授权对象转在线会话（授权不存在或访问令牌缺失时返回 null）
     */
    private OnlineSessionDTO toSession(OAuth2Authorization authorization, String username) {
        if (authorization == null) {
            // 索引脏数据：授权已过期但会话索引未清理
            return null;
        }
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken == null || accessToken.getToken() == null) {
            return null;
        }
        Map<String, Object> claims = accessToken.getClaims();

        Instant loginInstant = accessToken.getToken().getIssuedAt();
        Instant expireInstant = null;
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        if (refreshToken != null && refreshToken.getToken() != null) {
            expireInstant = refreshToken.getToken().getExpiresAt();
        }
        if (expireInstant == null) {
            expireInstant = accessToken.getToken().getExpiresAt();
        }

        OnlineSessionDTO session = new OnlineSessionDTO()
            .setSessionId(authorization.getId())
            .setUsername(username)
            .setUserId(extractLongClaim(claims, "user_id"))
            .setClientId(authorization.getRegisteredClientId())
            .setGrantType(authorization.getAuthorizationGrantType().getValue())
            .setLoginTime(toLocalDateTime(loginInstant))
            .setExpireTime(toLocalDateTime(expireInstant));
        fillSessionMeta(session);
        return session;
    }

    /**
     * 填充会话元数据（IP/UA，签发时写入，存量会话可能不存在）
     */
    private void fillSessionMeta(OnlineSessionDTO session) {
        Object metaJson = RedisUtil.getValue(AuthRedisKey.sessionMeta(session.getSessionId()));
        if (metaJson == null) {
            return;
        }
        try {
            JSONObject meta = JSONUtil.parseObj(String.valueOf(metaJson));
            session.setIp(meta.getStr(AuthRedisKey.SESSION_META_FIELD_IP));
            session.setUserAgent(meta.getStr(AuthRedisKey.SESSION_META_FIELD_UA));
        } catch (Exception e) {
            log.warn("会话元数据解析失败: sessionId={}", session.getSessionId());
        }
    }

    private Long extractLongClaim(Map<String, Object> claims, String key) {
        if (claims == null || claims.get(key) == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(claims.get(key)));
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

}
