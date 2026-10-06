package com.carlos.auth.qrcode;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扫码登录会话
 *
 * <p>存储在 Redis 中，包含二维码状态、扫码用户、客户端信息等。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
@Data
@Builder
public class QrCodeSession {

    /**
     * 二维码 ID（全局唯一）
     */
    private String qrcodeId;

    /**
     * 当前状态
     */
    private QrCodeState state;

    /**
     * 客户端 ID（PC 端生成二维码时传入）
     */
    private String clientId;

    /**
     * PC 端设备指纹
     */
    private String deviceFingerprint;

    /**
     * 二维码内容 URL（用于 APP 扫码）
     */
    private String qrcodeUrl;

    /**
     * HMAC 签名（防伪造）
     */
    private String signature;

    /**
     * 扫码用户 ID（APP 扫码后填充）
     */
    private Long scannedUserId;

    /**
     * 扫码用户名（APP 扫码后填充）
     */
    private String scannedUsername;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
}
