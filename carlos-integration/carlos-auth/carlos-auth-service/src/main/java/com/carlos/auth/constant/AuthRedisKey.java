package com.carlos.auth.constant;

import cn.hutool.crypto.digest.DigestUtil;
import com.carlos.core.constant.RedisKeys;

/**
 * carlos-auth 模块全局 Redis Key 常量类
 *
 * <p>所有 Redis Key 统一收口，禁止在业务代码中硬编码 Key 字符串。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public final class AuthRedisKey {

    // ========== OAuth2 Authorization 祖前缀（须最先声明：下列多个常量引用它） ==========
    public static final String AUTHORIZATION_PREFIX = "auth:oauth2";

    // ========== Captcha ==========
    public static final String CAPTCHA = "auth:captcha:%s:%s";
    public static final String CAPTCHA_LIMIT = "auth:captcha:limit:%s:%s";

    // ========== Login Security ==========
    public static final String USER_LOCK = "auth:lock:user:%s";
    public static final String USER_ATTEMPT = "auth:attempt:user:%s";
    public static final String IP_LOCK = "auth:lock:ip:%s";
    public static final String IP_ATTEMPT = "auth:attempt:ip:%s";

    // ========== Rate Limit ==========
    public static final String RATE_IP = "auth:rate:ip:%s";
    public static final String RATE_LOGIN = "auth:rate:login:%s";
    public static final String RATE_CAPTCHA_SMS = "auth:rate:captcha:sms:%s";
    public static final String RATE_CAPTCHA_EMAIL = "auth:rate:captcha:email:%s";
    public static final String RATE_SSO_LOGOUT = "auth:sso:logout:%s";
    public static final String RATE_SSO_EXCHANGE_IP = "auth:sso:exchange:%s";

    // ========== Location ==========
    public static final String COMMON_LOCATIONS = "auth:common:locations:%s";

    // ========== OAuth2 Client ==========
    public static final String OAUTH2_CLIENT = AUTHORIZATION_PREFIX + ":client:%s:%s";
    public static final String OAUTH2_CONSENT = AUTHORIZATION_PREFIX + ":consent:%s:%s";

    // ========== Password Encryption Key ==========
    public static final String PASSWORD_PRIVATE_KEY = "auth:password:key:%s";

    // ========== 在线用户会话元数据（S4，RedisOAuth2AuthorizationService 签发时写入） ==========
    public static final String OAUTH2_SESSION_META = AUTHORIZATION_PREFIX + ":session_meta:%s";

    /** 会话元数据字段名（RedisOAuth2AuthorizationService 写入 / OnlineUserManager 读取共用口径） */
    public static final String SESSION_META_FIELD_IP = "ip";
    public static final String SESSION_META_FIELD_UA = "userAgent";

    /** 用户会话索引 Key 前缀（扫描结果截取用户名用，OnlineUserManager 共用） */
    public static final String USER_SESSIONS_KEY_PREFIX = AUTHORIZATION_PREFIX + ":user_sessions:";

    // ========== 网关内省缓存（OpaqueTokenValidator，踢下线时需同步删除） ==========
    public static final String TOKEN_INTROSPECTION = RedisKeys.AUTH_TOKEN_INTROSPECTION + "%s";

    // ========== Passkey / WebAuthn ==========
    public static final String PASSKEY_CHALLENGE = "auth:passkey:challenge:%s";
    public static final String PASSKEY_REGISTRATION = "auth:passkey:registration:%s";

    // ========== Social Login ==========
    public static final String SOCIAL_STATE = "auth:social:state:%s";

    private AuthRedisKey() {
        // utility class
    }

    /**
     * 构建验证码 Key
     */
    public static String captcha(String type, String target) {
        return String.format(CAPTCHA, type, target);
    }

    /**
     * 构建验证码限制 Key
     */
    public static String captchaLimit(String type, String target) {
        return String.format(CAPTCHA_LIMIT, type, target);
    }

    /**
     * 构建用户锁定 Key
     */
    public static String userLock(String username) {
        return String.format(USER_LOCK, username);
    }

    /**
     * 构建用户尝试次数 Key
     */
    public static String userAttempt(String username) {
        return String.format(USER_ATTEMPT, username);
    }

    /**
     * 构建 IP 封禁 Key
     */
    public static String ipLock(String ip) {
        return String.format(IP_LOCK, ip);
    }

    /**
     * 构建 IP 尝试次数 Key
     */
    public static String ipAttempt(String ip) {
        return String.format(IP_ATTEMPT, ip);
    }

    /**
     * 构建 IP 速率限制 Key
     */
    public static String rateIp(String ip) {
        return String.format(RATE_IP, ip);
    }

    /**
     * 构建登录速率限制 Key
     */
    public static String rateLogin(String username) {
        return String.format(RATE_LOGIN, username);
    }

    /**
     * 构建短信验证码速率限制 Key
     */
    public static String rateCaptchaSms(String phone) {
        return String.format(RATE_CAPTCHA_SMS, phone);
    }

    /**
     * 构建邮箱验证码速率限制 Key
     */
    public static String rateCaptchaEmail(String email) {
        return String.format(RATE_CAPTCHA_EMAIL, email);
    }

    /**
     * 构建 SSO 登出速率限制 Key
     */
    public static String rateSsoLogout(String token) {
        return String.format(RATE_SSO_LOGOUT, token);
    }

    /**
     * 构建常用登录地点 Key
     */
    public static String commonLocations(Long userId) {
        return String.format(COMMON_LOCATIONS, userId);
    }

    /**
     * 构建 OAuth2 客户端缓存 Key
     */
    public static String oauth2Client(String id, String appKey) {
        return String.format(OAUTH2_CLIENT, id, appKey);
    }

    /**
     * 构建 OAuth2 授权同意 Key
     */
    public static String oauth2Consent(String clientId, String principalName) {
        return String.format(OAUTH2_CONSENT, clientId, principalName);
    }

    /**
     * 构建 SSO Token 交换 IP 速率限制 Key
     */
    public static String rateSsoExchangeIp(String ip) {
        return String.format(RATE_SSO_EXCHANGE_IP, ip);
    }

    /**
     * 构建密码加密私钥 Key
     */
    public static String passwordPrivateKey(String keyId) {
        return String.format(PASSWORD_PRIVATE_KEY, keyId);
    }

    /**
     * 构建 OAuth2 授权 Key（通用）
     */
    public static String oauth2Key(String type, String id) {
        return String.format("%s:%s:%s", AUTHORIZATION_PREFIX, type, id);
    }

    /**
     * 构建在线用户会话元数据 Key
     */
    public static String sessionMeta(String authorizationId) {
        return String.format(OAUTH2_SESSION_META, authorizationId);
    }

    /**
     * 构建网关内省缓存 Key（value 为 token 原文的 SHA-256，与网关 OpaqueTokenValidator 口径一致）
     */
    public static String tokenIntrospection(String tokenValue) {
        return String.format(TOKEN_INTROSPECTION, DigestUtil.sha256Hex(tokenValue));
    }

    /**
     * 用户会话索引扫描 Pattern（{@code auth:oauth2:user_sessions:*}，在线用户枚举用）
     */
    public static String userSessionPattern() {
        return oauth2Key("user_sessions", "*");
    }

    /**
     * 用户会话索引 Key（{@code auth:oauth2:user_sessions:{username}}）
     */
    public static String userSessions(String username) {
        return oauth2Key("user_sessions", username);
    }

    /**
     * 用户会话索引扫描 Pattern（按用户名模糊缩小范围；已剔除 Redis glob 元字符防注入）
     *
     * @param keyword 用户名关键字，空则匹配全部
     */
    public static String userSessionPattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return userSessionPattern();
        }
        // Redis glob 元字符（* ? [ ]）剔除，保证关键字只做字面量子串匹配
        String sanitized = keyword.replaceAll("[*?[\\]]", "");
        return oauth2Key("user_sessions", "*" + sanitized + "*");
    }

    /**
     * 从用户会话索引 Key 中截取用户名
     */
    public static String extractUsername(String sessionIndexKey) {
        return sessionIndexKey.substring(USER_SESSIONS_KEY_PREFIX.length());
    }

    /**
     * 构建 Passkey challenge Key
     */
    public static String passkeyChallenge(String token) {
        return String.format(PASSKEY_CHALLENGE, token);
    }

    /**
     * 构建 Passkey registration Key
     */
    public static String passkeyRegistration(String token) {
        return String.format(PASSKEY_REGISTRATION, token);
    }

    /**
     * 构建社交登录 State Key（防 CSRF）
     */
    public static String socialState(String state) {
        return String.format(SOCIAL_STATE, state);
    }
}
