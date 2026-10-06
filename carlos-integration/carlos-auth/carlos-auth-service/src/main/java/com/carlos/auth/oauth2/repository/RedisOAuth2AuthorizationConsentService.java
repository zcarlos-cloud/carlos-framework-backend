package com.carlos.auth.oauth2.repository;

import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.redis.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 自定义 OAuth2 授权许可服务 - Redis 实现
 * </p>
 *
 * <p>管理用户对客户端的授权同意记录，核心功能包括：</p>
 * <ul>
 *   <li>持久化授权同意信息，避免用户重复授权</li>
 *   <li>支持授权流程优化，提升用户体验</li>
 *   <li>通过 Redis 实现分布式共享</li>
 * </ul>
 *
 * <p>Redis Key 设计：</p>
 * <pre>
 * auth:oauth2:consent:{clientId}:{principalName}
 * </pre>
 *
 * <p>适用场景：</p>
 * <ul>
 *   <li>授权码模式下记录用户的 Scope 授权同意</li>
 *   <li>多实例授权服务器共享授权记录</li>
 *   <li>避免服务器重启后用户需重复授权</li>
 * </ul>
 *
 * @author carlos
 * @date 2022/11/4 13:19
 * @since 3.0.0
 */
@Slf4j
public class RedisOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {


    /**
     * 默认 TTL（7天）
     * <p>授权同意信息保留7天，之后需要用户重新确认</p>
     */
    private static final Long TIMEOUT_DAYS = 7L;

    /**
     * 保存授权许可
     *
     * @param authorizationConsent 授权许可信息
     */
    @Override
    public void save(OAuth2AuthorizationConsent authorizationConsent) {
        Assert.notNull(authorizationConsent, "authorizationConsent cannot be null");
        log.debug("Saving OAuth2 authorization consent: clientId={}, principalName={}, scopes={}",
            authorizationConsent.getRegisteredClientId(),
            authorizationConsent.getPrincipalName(),
            authorizationConsent.getScopes());

        String key = buildKey(authorizationConsent);
        RedisUtil.setValue(key, authorizationConsent, TIMEOUT_DAYS, TimeUnit.DAYS);

        log.debug("Successfully saved OAuth2 authorization consent: key={}", key);
    }

    /**
     * 移除授权许可
     *
     * @param authorizationConsent 授权许可信息
     */
    @Override
    public void remove(OAuth2AuthorizationConsent authorizationConsent) {
        Assert.notNull(authorizationConsent, "authorizationConsent cannot be null");
        log.debug("Removing OAuth2 authorization consent: clientId={}, principalName={}",
            authorizationConsent.getRegisteredClientId(),
            authorizationConsent.getPrincipalName());

        String key = buildKey(authorizationConsent);
        RedisUtil.delete(key);

        log.debug("Successfully removed OAuth2 authorization consent: key={}", key);
    }

    /**
     * 根据客户端ID和主体名称查找授权许可
     *
     * @param registeredClientId 客户端ID
     * @param principalName      主体名称（用户名）
     * @return 授权许可信息，不存在返回 null
     */
    @Override
    @Nullable
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        Assert.hasText(registeredClientId, "registeredClientId cannot be empty");
        Assert.hasText(principalName, "principalName cannot be empty");
        log.debug("Finding OAuth2 authorization consent: clientId={}, principalName={}",
            registeredClientId, principalName);

        String key = buildKey(registeredClientId, principalName);
        OAuth2AuthorizationConsent consent = RedisUtil.getValue(key);

        if (consent != null) {
            log.debug("Found OAuth2 authorization consent: scopes={}", consent.getScopes());
        } else {
            log.debug("OAuth2 authorization consent not found: clientId={}, principalName={}",
                registeredClientId, principalName);
        }

        return consent;
    }

    /**
     * 构建 Redis Key
     *
     * @param registeredClientId 客户端ID
     * @param principalName      主体名称
     * @return Redis Key
     */
    private static String buildKey(String registeredClientId, String principalName) {
        return AuthRedisKey.oauth2Consent(registeredClientId, principalName);
    }

    /**
     * 构建 Redis Key
     *
     * @param authorizationConsent 授权许可信息
     * @return Redis Key
     */
    private static String buildKey(OAuth2AuthorizationConsent authorizationConsent) {
        return buildKey(authorizationConsent.getRegisteredClientId(), authorizationConsent.getPrincipalName());
    }
}
