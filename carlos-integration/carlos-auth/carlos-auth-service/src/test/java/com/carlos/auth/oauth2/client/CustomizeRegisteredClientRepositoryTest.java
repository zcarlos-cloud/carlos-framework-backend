package com.carlos.auth.oauth2.client;

import com.carlos.auth.app.convert.CommonConvert;
import com.carlos.auth.app.pojo.dto.AppClientDTO;
import com.carlos.auth.app.pojo.dto.Oauth2TokenSettings;
import com.carlos.auth.app.service.AppClientService;
import com.carlos.auth.config.OAuth2Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * {@link CustomizeRegisteredClientRepository} 令牌 TTL 解析优先级测试
 *
 * <p>规则：DB 中该客户端显式配置了 token_settings TTL 就以 DB 为准；
 * 未配置时回落到 carlos.auth.login.access-token-ttl / refresh-token-ttl。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@ExtendWith(MockitoExtension.class)
class CustomizeRegisteredClientRepositoryTest {

    private static final String CLIENT_ID = "carlos-client";
    private static final Duration CONFIG_ACCESS_TOKEN_TTL = Duration.ofSeconds(1234);
    private static final Duration CONFIG_REFRESH_TOKEN_TTL = Duration.ofSeconds(5678);

    @Mock
    private AppClientService clientService;

    @Test
    @DisplayName("DB 显式配置了 TTL 时应以 DB 为准，不受全局配置影响")
    void should_use_db_ttl_when_configured() {
        AppClientDTO dto = newClientDTO(
            CommonConvert.str2tokensetting("{\"accessTokenTimeToLive\":30,\"refreshTokenTimeToLive\":120}"));
        when(clientService.findByAppkey(CLIENT_ID, false)).thenReturn(dto);

        RegisteredClient client = repository().findByClientId(CLIENT_ID);

        assertEquals(Duration.ofMinutes(30), client.getTokenSettings().getAccessTokenTimeToLive());
        assertEquals(Duration.ofMinutes(120), client.getTokenSettings().getRefreshTokenTimeToLive());
    }

    @Test
    @DisplayName("DB 未配置 TTL 时应回落到 carlos.auth.login.*-ttl")
    void should_fall_back_to_config_ttl_when_absent() {
        // DB 里的 token_settings 只配了格式，没有 TTL 字段
        AppClientDTO dto = newClientDTO(
            CommonConvert.str2tokensetting("{\"accessTokenFormat\":\"self-contained\"}"));
        when(clientService.findByAppkey(CLIENT_ID, false)).thenReturn(dto);

        RegisteredClient client = repository().findByClientId(CLIENT_ID);

        assertEquals(CONFIG_ACCESS_TOKEN_TTL, client.getTokenSettings().getAccessTokenTimeToLive());
        assertEquals(CONFIG_REFRESH_TOKEN_TTL, client.getTokenSettings().getRefreshTokenTimeToLive());
    }

    private CustomizeRegisteredClientRepository repository() {
        OAuth2Properties properties = new OAuth2Properties();
        properties.getLogin().setAccessTokenTtl(CONFIG_ACCESS_TOKEN_TTL.toSeconds());
        properties.getLogin().setRefreshTokenTtl(CONFIG_REFRESH_TOKEN_TTL.toSeconds());
        return new CustomizeRegisteredClientRepository(clientService, properties);
    }

    private AppClientDTO newClientDTO(Oauth2TokenSettings tokenSettings) {
        LocalDateTime now = LocalDateTime.now();
        return new AppClientDTO()
            .setId(1L)
            .setAppKey(CLIENT_ID)
            .setAppName("Carlos Client")
            .setAppSecret("secret")
            .setClientIssuedAt(now)
            .setClientSecretExpiresAt(now.plusYears(10))
            .setAuthenticationMethods(Set.of("client_secret_basic"))
            .setAuthorizationGrantTypes(Set.of("authorization_code"))
            .setRedirectUris(Set.of("http://localhost:5173/login/oauth2/code/carlos"))
            .setScopes(Set.of("openid"))
            .setTokenSettings(tokenSettings);
    }
}
