package com.carlos.auth.risk;

import lombok.Builder;
import lombok.Data;

/**
 * 风控评估结果
 *
 * <p>由 {@link RiskEngine} 评估后输出，包含风险等级、得分和建议的挑战类型。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
@Data
@Builder
public class RiskAssessment {

    /**
     * 风险等级
     */
    private RiskLevel level;

    /**
     * 风险得分（0-100+）
     */
    private int score;

    /**
     * 建议的验证码类型
     *
     * <ul>
     *   <li>{@code null} - 无需验证码（低风险）</li>
     *   <li>{@code "slider"} - 滑块验证码（中风险）</li>
     *   <li>{@code "behavior"} - 行为验证码/点选（中高风险）</li>
     *   <li>{@code "sms"} - 短信 OTP（高风险）</li>
     * </ul>
     */
    private String challengeType;

    /**
     * 是否需要 MFA
     */
    private boolean mfaRequired;

    /**
     * 是否阻断（拒绝登录）
     */
    private boolean blocked;

    /**
     * 阻断原因（仅 blocked=true 时有值）
     */
    private String blockReason;

    /**
     * 命中的信号摘要（用于审计日志）
     */
    private String hitSignals;
}
