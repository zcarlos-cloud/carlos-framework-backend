package com.carlos.auth.mfa.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MFA 设置响应 VO
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MFA设置响应")
public class MfaSetupVO {

    @Schema(description = "MFA密钥")
    private String secret;

    @Schema(description = "QR码URL")
    private String qrCodeUrl;

    @Schema(description = "格式化后的密钥（便于手动输入）")
    private String formattedSecret;

    @Schema(description = "备用恢复码列表")
    private List<String> recoveryCodes;
}
