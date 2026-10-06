package com.carlos.auth.mfa.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MFA 状态响应 VO
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MFA状态响应")
public class MfaStatusVO {

    @Schema(description = "是否已启用MFA")
    private boolean enabled;

    @Schema(description = "MFA类型（TOTP/SMS/EMAIL）")
    private String mfaType;
}
