package com.carlos.auth.risk;

/**
 * 风控风险等级
 *
 * <p>基于 NIST SP 800-63B AAL 分层设计，风控引擎根据多维信号评分后
 * 输出风险等级，决定后续挑战强度。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
public enum RiskLevel {

    /**
     * 低风险：无感通过，不需验证码，AAL1 即可
     */
    LOW,

    /**
     * 中风险：需要滑块/行为验证码，维持当前 AAL
     */
    MEDIUM,

    /**
     * 高风险：强制 MFA step-up 到 AAL2+
     */
    HIGH,

    /**
     * 极高风险：阻断，拒绝登录，触发安全告警
     */
    CRITICAL
}
