package com.carlos.auth.oauth2.codec;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * OAuth2Authorization 自定义反序列化器
 *
 * <p>
 * 与 {@link OAuth2AuthorizationSerializer} 配对使用，通过 Builder 模式重建 OAuth2Authorization 实例。
 * token 字段手动解析（不依赖 default typing），attributes 保留 default typing 支持。
 *
 * @author Carlos
 * @date 2026-05-23
 */
public class OAuth2AuthorizationDeserializer extends ValueDeserializer<OAuth2Authorization> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OAuth2AuthorizationDeserializer.class);

    @Override
    public OAuth2Authorization deserialize(JsonParser p, DeserializationContext ctxt) {
        JsonNode node = ctxt.readTree(p);

        String registeredClientId = getText(node, "registeredClientId");
        if (registeredClientId == null) {
            throw new IllegalArgumentException("registeredClientId is required");
        }

        OAuth2Authorization.Builder builder = createBuilder(registeredClientId);

        if (node.has("id")) {
            builder.id(getText(node, "id"));
        }
        if (node.has("principalName")) {
            builder.principalName(getText(node, "principalName"));
        }
        if (node.has("authorizationGrantType")) {
            builder.authorizationGrantType(new AuthorizationGrantType(getText(node, "authorizationGrantType")));
        }
        if (node.has("authorizedScopes")) {
            Set<String> scopes = new HashSet<>();
            for (JsonNode scopeNode : node.get("authorizedScopes")) {
                scopes.add(scopeNode.asText());
            }
            builder.authorizedScopes(scopes);
        }

        // tokens
        readAccessToken(ctxt, node, builder);
        readRefreshToken(ctxt, node, builder);
        readIdToken(ctxt, node, builder);
        readAuthorizationCode(ctxt, node, builder);

        // attributes
        if (node.has("attributes")) {
            Map<String, Object> attributes = readTreeAsValue(
                ctxt, node.get("attributes"), new TypeReference<Map<String, Object>>() {
                }
            );
            attributes.forEach(builder::attribute);
        }

        return builder.build();
    }

    private OAuth2Authorization.Builder createBuilder(String registeredClientId) {
        try {
            Constructor<OAuth2Authorization.Builder> constructor =
                OAuth2Authorization.Builder.class.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(registeredClientId);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法通过反射创建 OAuth2Authorization.Builder，可能是 Spring Security 版本不兼容", e);
        }
    }

    private String getText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() ? field.asText() : null;
    }

    private Instant parseInstant(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        return Instant.parse(field.asText());
    }

    private <T> T readTreeAsValue(DeserializationContext ctxt, JsonNode node, TypeReference<T> typeRef) {
        return ctxt.readTreeAsValue(node, ctxt.getTypeFactory().constructType(typeRef));
    }

    private void readAccessToken(DeserializationContext ctxt, JsonNode node, OAuth2Authorization.Builder builder) {
        if (!node.has("accessToken")) {
            return;
        }
        JsonNode tokenNode = node.get("accessToken");
        JsonNode tokenValueNode = tokenNode.get("token");

        String tokenTypeValue = getText(tokenValueNode, "tokenType");
        OAuth2AccessToken.TokenType tokenType = OAuth2AccessToken.TokenType.BEARER.getValue().equals(tokenTypeValue)
            ? OAuth2AccessToken.TokenType.BEARER : new OAuth2AccessToken.TokenType(tokenTypeValue);
        String tokenValue = getText(tokenValueNode, "tokenValue");
        Instant issuedAt = parseInstant(tokenValueNode, "issuedAt");
        Instant expiresAt = parseInstant(tokenValueNode, "expiresAt");

        Set<String> scopes = new HashSet<>();
        if (tokenValueNode.has("scopes")) {
            for (JsonNode scopeNode : tokenValueNode.get("scopes")) {
                scopes.add(scopeNode.asText());
            }
        }

        OAuth2AccessToken accessToken = new OAuth2AccessToken(tokenType, tokenValue, issuedAt, expiresAt, scopes);
        Map<String, Object> metadata = readMetadata(ctxt, tokenNode);
        builder.token(accessToken, m -> m.putAll(metadata));
    }

    private void readRefreshToken(DeserializationContext ctxt, JsonNode node, OAuth2Authorization.Builder builder) {
        if (!node.has("refreshToken")) {
            return;
        }
        JsonNode tokenNode = node.get("refreshToken");
        JsonNode tokenValueNode = tokenNode.get("token");

        String tokenValue = getText(tokenValueNode, "tokenValue");
        Instant issuedAt = parseInstant(tokenValueNode, "issuedAt");
        Instant expiresAt = parseInstant(tokenValueNode, "expiresAt");

        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(tokenValue, issuedAt, expiresAt);
        Map<String, Object> metadata = readMetadata(ctxt, tokenNode);
        builder.token(refreshToken, m -> m.putAll(metadata));
    }

    private void readIdToken(DeserializationContext ctxt, JsonNode node, OAuth2Authorization.Builder builder) {
        if (!node.has("idToken")) {
            return;
        }
        JsonNode tokenNode = node.get("idToken");
        JsonNode tokenValueNode = tokenNode.get("token");

        String tokenValue = getText(tokenValueNode, "tokenValue");
        Instant issuedAt = parseInstant(tokenValueNode, "issuedAt");
        Instant expiresAt = parseInstant(tokenValueNode, "expiresAt");

        Map<String, Object> claims = Collections.emptyMap();
        if (tokenValueNode.has("claims")) {
            claims = readTreeAsValue(
                ctxt, tokenValueNode.get("claims"), new TypeReference<Map<String, Object>>() {
                }
            );
        }

        OidcIdToken idToken = new OidcIdToken(tokenValue, issuedAt, expiresAt, claims);
        Map<String, Object> metadata = readMetadata(ctxt, tokenNode);
        builder.token(idToken, m -> m.putAll(metadata));
    }

    private void readAuthorizationCode(DeserializationContext ctxt, JsonNode node, OAuth2Authorization.Builder builder) {
        if (!node.has("authorizationCode")) {
            return;
        }
        JsonNode tokenNode = node.get("authorizationCode");
        JsonNode tokenValueNode = tokenNode.get("token");

        String tokenValue = getText(tokenValueNode, "tokenValue");
        Instant issuedAt = parseInstant(tokenValueNode, "issuedAt");
        Instant expiresAt = parseInstant(tokenValueNode, "expiresAt");

        OAuth2AuthorizationCode code = new OAuth2AuthorizationCode(tokenValue, issuedAt, expiresAt);
        Map<String, Object> metadata = readMetadata(ctxt, tokenNode);
        builder.token(code, m -> m.putAll(metadata));
    }

    private Map<String, Object> readMetadata(DeserializationContext ctxt, JsonNode tokenNode) {
        if (!tokenNode.has("metadata")) {
            return Collections.emptyMap();
        }
        JsonNode metadataNode = tokenNode.get("metadata");
        if (metadataNode == null || metadataNode.isNull()) {
            return Collections.emptyMap();
        }
        // 注意：default typing（WRAPPER_ARRAY）下 metadata 是 ["java.util.LinkedHashMap", {...}] 包装数组，
        // 直接交给 Jackson 解析（其 default typing 会解包），不要按 isObject() 拦截或手动解包
        try {
            return readTreeAsValue(ctxt, metadataNode, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("Token metadata 反序列化失败，使用空 Map 替代", e);
            return Collections.emptyMap();
        }
    }
}
