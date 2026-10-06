package com.carlos.auth.risk;

import lombok.Builder;
import lombok.Data;

/**
 * 风控上下文
 *
 * <p>聚合登录请求中的多维信号，供 {@link RiskEngine} 评分。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
@Data
@Builder
public class RiskContext {

    // ========== 设备维度 ==========

    /**
     * 设备指纹（前端采集：Canvas + WebGL + 字体 + 分辨率 哈希）
     */
    private String deviceFingerprint;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 是否首次出现的设备
     */
    private boolean newDevice;

    // ========== 网络维度 ==========

    /**
     * 客户端 IP
     */
    private String clientIp;

    /**
     * IP 地理位置（省市区）
     */
    private String ipLocation;

    /**
     * 是否检测到代理/VPN/Tor
     */
    private boolean proxy;

    /**
     * 是否数据中心 IP
     */
    private boolean dataCenterIp;

    // ========== 行为维度 ==========

    /**
     * 近期失败次数（5min 内）
     */
    private int recentFailCount;

    /**
     * 是否异常时段（22:00-06:00）
     */
    private boolean unusualTime;

    /**
     * 是否异常地理位置（距历史常用地 > 500km）
     */
    private boolean unusualLocation;

    /**
     * 与上次登录的"旅行速度"(km/h)，> 1000 为不可能旅行
     */
    private double travelSpeed;

    // ========== 账号维度 ==========

    /**
     * 用户 ID（已注册用户）
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 是否启用 MFA
     */
    private boolean mfaEnabled;

    /**
     * 账龄（天）
     */
    private int accountAgeDays;

    /**
     * 上次登录 IP
     */
    private String lastLoginIp;

    /**
     * 上次登录地理位置
     */
    private String lastLoginLocation;
}
