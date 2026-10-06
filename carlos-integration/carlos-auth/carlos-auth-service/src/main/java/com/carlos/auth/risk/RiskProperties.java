package com.carlos.auth.risk;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 风控引擎配置属性
 *
 * <p>配置前缀：{@code carlos.auth.risk}</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
@Data
@ConfigurationProperties(prefix = "carlos.auth.risk")
public class RiskProperties {

    /**
     * 是否启用风控引擎
     */
    private boolean enabled = true;

    /**
     * 风控评分阈值
     */
    private Thresholds thresholds = new Thresholds();

    /**
     * 各信号加分规则
     */
    private Signals signals = new Signals();

    @Data
    public static class Thresholds {
        /**
         * 低风险上界（得分 < 此值无感通过）
         */
        private int low = 30;

        /**
         * 中风险上界（得分 < 此值弹滑块验证码）
         */
        private int medium = 60;

        /**
         * 高风险上界（得分 < 此值强制 MFA）
         */
        private int high = 85;

        // >= high 拒绝登录 + 告警
    }

    @Data
    public static class Signals {
        private int newDevice = 15;
        private int unusualLocation = 20;
        private int impossibleTravel = 30;
        private int proxyDetected = 10;
        private int datacenterIp = 20;
        private int unusualTime = 10;
        private int recentFailPerCount = 5;
        private int ipReputationBad = 15;
        private int newAccount = 10;
    }
}
