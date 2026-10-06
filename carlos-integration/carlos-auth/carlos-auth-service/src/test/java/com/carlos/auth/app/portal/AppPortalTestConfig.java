package com.carlos.auth.app.portal;

import com.carlos.audit.api.ApiAuditLogMain;
import com.carlos.audit.api.ApiAuditLogQuery;
import com.carlos.auth.audit.AuditLogPublisher;
import com.carlos.auth.app.manager.AppCategoryManager;
import com.carlos.auth.app.manager.AppClientManager;
import com.carlos.auth.app.manager.AppPortalManager;
import com.carlos.auth.app.manager.AppRoleAccessManager;
import com.carlos.auth.app.pojo.dto.AppClientDTO;
import com.carlos.auth.app.pojo.dto.Oauth2ClientSettings;
import com.carlos.auth.app.service.AppClientService;
import com.carlos.auth.audit.manager.SecurityAlertManager;
import com.carlos.auth.audit.service.SecurityAlertService;
import com.carlos.auth.idp.IdentityProviderRegistry;
import com.carlos.auth.idp.RoleProvider;
import com.carlos.auth.login.UserLoginService;
import com.carlos.auth.mfa.manager.MfaManager;
import com.carlos.auth.oauth2.repository.RedisOAuth2AuthorizationConsentService;
import com.carlos.auth.oauth2.repository.RedisOAuth2AuthorizationService;
import com.carlos.auth.provider.DefaultUserProvider;
import com.carlos.auth.provider.UserProvider;
import com.carlos.auth.qrcode.QrCodeProperties;
import com.carlos.auth.security.manager.IpBlockManager;
import com.carlos.auth.security.manager.LoginAttemptManager;
import com.carlos.auth.util.IpLocationUtil;
import com.carlos.org.api.ApiOrgTenant;
import com.carlos.org.api.ApiOrgUser;
import com.carlos.org.api.ApiOrgUserRole;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;

/**
 * 应用门户测试专用 Bean 覆盖配置。
 * 将 Redis 依赖的授权服务替换为内存版本，Mock 数据库/外部服务依赖。
 */
@TestConfiguration
public class AppPortalTestConfig {

    /**
     * 使用内存版 OAuth2 授权服务，替代 Redis 版本
     */
    @Bean
    @Primary
    public RedisOAuth2AuthorizationService authorizationService() {
        return new RedisOAuth2AuthorizationService() {
            private final InMemoryOAuth2AuthorizationService delegate = new InMemoryOAuth2AuthorizationService();

            @Override
            public void save(org.springframework.security.oauth2.server.authorization.OAuth2Authorization authorization) {
                delegate.save(authorization);
            }

            @Override
            public void remove(org.springframework.security.oauth2.server.authorization.OAuth2Authorization authorization) {
                delegate.remove(authorization);
            }

            @Override
            public org.springframework.security.oauth2.server.authorization.OAuth2Authorization findById(String id) {
                return delegate.findById(id);
            }

            @Override
            public org.springframework.security.oauth2.server.authorization.OAuth2Authorization findByToken(String token, org.springframework.security.oauth2.server.authorization.OAuth2TokenType tokenType) {
                return delegate.findByToken(token, tokenType);
            }
        };
    }

    /**
     * 使用内存版 OAuth2 授权确认服务，替代 Redis 版本
     */
    @Bean
    @Primary
    public RedisOAuth2AuthorizationConsentService authorizationConsentService() {
        return new RedisOAuth2AuthorizationConsentService() {
            private final InMemoryOAuth2AuthorizationConsentService delegate = new InMemoryOAuth2AuthorizationConsentService();

            @Override
            public void save(org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent authorizationConsent) {
                delegate.save(authorizationConsent);
            }

            @Override
            public void remove(org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent authorizationConsent) {
                delegate.remove(authorizationConsent);
            }

            @Override
            public org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
                return delegate.findById(registeredClientId, principalName);
            }
        };
    }

    private static final KeyPair TEST_KEY_PAIR;

    static {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            TEST_KEY_PAIR = keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    @Bean
    @Primary
    public OAuth2TokenGenerator<? extends OAuth2Token> oAuth2TokenGenerator() {
        // 标准 JWT Token 生成器（用于 authorization_code 等标准流程）
        JwtGenerator jwtGenerator = new JwtGenerator(
                new NimbusJwtEncoder(jwkSource())
        );

        // 标准 Access Token 生成器（备用）
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();

        // Refresh Token 生成器
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();

        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator, accessTokenGenerator, refreshTokenGenerator
        );
    }

    @Bean
    @Primary
    public JWKSource<SecurityContext> jwkSource() {
        RSAPublicKey publicKey = (RSAPublicKey) TEST_KEY_PAIR.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) TEST_KEY_PAIR.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("test-key")
                .build();

        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    @Primary
    public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
        Set<JWSAlgorithm> jwsAlgs = new HashSet<>();
        jwsAlgs.addAll(JWSAlgorithm.Family.RSA);
        jwsAlgs.addAll(JWSAlgorithm.Family.EC);
        jwsAlgs.addAll(JWSAlgorithm.Family.HMAC_SHA);

        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        JWSKeySelector<SecurityContext> jwsKeySelector =
                new JWSVerificationKeySelector<>(jwsAlgs, jwkSource());
        jwtProcessor.setJWSKeySelector(jwsKeySelector);
        jwtProcessor.setJWTClaimsSetVerifier((claims, context) -> {
        });

        return new org.springframework.security.oauth2.jwt.NimbusJwtDecoder(jwtProcessor);
    }

    @Bean
    @Primary
    public AppPortalManager appPortalManager() {
        return Mockito.mock(AppPortalManager.class);
    }

    @Bean
    @Primary
    public AppCategoryManager appCategoryManager() {
        return Mockito.mock(AppCategoryManager.class);
    }

    @Bean
    @Primary
    public AppRoleAccessManager appRoleAccessManager() {
        return Mockito.mock(AppRoleAccessManager.class);
    }

    @Bean
    @Primary
    public AppClientManager appClientManager() {
        return Mockito.mock(AppClientManager.class);
    }

    @Bean
    @Primary
    public AppClientService appClientService() {
        AppClientService service = Mockito.mock(AppClientService.class);
        // OAuth2 流程按 client_id 反查客户端：返回门户公共客户端（PKCE）夹具
        AppClientDTO portal = new AppClientDTO();
        portal.setId(1L);
        portal.setAppKey("carlos-framework-portal");
        portal.setAppName("Carlos Portal");
        portal.setAppSecret(null);
        portal.setClientSecretExpiresAt(LocalDateTime.now().plusYears(1));
        portal.setClientIssuedAt(LocalDateTime.now().minusDays(1));
        portal.setAuthenticationMethods(Set.of(ClientAuthenticationMethod.NONE.getValue()));
        portal.setAuthorizationGrantTypes(Set.of(AuthorizationGrantType.AUTHORIZATION_CODE.getValue(),
                AuthorizationGrantType.REFRESH_TOKEN.getValue()));
        portal.setScopes(Set.of("openid", "profile"));
        portal.setRedirectUris(Set.of("http://localhost:5200/oauth2/callback"));
        Oauth2ClientSettings clientSettings = new Oauth2ClientSettings();
        clientSettings.setRequireAuthorizationConsent(false);
        clientSettings.setRequireProofKey(true);
        portal.setClientSettings(clientSettings);
        Mockito.when(service.findByAppkey("carlos-framework-portal", false)).thenReturn(portal);
        Mockito.when(service.findByAppkeyOrNull("carlos-framework-portal")).thenReturn(portal);
        Mockito.when(service.findById("1", false)).thenReturn(portal);
        return service;
    }

    @Bean
    @Primary
    public SecurityAlertManager securityAlertManager() {
        return Mockito.mock(SecurityAlertManager.class);
    }

    @Bean
    @Primary
    public SecurityAlertService securityAlertService() {
        return Mockito.mock(SecurityAlertService.class);
    }

    @Bean
    @Primary
    public MfaManager mfaManager() {
        return Mockito.mock(MfaManager.class);
    }

    @Bean
    @Primary
    public UserProvider userProvider() {
        return new DefaultUserProvider(passwordEncoder());
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ======================== Mock 审计服务 ========================

    @Bean
    @Primary
    public ApiAuditLogMain apiAuditLogMain() {
        return Mockito.mock(ApiAuditLogMain.class);
    }

    @Bean
    @Primary
    public ApiAuditLogQuery apiAuditLogQuery() {
        return Mockito.mock(ApiAuditLogQuery.class);
    }

    /**
     * qrcode 包被测试应用排除扫描，{@code LoginMethodValidator} 依赖其配置类
     */
    @Bean
    @Primary
    public QrCodeProperties qrCodeProperties() {
        return new QrCodeProperties();
    }

    /**
     * 审计投递点用 Mock 替代（{@code @Primary}）
     * <p>
     * 审计属外部依赖，测试不应真的走 MQ 投递。保留上面的 {@code ApiAuditLogMain} Mock
     * 是因为真实 {@code AuditLogPublisher} 的 Feign 回退路径需要它。
     * </p>
     */
    @Bean
    @Primary
    public AuditLogPublisher auditLogPublisher() {
        return Mockito.mock(AuditLogPublisher.class);
    }

    @Bean
    @Primary
    public ApiOrgUser apiOrgUser() {
        return Mockito.mock(ApiOrgUser.class);
    }

    @Bean
    @Primary
    public ApiOrgTenant apiOrgTenant() {
        // S2 租户管理：TenantLoginChecker 依赖（默认 carlos.auth.tenant.enabled=false，不会被调用）
        return Mockito.mock(ApiOrgTenant.class);
    }

    @Bean
    @Primary
    public ApiOrgUserRole apiOrgUserRole() {
        return Mockito.mock(ApiOrgUserRole.class);
    }

    @Bean
    @Primary
    public IdentityProviderRegistry identityProviderRegistry() {
        return Mockito.mock(IdentityProviderRegistry.class);
    }

    @Bean
    @Primary
    public RoleProvider roleProvider() {
        return Mockito.mock(RoleProvider.class);
    }

    @Bean
    @Primary
    public UserLoginService userLoginService() {
        return Mockito.mock(UserLoginService.class);
    }

    // ======================== Mock Redis 连接（测试无 Redis 服务器） ========================

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory() {
        return mock(LettuceConnectionFactory.class);
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        return mock(RedissonClient.class);
    }

    // ======================== Mock 风控/安全组件（无 Redis/DB 时无法工作） ========================

    @Bean
    @Primary
    public IpLocationUtil ipLocationUtil() {
        return Mockito.mock(IpLocationUtil.class);
    }

    @Bean
    @Primary
    public IpBlockManager ipBlockManager() {
        return Mockito.mock(IpBlockManager.class);
    }

    @Bean
    @Primary
    public LoginAttemptManager loginAttemptManager() {
        return Mockito.mock(LoginAttemptManager.class);
    }
}
