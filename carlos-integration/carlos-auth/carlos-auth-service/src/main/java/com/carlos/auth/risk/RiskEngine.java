package com.carlos.auth.risk;

import com.carlos.auth.security.manager.IpBlockManager;
import com.carlos.auth.security.manager.LoginAttemptManager;
import com.carlos.auth.util.IpLocationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 风控评分引擎 v1
 *
 * <p>基于规则的实时风控决策引擎，评估登录请求的风险等级。</p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *   <li>同步决策链（< 50ms）：规则评分 → 输出风险等级</li>
 *   <li>渐进式挑战：无感 → 滑块 → MFA → 阻断</li>
 *   <li>可配置：阈值与信号加分通过 {@link RiskProperties} 外部化</li>
 * </ul>
 *
 * <h3>评分规则：</h3>
 * <p>各维度信号命中后累加分数，最终按阈值划分风险等级。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 * @see RiskContext
 * @see RiskAssessment
 * @see RiskProperties
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(RiskProperties.class)
public class RiskEngine {

    private final RiskProperties riskProperties;
    private final LoginAttemptManager loginAttemptManager;
    private final IpBlockManager ipBlockManager;
    private final IpLocationUtil ipLocationUtil;

    /**
     * 评估登录风险
     *
     * @param context 风控上下文（聚合多维信号）
     * @return 风控评估结果
     */
    public RiskAssessment assess(RiskContext context) {
        if (!riskProperties.isEnabled()) {
            return RiskAssessment.builder()
                .level(RiskLevel.LOW)
                .score(0)
                .mfaRequired(false)
                .blocked(false)
                .build();
        }

        int score = 0;
        List<String> hitSignals = new ArrayList<>();
        RiskProperties.Signals signals = riskProperties.getSignals();

        // 1. 设备维度
        if (context.isNewDevice()) {
            score += signals.getNewDevice();
            hitSignals.add("new_device");
        }

        // 2. 网络维度
        if (context.isProxy()) {
            score += signals.getProxyDetected();
            hitSignals.add("proxy");
        }
        if (context.isDataCenterIp()) {
            score += signals.getDatacenterIp();
            hitSignals.add("datacenter_ip");
        }

        // 3. 地理位置
        if (context.isUnusualLocation()) {
            score += signals.getUnusualLocation();
            hitSignals.add("unusual_location");
        }
        if (context.getTravelSpeed() > 1000) {
            score += signals.getImpossibleTravel();
            hitSignals.add("impossible_travel");
        }

        // 4. 时间维度
        if (context.isUnusualTime() || isUnusualTimeNow()) {
            score += signals.getUnusualTime();
            hitSignals.add("unusual_time");
        }

        // 5. 频次维度
        int failCount = context.getRecentFailCount();
        if (failCount > 0) {
            score += failCount * signals.getRecentFailPerCount();
            hitSignals.add("recent_fails:" + failCount);
        }

        // 6. 账号维度
        if (context.getAccountAgeDays() > 0 && context.getAccountAgeDays() < 7) {
            score += signals.getNewAccount();
            hitSignals.add("new_account");
        }

        // IP 已在封禁列表
        if (context.getClientIp() != null && ipBlockManager.isIpBlocked(context.getClientIp())) {
            score += 50;
            hitSignals.add("ip_blocked");
        }

        // 评分结果
        RiskProperties.Thresholds thresholds = riskProperties.getThresholds();
        RiskLevel level = calculateLevel(score, thresholds);

        RiskAssessment assessment = RiskAssessment.builder()
            .level(level)
            .score(score)
            .challengeType(determineChallengeType(level))
            .mfaRequired(level == RiskLevel.HIGH)
            .blocked(level == RiskLevel.CRITICAL)
            .blockReason(level == RiskLevel.CRITICAL ? "风险评分过高，拒绝登录" : null)
            .hitSignals(String.join(",", hitSignals))
            .build();

        if (log.isDebugEnabled()) {
            log.debug("Risk assessment for {}: score={}, level={}, signals=[{}]",
                context.getUsername(), score, level, assessment.getHitSignals());
        }

        return assessment;
    }

    /**
     * 快速评估（仅用于 pre-check，不访问数据库）
     *
     * @param clientIp        客户端 IP
     * @param username        用户名（可为 null）
     * @param deviceFingerprint 设备指纹（可为 null）
     * @return 风控评估结果
     */
    public RiskAssessment quickAssess(String clientIp, String username, String deviceFingerprint) {
        int failCount = 0;
        if (username != null && !username.isBlank()) {
            failCount = loginAttemptManager.getCurrentAttempts(username);
        }

        // 设备指纹非空 ≠ 新设备：判定"新设备"必须与已知设备库比对，而当前尚无该数据源。
        // 此处曾以 "指纹非空" 作为 newDevice，导致每次预校验恒加 15 分（前端每次都会带指纹），
        // 风险等级被系统性高估，MEDIUM 被误触发。在设备库落地前不臆断该信号。
        RiskContext context = RiskContext.builder()
            .clientIp(clientIp)
            .username(username)
            .deviceFingerprint(deviceFingerprint)
            .recentFailCount(failCount)
            .unusualTime(isUnusualTimeNow())
            .newDevice(false)
            .build();

        return assess(context);
    }

    /**
     * 判断当前是否异常时段
     */
    private boolean isUnusualTimeNow() {
        int hour = LocalTime.now().getHour();
        return hour >= 22 || hour < 6;
    }

    /**
     * 根据得分计算风险等级
     */
    private RiskLevel calculateLevel(int score, RiskProperties.Thresholds thresholds) {
        if (score >= thresholds.getHigh()) {
            return RiskLevel.CRITICAL;
        }
        if (score >= thresholds.getMedium()) {
            return RiskLevel.HIGH;
        }
        if (score >= thresholds.getLow()) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    /**
     * 根据风险等级确定挑战类型
     */
    private String determineChallengeType(RiskLevel level) {
        return switch (level) {
            case LOW -> null;
            case MEDIUM -> "slider";
            case HIGH -> "mfa";
            case CRITICAL -> "blocked";
        };
    }
}
