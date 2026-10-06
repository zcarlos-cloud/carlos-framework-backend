package com.carlos.auth.passkey.config;

import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.passkey.PasskeyCredentialRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;

/**
 * <p>
 * Passkey / WebAuthn 自动配置
 * </p>
 *
 * <p>当 {@code carlos.auth.login.methods.passkey=true} 时启用，构造 Yubico RelyingParty Bean。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "carlos.auth.login.methods", name = "passkey", havingValue = "true")
public class PasskeyAutoConfiguration {

    @Bean
    public RelyingParty relyingParty(OAuth2Properties properties,
                                     PasskeyCredentialRepository credentialRepository) {
        OAuth2Properties.PasskeyConfig config = properties.getLogin().getMethods().getPasskeyConfig();
        if (config == null) {
            throw new IllegalStateException("Passkey 已启用，但 passkey-config 未配置");
        }
        if (config.getRpId() == null || config.getRpId().isBlank()) {
            throw new IllegalStateException("Passkey 已启用，但 rpId 未配置");
        }
        if (config.getOrigins() == null || config.getOrigins().isEmpty()) {
            throw new IllegalStateException("Passkey 已启用，但 origins 未配置");
        }

        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id(config.getRpId())
                .name(config.getRpName())
                .build();

        return RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(credentialRepository)
                .origins(new HashSet<>(config.getOrigins()))
                .allowOriginPort(false)
                .allowOriginSubdomain(false)
                .allowUntrustedAttestation(true)
                .validateSignatureCounter(true)
                .build();
    }

}
