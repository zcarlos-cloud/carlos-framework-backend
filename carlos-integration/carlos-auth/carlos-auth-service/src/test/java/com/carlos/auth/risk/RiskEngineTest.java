package com.carlos.auth.risk;

import com.carlos.auth.security.manager.IpBlockManager;
import com.carlos.auth.security.manager.LoginAttemptManager;
import com.carlos.auth.util.IpLocationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 风控评分引擎单元测试
 *
 * <p>覆盖 {@link RiskEngine#quickAssess(String, String, String)} 的预校验决策。
 * 注意：评估结果含"异常时段"信号（22:00-06:00 加 10 分），故断言只使用与时钟无关的边界。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-09-18
 */
@ExtendWith(MockitoExtension.class)
class RiskEngineTest {

    private static final String CLIENT_IP = "10.0.0.1";

    private static final String USERNAME = "admin";

    private static final String DEVICE_FINGERPRINT = "fp_MWUxMjM0NTY3ODkwYWJjZGVm";

    @Mock
    private LoginAttemptManager loginAttemptManager;

    @Mock
    private IpBlockManager ipBlockManager;

    @Mock
    private IpLocationUtil ipLocationUtil;

    private RiskProperties riskProperties;

    private RiskEngine riskEngine;

    @BeforeEach
    void setUp() {
        riskProperties = new RiskProperties();
        riskEngine = new RiskEngine(riskProperties, loginAttemptManager, ipBlockManager, ipLocationUtil);
    }

    @Test
    @DisplayName("应该不因携带设备指纹而判定为新设备")
    void should_not_treat_present_device_fingerprint_as_new_device() {
        // given: 无历史失败、IP 未被封禁，前端照例携带设备指纹
        when(loginAttemptManager.getCurrentAttempts(USERNAME)).thenReturn(0);
        when(ipBlockManager.isIpBlocked(CLIENT_IP)).thenReturn(false);

        // when
        RiskAssessment assessment = riskEngine.quickAssess(CLIENT_IP, USERNAME, DEVICE_FINGERPRINT);

        // then: 设备指纹本身不得贡献任何分数（回归：旧实现把"指纹非空"当新设备，恒加 15 分）
        assertFalse(assessment.getHitSignals().contains("new_device"));
        assertTrue(assessment.getScore() < riskProperties.getSignals().getNewDevice());
        // 剩余得分只可能来自"异常时段"信号，不足以触发挑战
        assertEquals(RiskLevel.LOW, assessment.getLevel());
        assertNull(assessment.getChallengeType());
        assertFalse(assessment.isMfaRequired());
        assertFalse(assessment.isBlocked());
    }

    @Test
    @DisplayName("应该在高频失败后升级为 MFA 挑战")
    void should_escalate_to_mfa_when_recent_failures_high() {
        // given: 该账号近期失败 12 次（12 × 5 分 = 60 分，昼夜均达 HIGH 阈值）
        when(loginAttemptManager.getCurrentAttempts(USERNAME)).thenReturn(12);
        when(ipBlockManager.isIpBlocked(CLIENT_IP)).thenReturn(false);

        // when
        RiskAssessment assessment = riskEngine.quickAssess(CLIENT_IP, USERNAME, DEVICE_FINGERPRINT);

        // then
        assertTrue(assessment.getScore() >= riskProperties.getThresholds().getMedium());
        assertEquals(RiskLevel.HIGH, assessment.getLevel());
        assertTrue(assessment.isMfaRequired());
        assertEquals("mfa", assessment.getChallengeType());
        assertFalse(assessment.isBlocked());
    }

    @Test
    @DisplayName("应该在 IP 已被封禁时不再判定为低风险")
    void should_not_rate_low_risk_when_ip_blocked() {
        // given: 客户端 IP 命中封禁名单（+50 分）
        when(loginAttemptManager.getCurrentAttempts(USERNAME)).thenReturn(0);
        when(ipBlockManager.isIpBlocked(CLIENT_IP)).thenReturn(true);

        // when
        RiskAssessment assessment = riskEngine.quickAssess(CLIENT_IP, USERNAME, DEVICE_FINGERPRINT);

        // then: 至少 MEDIUM，必须下发挑战
        assertTrue(assessment.getHitSignals().contains("ip_blocked"));
        assertTrue(assessment.getScore() >= 50);
        assertNotEquals(RiskLevel.LOW, assessment.getLevel());
        assertNotNull(assessment.getChallengeType());
    }
}
