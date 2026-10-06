package com.carlos.auth.passkey;

import com.carlos.auth.passkey.manager.PasskeyCredentialManager;
import com.carlos.auth.passkey.pojo.entity.PasskeyCredential;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.exception.Base64UrlException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Yubico WebAuthn CredentialRepository 实现
 * </p>
 *
 * <p>负责为 RelyingParty 提供凭据查询能力。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasskeyCredentialRepository implements CredentialRepository {

    private final PasskeyCredentialManager credentialManager;

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        Long userId = parseUserId(username);
        if (userId == null) {
            return Set.of();
        }
        return credentialManager.listByUserId(userId).stream()
                .map(cred -> PublicKeyCredentialDescriptor.builder()
                        .id(byteArrayFromB64(cred.getCredentialIdB64()))
                        .build())
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        Long userId = parseUserId(username);
        if (userId == null) {
            return Optional.empty();
        }
        return credentialManager.listByUserId(userId).stream()
                .findFirst()
                .map(cred -> byteArrayFromB64(cred.getUserHandleB64()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        PasskeyCredential credential =
                credentialManager.getByUserHandleB64(userHandle.getBase64Url());
        if (credential == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(credential.getUserId()).map(String::valueOf);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        PasskeyCredential credential =
                credentialManager.getByCredentialIdB64(credentialId.getBase64Url());
        if (credential == null) {
            return Optional.empty();
        }
        return Optional.of(toRegisteredCredential(credential));
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        PasskeyCredential credential =
                credentialManager.getByCredentialIdB64(credentialId.getBase64Url());
        if (credential == null) {
            return Set.of();
        }
        return Set.of(toRegisteredCredential(credential));
    }

    private RegisteredCredential toRegisteredCredential(PasskeyCredential credential) {
        return RegisteredCredential.builder()
                .credentialId(byteArrayFromB64(credential.getCredentialIdB64()))
                .userHandle(byteArrayFromB64(credential.getUserHandleB64()))
                .publicKeyCose(new ByteArray(credential.getPublicKeyCose()))
                .signatureCount(credential.getSignCount())
                .build();
    }

    private static ByteArray byteArrayFromB64(String base64Url) {
        try {
            return ByteArray.fromBase64Url(base64Url);
        } catch (Base64UrlException e) {
            throw new RuntimeException("Invalid base64url: " + base64Url, e);
        }
    }

    private Long parseUserId(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(username);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
