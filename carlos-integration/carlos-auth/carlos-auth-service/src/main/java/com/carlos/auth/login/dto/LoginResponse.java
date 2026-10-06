package com.carlos.auth.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 登录响应
 * </p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {

    /**
     * 访问令牌
     */
    @Schema(description = "访问令牌", example = "eyJhbGciOiJSUzI1NiJ9...")
    private String accessToken;

    /**
     * 刷新令牌
     */
    @Schema(description = "刷新令牌", example = "eyJhbGciOiJSUzI1NiJ9...")
    private String refreshToken;

    /**
     * 令牌类型
     */
    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType;

    /**
     * 过期时间（秒）
     */
    @Schema(description = "过期时间（秒）", example = "7200")
    private Long expiresIn;

    /**
     * 用户信息
     */
    @Schema(description = "用户信息")
    private UserInfo userInfo;

    /**
     * 是否需要MFA验证
     */
    @Schema(description = "是否需要MFA验证", example = "false")
    private Boolean mfaRequired;

    /**
     * 是否建议启用MFA（用于未启用MFA的用户）
     */
    @Schema(description = "是否建议启用MFA", example = "false")
    private Boolean mfaRecommended;

    /**
     * MFA 临时令牌（仅 mfaRequired=true 时返回）
     *
     * <p>前端携带此令牌 + MFA code 调用 /auth/mfa/step-up-verify 完成第二因素验证。
     * 5 分钟有效，一次性使用。</p>
     */
    @Schema(description = "MFA 临时令牌（需要 MFA 时返回）", example = "abc123def456")
    private String mfaToken;

    /**
     * 用户信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户信息")
    public static class UserInfo {

        /**
         * 用户ID
         */
        @Schema(description = "用户ID", example = "1")
        private Long id;

        /**
         * 用户名
         */
        @Schema(description = "用户名", example = "admin")
        private String username;

        /**
         * 邮箱
         */
        @Schema(description = "邮箱", example = "admin@carlos.com")
        private String email;

        /**
         * 手机号
         */
        @Schema(description = "手机号", example = "13800000000")
        private String phone;
    }
}
