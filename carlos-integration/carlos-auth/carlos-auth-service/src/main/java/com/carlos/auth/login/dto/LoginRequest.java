package com.carlos.auth.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * <p>
 * 登录请求
 * </p>
 *
 * <p>支持用户名、邮箱或手机号登录</p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest {

    /**
     * 用户名、邮箱或手机号（三选一）
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过64")
    @Schema(description = "用户名、邮箱或手机号", example = "admin")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(max = 512, message = "密码长度不能超过512")
    @Schema(description = "密码", example = "admin123")
    private String password;

    /**
     * 验证码（可选，用于安全验证）
     */
    @Size(max = 32, message = "验证码长度不能超过32")
    @Schema(description = "验证码（可选）", example = "123456")
    private String captcha;

    /**
     * 客户端ID（可选）
     */
    @Size(max = 64, message = "客户端ID长度不能超过64")
    @Schema(description = "客户端ID（可选）", example = "carlos-client")
    private String clientId;

    /**
     * 密码是否已加密（SM2 前端加密标识）
     *
     * <p>当设置为 true 时，表示 password 字段已使用 SM2 公钥加密，
     * 后端需要使用 SM2 私钥解密后再进行密码校验。</p>
     */
    @Schema(description = "密码是否已使用 SM2 加密", example = "false")
    private Boolean encrypted;

    /**
     * 加密密钥 ID（动态公钥下发时使用）
     *
     * <p>前端从 /auth/public-key 获取公钥时得到的 keyId，
     * 后端根据此 keyId 从 Redis 中取出对应私钥解密密码。</p>
     */
    @Size(max = 64, message = "加密密钥ID长度不能超过64")
    @Schema(description = "加密密钥 ID", example = "a1b2c3d4e5f6...")
    private String keyId;

    /**
     * 租户编码（可选）
     *
     * <p>仅当 {@code carlos.auth.tenant.enabled=true} 时生效；
     * 不传则使用默认租户（平台租户 0）。</p>
     */
    @Size(max = 64, message = "租户编码长度不能超过64")
    @Schema(description = "租户编码（可选，需开启租户登录校验）", example = "PLATFORM")
    private String tenantCode;
}
