package com.carlos.auth.passkey;

import com.alibaba.fastjson2.JSON;
import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.auth.passkey.manager.PasskeyCredentialManager;
import com.carlos.auth.passkey.pojo.entity.PasskeyCredential;
import com.carlos.auth.passkey.pojo.param.PasskeyAuthenticateFinishRequest;
import com.carlos.auth.passkey.pojo.param.PasskeyRegisterFinishRequest;
import com.carlos.auth.provider.UserInfo;
import com.carlos.auth.provider.UserProvider;
import com.carlos.core.exception.ComponentException;
import com.carlos.redis.util.RedisUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;

/**
 * <p>
 * Passkey/WebAuthn 服务
 * </p>
 *
 * <p>基于 Yubico webauthn-server-core 实现 FIDO2/WebAuthn 注册与认证。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "carlos.auth.login.methods", name = "passkey", havingValue = "true")
public class PasskeyService {

    private static final int MAX_CLIENT_DATA_JSON_LENGTH = 4096;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RelyingParty relyingParty;
    private final PasskeyCredentialManager credentialManager;
    private final UserProvider userProvider;
    private final ObjectMapper objectMapper;
    private final long challengeTtlSeconds;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public PasskeyService(RelyingParty relyingParty,
                          PasskeyCredentialManager credentialManager,
                          UserProvider userProvider,
                          ObjectMapper objectMapper,
                          com.carlos.auth.config.OAuth2Properties oauth2Properties) {
        this.relyingParty = relyingParty;
        this.credentialManager = credentialManager;
        this.userProvider = userProvider;
        this.objectMapper = objectMapper;
        this.challengeTtlSeconds = oauth2Properties.getLogin().getMethods()
                .getPasskeyConfig().getChallengeTtl();
    }

    // ==================== Registration ====================

    public Map<String, Object> beginRegistration(String account) {
        UserInfo userInfo = resolveUser(account);
        String userId = String.valueOf(userInfo.getUserId());
        ByteArray userHandle = getOrCreateUserHandle(userId);

        UserIdentity userIdentity = UserIdentity.builder()
                .name(userId)
                .displayName(account)
                .id(userHandle)
                .build();

        PublicKeyCredentialCreationOptions options = relyingParty.startRegistration(
                StartRegistrationOptions.builder()
                        .user(userIdentity)
                        .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                                .residentKey(ResidentKeyRequirement.PREFERRED)
                                .userVerification(UserVerificationRequirement.PREFERRED)
                                .build())
                        .build());

        // 缓存完整 options JSON + 上下文
        String challengeB64 = options.getChallenge().getBase64Url();
        String optionsJson = serializeToJson(options);
        PasskeyChallengeCache cache = new PasskeyChallengeCache();
        cache.setOptionsJson(optionsJson);
        cache.setUserId(userId);
        cache.setAccount(account);
        cache.setUserHandleB64(userHandle.getBase64Url());
        RedisUtil.setValue(AuthRedisKey.passkeyRegistration(challengeB64),
                serializeToJson(cache), Duration.ofSeconds(challengeTtlSeconds));

        log.info("Passkey registration begin: account={}", account);
        return objectMapper.convertValue(options, new TypeReference<>() {});
    }

    public boolean finishRegistration(String account, PasskeyRegisterFinishRequest request) {
        String challenge = extractChallengeFromClientDataJSON(request.getResponse().getClientDataJSON());
        if (challenge == null) {
            log.warn("Cannot extract challenge from clientDataJSON in registration");
            return false;
        }

        String key = AuthRedisKey.passkeyRegistration(challenge);
        String cachedJson = RedisUtil.getValue(key);
        if (cachedJson == null) {
            log.warn("Registration challenge expired for account: {}", account);
            return false;
        }
        // 防止重放：原子删除并检查
        if (!RedisUtil.delete(key)) {
            log.warn("Registration challenge already consumed (possible replay) for account: {}", account);
            return false;
        }

        PasskeyChallengeCache cache = parseCache(cachedJson);
        if (cache == null || cache.getOptionsJson() == null) {
            log.warn("Invalid registration cache for account: {}", account);
            return false;
        }

        try {
            PublicKeyCredentialCreationOptions creationOptions =
                    objectMapper.readValue(cache.getOptionsJson(), PublicKeyCredentialCreationOptions.class);

            String requestJson = objectMapper.writeValueAsString(request);
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> credential =
                    objectMapper.readValue(requestJson, new TypeReference<>() {});

            RegistrationResult result = relyingParty.finishRegistration(
                    FinishRegistrationOptions.builder()
                            .request(creationOptions)
                            .response(credential)
                            .build());

            PasskeyCredential entity = new PasskeyCredential();
            entity.setUserId(Long.valueOf(cache.getUserId()));
            entity.setCredentialId(result.getKeyId().getId().getBytes());
            entity.setCredentialIdB64(result.getKeyId().getId().getBase64Url());
            entity.setPublicKeyCose(result.getPublicKeyCose().getBytes());
            entity.setSignCount(result.getSignatureCount());
            entity.setRpId(creationOptions.getRp().getId());
            entity.setUserHandle(creationOptions.getUser().getId().getBytes());
            entity.setUserHandleB64(creationOptions.getUser().getId().getBase64Url());

            if (result.getAttestationType() != null) {
                entity.setAttestationType(result.getAttestationType().name().toLowerCase());
            }
            if (request.getAuthenticatorAttachment() != null) {
                entity.setDeviceName(request.getAuthenticatorAttachment());
            }
            if (request.getResponse() != null && request.getResponse().getTransports() != null
                    && !request.getResponse().getTransports().isEmpty()) {
                entity.setTransports(JSON.toJSONString(request.getResponse().getTransports()));
            }

            credentialManager.saveCredential(entity);
            log.info("Passkey registered: account={}, credId={}", account, entity.getCredentialIdB64());
            return true;

        } catch (JsonProcessingException e) {
            log.error("Passkey registration JSON parse error for account: {}", account, e);
            return false;
        } catch (Exception e) {
            log.error("Passkey registration failed for account: {}", account, e);
            return false;
        }
    }

    // ==================== Authentication ====================

    public Map<String, Object> beginAuthentication(String username) {
        StartAssertionOptions.StartAssertionOptionsBuilder builder = StartAssertionOptions.builder()
                .userVerification(UserVerificationRequirement.PREFERRED);

        String userId = null;
        if (username != null && !username.isBlank()) {
            try {
                UserInfo userInfo = userProvider.loadUserByIdentifier(username);
                if (userInfo != null && userInfo.getUserId() != null) {
                    userId = String.valueOf(userInfo.getUserId());
                    builder.username(userId);
                }
            } catch (Exception e) {
                log.debug("Cannot resolve user for passkey begin: {}", username);
            }
        }

        AssertionRequest assertionRequest = relyingParty.startAssertion(builder.build());

        String challengeB64 =
                assertionRequest.getPublicKeyCredentialRequestOptions().getChallenge().getBase64Url();

        String requestJson = serializeToJson(assertionRequest);
        PasskeyChallengeCache cache = new PasskeyChallengeCache();
        cache.setOptionsJson(requestJson);
        cache.setUserId(userId != null ? userId : "");
        cache.setAccount(username != null ? username : "");
        RedisUtil.setValue(AuthRedisKey.passkeyChallenge(challengeB64), serializeToJson(cache),
                Duration.ofSeconds(challengeTtlSeconds));

        log.info("Passkey authentication begin: username={}", username);
        return objectMapper.convertValue(
                assertionRequest.getPublicKeyCredentialRequestOptions(), new TypeReference<>() {});
    }

    public String finishAuthentication(PasskeyAuthenticateFinishRequest request) {
        try {
            String challenge = extractChallengeFromClientDataJSON(
                    request.getResponse() != null ? request.getResponse().getClientDataJSON() : null);
            if (challenge == null) {
                log.warn("Cannot extract challenge from assertion clientDataJSON");
                return null;
            }

            String key = AuthRedisKey.passkeyChallenge(challenge);
            String cachedJson = RedisUtil.getValue(key);
            if (cachedJson == null) {
                log.warn("Auth challenge expired or not found");
                return null;
            }
            // 防止重放攻击：原子删除并检查返回值
            if (!RedisUtil.delete(key)) {
                log.warn("Auth challenge already consumed (possible replay attack)");
                return null;
            }

            PasskeyChallengeCache cache = parseCache(cachedJson);
            if (cache == null || cache.getOptionsJson() == null) {
                log.warn("Invalid auth cache");
                return null;
            }

            AssertionRequest assertionRequest =
                    objectMapper.readValue(cache.getOptionsJson(), AssertionRequest.class);

            String credentialJson = objectMapper.writeValueAsString(request);
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> credential =
                    objectMapper.readValue(credentialJson, new TypeReference<>() {});

            AssertionResult result = relyingParty.finishAssertion(
                    FinishAssertionOptions.builder()
                            .request(assertionRequest)
                            .response(credential)
                            .build());

            if (!result.isSuccess()) {
                log.warn("Passkey assertion verification failed");
                return null;
            }

            // 更新 sign count（使用 CAS 乐观锁防止计数器倒退）
            String credentialIdB64 = result.getCredential().getCredentialId().getBase64Url();
            PasskeyCredential entity = credentialManager.getByCredentialIdB64(credentialIdB64);
            if (entity != null) {
                credentialManager.updateSignCountCAS(entity.getId(), entity.getSignCount(),
                        result.getSignatureCount());
            }

            String username = resolveUsername(result, cache.getAccount());
            if (username == null) {
                log.error("Cannot resolve username from assertion result");
                return null;
            }

            log.info("Passkey authentication success: username={}", username);
            return username;

        } catch (JsonProcessingException e) {
            log.error("Passkey authentication JSON parse error", e);
            return null;
        } catch (Exception e) {
            log.error("Passkey authentication failed", e);
            return null;
        }
    }

    // ==================== Credential Management ====================

    public List<Map<String, Object>> listCredentials(String account) {
        UserInfo userInfo = userProvider.loadUserByIdentifier(account);
        if (userInfo == null || userInfo.getUserId() == null) {
            return Collections.emptyList();
        }
        return credentialManager.listByUserId(userInfo.getUserId()).stream()
                .map(cred -> {
                    Map<String, Object> vo = new LinkedHashMap<>();
                    vo.put("credentialId", cred.getCredentialIdB64());
                    vo.put("deviceName",
                            cred.getDeviceName() != null ? cred.getDeviceName() : "unknown");
                    vo.put("lastUsedTime", cred.getLastUsedTime());
                    vo.put("createTime", cred.getCreateTime());
                    return vo;
                })
                .toList();
    }

    public void deleteCredential(String account, String credentialIdB64) {
        UserInfo userInfo = resolveUser(account);
        credentialManager.deleteByUserIdAndCredentialId(userInfo.getUserId(), credentialIdB64);
        log.info("Passkey credential deleted: account={}, credId={}", account, credentialIdB64);
    }

    // ==================== Private Helpers ====================

    private UserInfo resolveUser(String account) {
        UserInfo user = userProvider.loadUserByIdentifier(account);
        if (user == null || user.getUserId() == null) {
            throw AuthErrorCode.AUTH_USER_NOT_FOUND.exception();
        }
        return user;
    }

    private ByteArray getOrCreateUserHandle(String userId) {
        List<PasskeyCredential> existing =
                credentialManager.listByUserId(Long.parseLong(userId));
        if (existing != null && !existing.isEmpty() && existing.get(0).getUserHandle() != null
                && existing.get(0).getUserHandle().length > 0) {
            return new ByteArray(existing.get(0).getUserHandle());
        }
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return new ByteArray(bytes);
    }

    private String extractChallengeFromClientDataJSON(String clientDataJSONB64) {
        if (clientDataJSONB64 == null || clientDataJSONB64.isEmpty()) {
            return null;
        }
        if (clientDataJSONB64.length() > MAX_CLIENT_DATA_JSON_LENGTH) {
            log.warn("clientDataJSON exceeds max length: {}", clientDataJSONB64.length());
            return null;
        }
        try {
            byte[] jsonBytes = Base64.getUrlDecoder().decode(clientDataJSONB64);
            @SuppressWarnings("unchecked")
            Map<String, Object> clientData = objectMapper.readValue(jsonBytes, Map.class);
            return (String) clientData.get("challenge");
        } catch (Exception e) {
            log.warn("Failed to parse clientDataJSON", e);
            return null;
        }
    }

    private String resolveUsername(AssertionResult result, String cachedUsername) {
        if (result.getUserHandle() != null) {
            try {
                var usernameOpt = relyingParty.getCredentialRepository()
                        .getUsernameForUserHandle(result.getUserHandle());
                if (usernameOpt.isPresent()) {
                    String resolved = usernameOpt.get();
                    if (resolved.matches("\\d+")) {
                        UserInfo user = userProvider.loadUserById(Long.parseLong(resolved));
                        if (user != null) return user.getUsername();
                    }
                    return resolved;
                }
            } catch (Exception e) {
                log.debug("userHandle lookup failed: {}", e.getMessage());
            }
        }
        try {
            String un = result.getUsername();
            if (un != null && un.matches("\\d+")) {
                UserInfo user = userProvider.loadUserById(Long.parseLong(un));
                if (user != null) return user.getUsername();
            }
            if (un != null && !un.isEmpty()) return un;
        } catch (Exception ignore) {}
        return cachedUsername;
    }

    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new ComponentException("JSON 序列化失败: " + e.getMessage());
        }
    }

    private PasskeyChallengeCache parseCache(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, PasskeyChallengeCache.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse challenge cache", e);
            return null;
        }
    }

    // ==================== Inner Types ====================

    /**
     * Redis 缓存的 challenge 上下文
     */
    static class PasskeyChallengeCache {
        private String optionsJson;
        private String userId;
        private String account;
        private String userHandleB64;

        public String getOptionsJson() { return optionsJson; }
        public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getAccount() { return account; }
        public void setAccount(String account) { this.account = account; }
        public String getUserHandleB64() { return userHandleB64; }
        public void setUserHandleB64(String userHandleB64) { this.userHandleB64 = userHandleB64; }
    }

}
