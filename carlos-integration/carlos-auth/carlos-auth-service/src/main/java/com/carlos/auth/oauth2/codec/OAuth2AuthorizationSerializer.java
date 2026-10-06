package com.carlos.auth.oauth2.codec;

import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OAuth2Authorization 自定义序列化器
 * <p>
 * 解决以下问题：
 * <ul>
 *   <li>Spring Security allowlist 阻止 OAuth2Authorization 反序列化</li>
 *   <li>Visibility.ANY 安全风险</li>
 *   <li>private tokens Map 无法通过标准 Jackson 反序列化</li>
 * </ul>
 * <p>
 * 序列化策略：
 * <ul>
 *   <li>token 字段手动扁平化序列化（不依赖 default typing）</li>
 *   <li>attributes 保留 default typing 以支持复杂对象（如 OAuth2AuthorizationRequest）</li>
 * </ul>
 *
 * @author Carlos
 * @date 2026-05-23
 */
public class OAuth2AuthorizationSerializer extends ValueSerializer<OAuth2Authorization> {

    @Override
    public void serialize(OAuth2Authorization value, JsonGenerator gen, SerializationContext serializers) {
        gen.writeStartObject();

        gen.writeStringProperty("id", value.getId());
        gen.writeStringProperty("registeredClientId", value.getRegisteredClientId());
        gen.writeStringProperty("principalName", value.getPrincipalName());
        gen.writeStringProperty("authorizationGrantType", value.getAuthorizationGrantType().getValue());

        gen.writeArrayPropertyStart("authorizedScopes");
        for (String scope : value.getAuthorizedScopes()) {
            gen.writeString(scope);
        }
        gen.writeEndArray();

        gen.writePOJOProperty("attributes", new LinkedHashMap<>(value.getAttributes()));

        writeToken(gen, "accessToken", value.getAccessToken(), this::writeAccessToken);
        writeToken(gen, "refreshToken", value.getRefreshToken(), this::writeRefreshToken);
        writeToken(gen, "idToken", value.getToken(OidcIdToken.class), this::writeOidcIdToken);
        writeToken(gen, "authorizationCode", value.getToken(OAuth2AuthorizationCode.class), this::writeAuthorizationCode);

        gen.writeEndObject();
    }

    private <T extends OAuth2Token> void writeToken(JsonGenerator gen, String fieldName,
                                                    OAuth2Authorization.Token<T> token,
                                                    TokenWriter<T> writer) {
        if (token == null) {
            return;
        }
        gen.writeObjectPropertyStart(fieldName);
        gen.writeObjectPropertyStart("token");
        writer.write(gen, token.getToken());
        gen.writeEndObject();
        writeMapField(gen, "metadata", token.getMetadata());
        writeMapField(gen, "claims", token.getClaims());
        gen.writeEndObject();
    }

    private void writeAccessToken(JsonGenerator gen, OAuth2AccessToken token) {
        gen.writeStringProperty("tokenType", token.getTokenType().getValue());
        gen.writeStringProperty("tokenValue", token.getTokenValue());
        gen.writeStringProperty("issuedAt", token.getIssuedAt() != null ? token.getIssuedAt().toString() : null);
        gen.writeStringProperty("expiresAt", token.getExpiresAt() != null ? token.getExpiresAt().toString() : null);
        gen.writeArrayPropertyStart("scopes");
        if (token.getScopes() != null) {
            for (String scope : token.getScopes()) {
                gen.writeString(scope);
            }
        }
        gen.writeEndArray();
    }

    private void writeRefreshToken(JsonGenerator gen, OAuth2RefreshToken token) {
        gen.writeStringProperty("tokenValue", token.getTokenValue());
        gen.writeStringProperty("issuedAt", token.getIssuedAt() != null ? token.getIssuedAt().toString() : null);
        gen.writeStringProperty("expiresAt", token.getExpiresAt() != null ? token.getExpiresAt().toString() : null);
    }

    private void writeOidcIdToken(JsonGenerator gen, OidcIdToken token) {
        gen.writeStringProperty("tokenValue", token.getTokenValue());
        gen.writeStringProperty("issuedAt", token.getIssuedAt() != null ? token.getIssuedAt().toString() : null);
        gen.writeStringProperty("expiresAt", token.getExpiresAt() != null ? token.getExpiresAt().toString() : null);
        writeMapField(gen, "claims", token.getClaims());
    }

    private void writeAuthorizationCode(JsonGenerator gen, OAuth2AuthorizationCode token) {
        gen.writeStringProperty("tokenValue", token.getTokenValue());
        gen.writeStringProperty("issuedAt", token.getIssuedAt() != null ? token.getIssuedAt().toString() : null);
        gen.writeStringProperty("expiresAt", token.getExpiresAt() != null ? token.getExpiresAt().toString() : null);
    }

    private void writeMapField(JsonGenerator gen, String fieldName, Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            gen.writeObjectPropertyStart(fieldName);
            gen.writeEndObject();
            return;
        }
        // 转换为 LinkedHashMap，避免 UnmodifiableMapMixin 添加 PROPERTY 格式类型信息
        // 与 WRAPPER_ARRAY 格式的 default typing 冲突
        gen.writePOJOProperty(fieldName, new LinkedHashMap<>(map));
    }

    @FunctionalInterface
    private interface TokenWriter<T extends OAuth2Token> {
        void write(JsonGenerator gen, T token);
    }
}
