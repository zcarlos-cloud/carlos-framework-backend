package com.carlos.auth.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录预校验请求
 *
 * <p>前端在展示登录页或提交登录前调用，用于获取风控决策与验证码类型。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
@Data
@Schema(description = "登录预校验请求")
public class PreCheckRequest {

    /**
     * 用户名/手机号/邮箱（可为空，用于无账号维度的纯 IP 风控）
     */
    @Size(max = 64, message = "用户名长度不能超过64")
    @Schema(description = "用户名/手机号/邮箱（可选）", example = "admin")
    private String username;

    /**
     * 设备指纹（前端采集）
     */
    @Size(max = 128, message = "设备指纹长度不能超过128")
    @Schema(description = "设备指纹（前端采集）", example = "d_abc123")
    private String deviceFingerprint;

    /**
     * 登录方式标识（用于风控决策上下文）
     */
    @NotBlank(message = "登录方式不能为空")
    @Size(max = 32, message = "登录方式长度不能超过32")
    @Schema(description = "登录方式：password/sms/qr_code/passkey", example = "password")
    private String loginType;
}
