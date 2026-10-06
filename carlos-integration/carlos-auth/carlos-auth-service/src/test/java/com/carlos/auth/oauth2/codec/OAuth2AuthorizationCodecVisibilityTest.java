package com.carlos.auth.oauth2.codec;

import com.carlos.auth.security.SecurityUser;
import com.carlos.core.auth.LoginUserInfo;
import com.carlos.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;

import java.io.Serializable;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 Jackson 3 升级后 OAuth2Authorization 序列化/反序列化是否正常
 */
class OAuth2AuthorizationCodecVisibilityTest {

    private OAuth2AuthorizationCodec codec;
    private OAuth2Authorization authorization;

    @BeforeEach
    void setUp() {
        // 构建一个完整的 OAuth2Authorization 实例
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "test-access-token",
            Instant.now(),
            Instant.now().plus(1, ChronoUnit.HOURS),
            Set.of("read", "write")
        );

        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
            "test-refresh-token",
            Instant.now(),
            Instant.now().plus(7, ChronoUnit.DAYS)
        );

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user123");
        claims.put("name", "Test User");

        OidcIdToken idToken = new OidcIdToken(
            "test-id-token",
            Instant.now(),
            Instant.now().plus(1, ChronoUnit.HOURS),
            claims
        );

        OAuth2Authorization.Builder builder = createAuthorizationBuilder("client-123");
        authorization = builder
            .id("auth-123")
            .principalName("user123")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of("openid", "profile", "read"))
            .attribute("customAttr", "customValue")
            .token(accessToken, metadata -> metadata.put("format", OAuth2TokenFormat.SELF_CONTAINED.getValue()))
            .token(refreshToken)
            .token(idToken)
            .attribute(OAuth2ParameterNames.STATE, "xyz-state")
            .build();

        codec = new OAuth2AuthorizationCodec();
    }

    /**
     * 通过反射创建 OAuth2Authorization.Builder（构造函数为包私有）
     */
    private OAuth2Authorization.Builder createAuthorizationBuilder(String registeredClientId) {
        try {
            java.lang.reflect.Constructor<OAuth2Authorization.Builder> constructor =
                OAuth2Authorization.Builder.class.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(registeredClientId);
        } catch (Exception e) {
            throw new IllegalStateException("无法创建 OAuth2Authorization.Builder", e);
        }
    }

    @Test
    @DisplayName("序列化/反序列化应正常")
    void testSerializeDeserialize() {
        String json = codec.serialize(authorization);
        System.out.println("=== Serialized JSON ===");
        System.out.println(json);
        System.out.println();

        OAuth2Authorization deserialized = codec.deserialize(json);
        assertDeserializationEquals(authorization, deserialized);
    }

    @Test
    @DisplayName("逐字段验证反序列化完整性")
    void testFieldByField() {
        String json = codec.serialize(authorization);
        OAuth2Authorization deserialized = codec.deserialize(json);

        assertAll("OAuth2Authorization 各字段反序列化验证",
            () -> assertEquals(authorization.getId(), deserialized.getId(), "id"),
            () -> assertEquals(authorization.getRegisteredClientId(), deserialized.getRegisteredClientId(), "registeredClientId"),
            () -> assertEquals(authorization.getPrincipalName(), deserialized.getPrincipalName(), "principalName"),
            () -> assertEquals(authorization.getAuthorizationGrantType(), deserialized.getAuthorizationGrantType(), "authorizationGrantType"),
            () -> assertEquals(authorization.getAuthorizedScopes(), deserialized.getAuthorizedScopes(), "authorizedScopes"),
            () -> assertEquals((String) authorization.getAttribute(OAuth2ParameterNames.STATE),
                (String) deserialized.getAttribute(OAuth2ParameterNames.STATE), "state"),
            () -> assertNotNull(deserialized.getAccessToken(), "accessToken 不应为 null"),
            () -> assertEquals(authorization.getAccessToken().getToken().getTokenValue(),
                deserialized.getAccessToken().getToken().getTokenValue(), "accessToken value"),
            () -> assertNotNull(deserialized.getRefreshToken(), "refreshToken 不应为 null"),
            () -> assertEquals(authorization.getRefreshToken().getToken().getTokenValue(),
                deserialized.getRefreshToken().getToken().getTokenValue(), "refreshToken value"),
            () -> assertNotNull(deserialized.getToken(OidcIdToken.class), "idToken 不应为 null"),
            () -> assertEquals(authorization.getToken(OidcIdToken.class).getToken().getTokenValue(),
                deserialized.getToken(OidcIdToken.class).getToken().getTokenValue(), "idToken value"),
            () -> assertEquals("customValue", deserialized.getAttribute("customAttr"), "custom attribute")
        );
    }

    @Test
    @DisplayName("attributes 中的 Authentication 属性应能正确序列化/反序列化")
    void testDeserializePrincipalAttribute() {
        // 复现现象：attributes 存 Principal -> UsernamePasswordAuthenticationToken，
        // principal 为 SecurityUser、authorities 为 SimpleGrantedAuthority
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
            createSecurityUser("user123"),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        OAuth2Authorization authorization = createAuthorizationBuilder("client-123")
            .id("auth-principal")
            .principalName("user123")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .attribute(Principal.class.getName(), authentication)
            .build();

        String json = codec.serialize(authorization);
        System.out.println("=== Serialized JSON (principal attribute) ===");
        System.out.println(json);
        System.out.println();

        OAuth2Authorization deserialized = codec.deserialize(json);

        Object attribute = deserialized.getAttribute(Principal.class.getName());
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, attribute,
            "attribute 应还原为 UsernamePasswordAuthenticationToken");

        UsernamePasswordAuthenticationToken restored = (UsernamePasswordAuthenticationToken) attribute;
        assertAll("principal / authorities 还原验证",
            () -> assertInstanceOf(SecurityUser.class, restored.getPrincipal(), "principal 类型"),
            () -> assertEquals("user123", ((SecurityUser) restored.getPrincipal()).getAccount(), "principal account"),
            () -> assertTrue(restored.isAuthenticated(), "authenticated"),
            () -> assertEquals(1, restored.getAuthorities().size(), "authorities 数量"),
            () -> assertEquals("ROLE_ADMIN", restored.getAuthorities().iterator().next().getAuthority(), "authority 值")
        );
    }

    @Test
    @DisplayName("旧格式（As.WRAPPER_ARRAY）存量数据应仍可反序列化")
    void testDeserializeLegacyWrapperArrayJson() {
        OAuth2Authorization deserialized = codec.deserialize(LEGACY_WRAPPER_ARRAY_JSON);

        assertAll("旧格式存量数据反序列化验证",
            () -> assertEquals("auth-legacy", deserialized.getId(), "id"),
            () -> assertEquals("client-123", deserialized.getRegisteredClientId(), "registeredClientId"),
            () -> assertEquals("user123", deserialized.getPrincipalName(), "principalName"),
            () -> assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE,
                deserialized.getAuthorizationGrantType(), "authorizationGrantType"),
            () -> assertEquals(Set.of("read"), deserialized.getAuthorizedScopes(), "authorizedScopes"),
            () -> assertEquals("customValue", deserialized.getAttribute("customAttr"), "custom attribute"),
            () -> assertEquals("xyz-state",
                deserialized.getAttribute(OAuth2ParameterNames.STATE), "state attribute"),
            () -> assertNotNull(deserialized.getAccessToken(), "accessToken 不应为 null"),
            () -> assertEquals("test-access-token",
                deserialized.getAccessToken().getToken().getTokenValue(), "accessToken value"),
            () -> assertEquals("self-contained",
                deserialized.getAccessToken().getMetadata().get("format"), "accessToken metadata")
        );
    }

    @Test
    @DisplayName("已知限制：旧格式中的 Authentication 属性当前无法反序列化")
    void testLegacyWrapperArrayAuthenticationAttributeIsNotReadable() {
        // 新配置 default typing 为 As.PROPERTY，而旧数据里 principal 被写成
        // ["com.carlos.auth.security.SecurityUser",{...}] 数组；Spring Security 官方的
        // UsernamePasswordAuthenticationTokenDeserializer 只识别「对象」形式的 principal 节点
        // （非对象时调用 asString()），因此这类旧数据在修复前、修复后都读不出来。
        // 本用例固化该现状：若将来（Jackson/Spring Security 升级或改回自定义 Mixin）变得可读，
        // 该断言会失败，提示此处需要更新。
        BusinessException ex = assertThrows(BusinessException.class,
            () -> codec.deserialize(LEGACY_WRAPPER_ARRAY_JSON_WITH_AUTHENTICATION),
            "旧格式中的 Authentication 属性当前应反序列化失败");
        assertTrue(ex.getMessage().contains("asString"),
            "应为官方 Deserializer 对数组形式 principal 调用 asString() 失败，实际：" + ex.getMessage());
    }

    /**
     * 构造 SecurityUser（其构造器依赖 LoginUserInfo）
     */
    private SecurityUser createSecurityUser(String account) {
        LoginUserInfo loginUserInfo = new LoginUserInfo()
            .setId("1")
            .setClientId("client-123")
            .setAccount(account)
            .setPassword("password")
            .setEnable(true)
            .setRoleIds(Set.<Serializable>of("role-admin"));
        return new SecurityUser(loginUserInfo);
    }

    private void assertDeserializationEquals(OAuth2Authorization expected, OAuth2Authorization actual) {
        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getRegisteredClientId(), actual.getRegisteredClientId());
        assertEquals(expected.getPrincipalName(), actual.getPrincipalName());
        assertEquals(expected.getAuthorizationGrantType(), actual.getAuthorizationGrantType());
        assertEquals(expected.getAuthorizedScopes(), actual.getAuthorizedScopes());
    }

    /**
     * 修复前的存量数据格式：default typing 为 {@code As.WRAPPER_ARRAY}，
     * 类型信息为 {@code ["类名",值]} 数组。该字面量取自修复前的真实序列化输出，
     * 用于验证存量数据（不含 Authentication 属性）在新配置下仍可读取。
     */
    private static final String LEGACY_WRAPPER_ARRAY_JSON = """
        {"id":"auth-legacy","registeredClientId":"client-123","principalName":"user123","authorizationGrantType":"authorization_code","authorizedScopes":["read"],"attributes":["java.util.LinkedHashMap",{"customAttr":"customValue","state":"xyz-state"}],"accessToken":{"token":{"tokenType":"Bearer","tokenValue":"test-access-token","issuedAt":"2026-09-15T15:40:52.503767Z","expiresAt":"2026-09-15T16:40:52.503767Z","scopes":["read","write"]},"metadata":["java.util.LinkedHashMap",{"format":"self-contained","metadata.token.invalidated":false}],"claims":{}}}
        """;

    /**
     * 修复前的存量数据格式，attributes 中含 Authentication 属性：
     * principal 被写成 {@code ["com.carlos.auth.security.SecurityUser",{...}]} 数组。
     * 该字面量取自修复前的真实序列化输出。
     */
    private static final String LEGACY_WRAPPER_ARRAY_JSON_WITH_AUTHENTICATION = """
        {"id":"auth-legacy","registeredClientId":"client-123","principalName":"user123","authorizationGrantType":"authorization_code","authorizedScopes":["read"],"attributes":["java.util.LinkedHashMap",{"java.security.Principal":["org.springframework.security.authentication.UsernamePasswordAuthenticationToken",{"principal":["com.carlos.auth.security.SecurityUser",{"account":"user123","authorities":["java.util.ArrayList",[["org.springframework.security.core.authority.SimpleGrantedAuthority",{"authority":"role-admin"}]]],"clientId":"client-123","enable":true,"id":"1"}],"credentials":null,"authorities":["java.util.Collections$UnmodifiableRandomAccessList",[["org.springframework.security.core.authority.SimpleGrantedAuthority",{"authority":"ROLE_ADMIN"}]]],"authenticated":true,"details":null}]}],"accessToken":{"token":{"tokenType":"Bearer","tokenValue":"test-access-token","issuedAt":"2026-09-15T15:40:52.503767Z","expiresAt":"2026-09-15T16:40:52.503767Z","scopes":["read","write"]},"metadata":["java.util.LinkedHashMap",{"format":"self-contained","metadata.token.invalidated":false}],"claims":{}}}
        """;
}
