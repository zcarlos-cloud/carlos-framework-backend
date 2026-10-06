package com.carlos.auth.mfa.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * MFA Step-Up 验证参数
 *
 * <p>登录第二因素验证：密码认证通过后，前端携带 mfaToken + MFA code 完成验证。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
@Data
@Schema(description = "MFA Step-Up 验证参数")
public class MfaStepUpVerifyParam {

    /**
     * MFA 临时令牌（密码认证通过后签发，5min 有效）
     */
    @NotBlank(message = "MFA 令牌不能为空")
    @Size(max = 512, message = "MFA 令牌长度不能超过512")
    @Schema(description = "MFA 临时令牌", example = "abc123def456")
    private String mfaToken;

    /**
     * MFA 验证码（TOTP 6位数字 或 恢复码）
     */
    @NotBlank(message = "验证码不能为空")
    @Size(max = 64, message = "验证码长度不能超过64")
    @Schema(description = "MFA 验证码（TOTP 或恢复码）", example = "123456")
    private String code;

    /**
     * 设备指纹（可选，用于验证后标记为受信任设备）
     */
    @Size(max = 128, message = "设备指纹长度不能超过128")
    @Schema(description = "设备指纹（可选）", example = "d_abc123")
    private String deviceFingerprint;

    /**
     * 设备名称（可选，如 "Chrome on Windows"）
     */
    @Size(max = 128, message = "设备名称长度不能超过128")
    @Schema(description = "设备名称（可选）", example = "Chrome on Windows 11")
    private String deviceName;

    /**
     * 是否标记为受信任设备（7天内免 MFA）
     */
    @Schema(description = "是否标记为受信任设备", example = "true")
    private Boolean trustDevice;
}
