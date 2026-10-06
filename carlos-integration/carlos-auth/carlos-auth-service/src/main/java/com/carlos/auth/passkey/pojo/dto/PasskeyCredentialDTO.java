package com.carlos.auth.passkey.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * Passkey 凭据 DTO
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
public class PasskeyCredentialDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String credentialIdB64;

    private Long signCount;

    private String rpId;

    private String userHandleB64;

    private String aaguid;

    private String attestationType;

    private String deviceName;

    private String transports;

    private Integer discoverable;

    private Integer backupEligible;

    private Integer backupState;

    private LocalDateTime lastUsedTime;

    private LocalDateTime createTime;

}
