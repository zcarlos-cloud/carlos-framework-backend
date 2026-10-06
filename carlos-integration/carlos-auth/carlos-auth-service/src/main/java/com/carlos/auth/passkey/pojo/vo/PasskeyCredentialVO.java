package com.carlos.auth.passkey.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * Passkey 凭据 VO
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
@Schema(description = "Passkey 凭据信息")
public class PasskeyCredentialVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "凭据ID")
    private String credentialIdB64;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "证明类型")
    private String attestationType;

    @Schema(description = "最后使用时间")
    private LocalDateTime lastUsedTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
