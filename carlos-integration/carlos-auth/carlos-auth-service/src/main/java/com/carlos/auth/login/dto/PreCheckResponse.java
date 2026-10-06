package com.carlos.auth.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 登录预校验响应
 *
 * <p>返回风控决策结果，指导前端展示验证码、MFA 等挑战。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
@Data
@Builder
@Schema(description = "登录预校验响应")
public class PreCheckResponse {

    /**
     * 风险等级：LOW/MEDIUM/HIGH/CRITICAL
     */
    @Schema(description = "风险等级", example = "LOW")
    private String riskLevel;

    /**
     * 是否需要验证码
     */
    @Schema(description = "是否需要验证码", example = "false")
    private Boolean needCaptcha;

    /**
     * 验证码类型：null/slider/behavior
     */
    @Schema(description = "验证码类型（无需时为 null）", example = "slider")
    private String captchaType;

    /**
     * 是否需要 MFA
     */
    @Schema(description = "是否需要 MFA", example = "false")
    private Boolean mfaRequired;

    /**
     * 是否阻断（拒绝登录）
     */
    @Schema(description = "是否阻断登录", example = "false")
    private Boolean blocked;

    /**
     * 阻断原因（blocked=true 时有值）
     */
    @Schema(description = "阻断原因", example = "风险评分过高")
    private String blockReason;

    /**
     * 是否建议启用 MFA（未启用 MFA 的用户，高风险时建议）
     */
    @Schema(description = "是否建议启用 MFA", example = "false")
    private Boolean mfaRecommended;
}
