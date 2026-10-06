package com.carlos.auth.oauth2.config;

import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.security.manager.KeyPairManager;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * JWT 密钥与编解码配置
 *
 * <p>管理 JWK 源、JWT 编码器和解码器。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(OAuth2Properties.class)
@ConditionalOnProperty(
        prefix = "carlos.auth.authorization-server",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OAuth2JwkConfig {

    private final OAuth2Properties oauth2Properties;
    private final Environment environment;

    /**
     * 判断当前是否为生产环境
     */
    private boolean isProductionProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.stream(activeProfiles)
                .anyMatch(profile -> profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"));
    }

    /**
     * JWK 源
     *
     * @return JWKSource JWT 密钥源
     */
    @Bean
    @ConditionalOnMissingBean
    public JWKSource<SecurityContext> jwkSource() {
        String keyStorePath = oauth2Properties.getJwt().getKeyStore();
        String keyStorePassword = oauth2Properties.getJwt().getKeyStorePassword();
        String keyAlias = oauth2Properties.getJwt().getKeyAlias();
        String keyPassword = oauth2Properties.getJwt().getKeyPassword();
        int keySize = oauth2Properties.getJwt().getKeySize() != null ? oauth2Properties.getJwt().getKeySize() : 2048;

        // 生产环境强制校验：必须配置 keystore 和 issuer
        if (isProductionProfile()) {
            if (keyStorePath == null || keyStorePath.isEmpty()) {
                throw new IllegalStateException(
                        "[carlos-auth] 生产环境必须配置 carlos.auth.jwt.key-store (JKS 密钥库路径)，禁止使用内存密钥对");
            }
            String issuer = oauth2Properties.getJwt().getIssuer();
            if (issuer == null || issuer.isEmpty()) {
                throw new IllegalStateException(
                        "[carlos-auth] 生产环境必须配置 carlos.auth.jwt.issuer (JWT 签发者域名)");
            }
        }

        KeyPair keyPair;
        if (keyStorePath != null && !keyStorePath.isEmpty()) {
            keyPair = KeyPairManager.loadOrGenerateKeyPair(keyStorePath, keyStorePassword, keyAlias, keyPassword, keySize);
            log.info("JWK Source initialized from JKS file: {}, key-id: {}", keyStorePath, oauth2Properties.getJwt().getKeyId());
        } else {
            log.warn("No keystore configured, generating in-memory RSA key pair (for development only)");
            keyPair = generateRsaKey();
        }

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(oauth2Properties.getJwt().getKeyId())
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);

        log.info("JWK Source initialized with key-id: {}", oauth2Properties.getJwt().getKeyId());

        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * 生成 RSA 密钥对
     *
     * @return KeyPair RSA 密钥对
     */
    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            log.error("Failed to generate RSA key pair", e);
            throw AuthErrorCode.AUTH_RSA_KEY_GENERATE_FAILED.exception(e.getMessage());
        }
    }

    /**
     * JWT 解码器
     *
     * <p>Spring Authorization Server 2.0 移除了 OAuth2AuthorizationServerConfiguration.jwtDecoder()，
     * 此处使用 NimbusJwtDecoder 自行构建。</p>
     *
     * @param jwkSource JWK 密钥源
     * @return JwtDecoder JWT 解码器
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        Set<JWSAlgorithm> jwsAlgs = new HashSet<>();
        jwsAlgs.addAll(JWSAlgorithm.Family.RSA);
        jwsAlgs.addAll(JWSAlgorithm.Family.EC);
        jwsAlgs.addAll(JWSAlgorithm.Family.HMAC_SHA);

        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        JWSKeySelector<SecurityContext> jwsKeySelector =
                new JWSVerificationKeySelector<>(jwsAlgs, jwkSource);
        jwtProcessor.setJWSKeySelector(jwsKeySelector);
        jwtProcessor.setJWTClaimsSetVerifier((claims, context) -> {
        });

        return new NimbusJwtDecoder(jwtProcessor);
    }

    /**
     * JWT 编码器
     *
     * @param jwkSource JWK 密钥源
     * @return JwtEncoder JWT 编码器
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }
}
