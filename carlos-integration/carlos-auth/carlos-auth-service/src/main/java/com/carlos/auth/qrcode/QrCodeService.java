package com.carlos.auth.qrcode;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.json.JSONUtil;
import com.carlos.redis.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 扫码登录服务
 *
 * <p>管理扫码登录的完整生命周期：生成、扫码、确认、取消、过期。</p>
 *
 * <h3>Redis Key 设计：</h3>
 * <pre>
 * auth:qrcode:{qrcodeId} → QrCodeSession JSON (TTL = 120s)
 * </pre>
 *
 * <h3>安全设计：</h3>
 * <ul>
 *   <li>一次性：qrcodeId 全局唯一，使用后立即失效</li>
 *   <li>短 TTL：120 秒过期</li>
 *   <li>签名：HMAC-SHA256 防伪造</li>
 *   <li>APP 端确认需携带有效 Token，服务端校验 APP 用户身份</li>
 * </ul>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
@Slf4j
@Service
@EnableConfigurationProperties(QrCodeProperties.class)
public class QrCodeService {

    private static final String REDIS_PREFIX = "auth:qrcode:";
    private final QrCodeProperties properties;
    private final HMac hmac;

    public QrCodeService(QrCodeProperties properties) {
        this.properties = properties;
        this.hmac = new HMac(HmacAlgorithm.HmacSHA256,
            properties.getSignSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成扫码登录二维码
     *
     * @param clientId         客户端 ID
     * @param deviceFingerprint PC 端设备指纹（可选）
     * @return 扫码会话
     */
    public QrCodeSession generate(String clientId, String deviceFingerprint) {
        String qrcodeId = generateQrcodeId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(properties.getTtlSeconds());

        // 构建二维码 URL 并签名
        String qrcodeUrl = properties.getQrcodeBaseUrl() + qrcodeId;
        String signature = sign(qrcodeId, expiresAt.toString());

        QrCodeSession session = QrCodeSession.builder()
            .qrcodeId(qrcodeId)
            .state(QrCodeState.PENDING)
            .clientId(clientId)
            .deviceFingerprint(deviceFingerprint)
            .qrcodeUrl(qrcodeUrl)
            .signature(signature)
            .createdAt(now)
            .expiresAt(expiresAt)
            .updatedAt(now)
            .build();

        // 存入 Redis
        saveSession(session);

        log.info("QrCode generated: id={}, ttl={}s", qrcodeId, properties.getTtlSeconds());
        return session;
    }

    /**
     * APP 端扫码
     *
     * <p>将状态从 PENDING → SCANNED。</p>
     *
     * @param qrcodeId 二维码 ID
     * @param userId   扫码用户 ID
     * @param username 扫码用户名
     * @return 更新后的会话，null 表示操作失败
     */
    public QrCodeSession scan(String qrcodeId, Long userId, String username) {
        QrCodeSession session = getSession(qrcodeId);
        if (session == null) {
            log.warn("QrCode session not found: {}", qrcodeId);
            return null;
        }

        if (!session.getState().canTransitionTo(QrCodeState.SCANNED)) {
            log.warn("QrCode state transition denied: {} → SCANNED, current={}", qrcodeId, session.getState());
            return null;
        }

        session.setState(QrCodeState.SCANNED);
        session.setScannedUserId(userId);
        session.setScannedUsername(username);
        session.setUpdatedAt(LocalDateTime.now());

        saveSession(session);
        log.info("QrCode scanned: id={}, userId={}", qrcodeId, userId);
        return session;
    }

    /**
     * APP 端确认登录
     *
     * <p>将状态从 SCANNED → CONFIRMED。</p>
     *
     * @param qrcodeId 二维码 ID
     * @param userId   确认的用户 ID（必须与扫码用户一致）
     * @return 更新后的会话，null 表示操作失败
     */
    public QrCodeSession confirm(String qrcodeId, Long userId) {
        QrCodeSession session = getSession(qrcodeId);
        if (session == null) {
            log.warn("QrCode session not found: {}", qrcodeId);
            return null;
        }

        if (!session.getState().canTransitionTo(QrCodeState.CONFIRMED)) {
            log.warn("QrCode state transition denied: {} → CONFIRMED, current={}", qrcodeId, session.getState());
            return null;
        }

        // 校验确认用户与扫码用户一致
        if (!userId.equals(session.getScannedUserId())) {
            log.warn("QrCode confirm denied: userId mismatch, scanned={}, confirm={}",
                session.getScannedUserId(), userId);
            return null;
        }

        session.setState(QrCodeState.CONFIRMED);
        session.setUpdatedAt(LocalDateTime.now());

        saveSession(session);
        log.info("QrCode confirmed: id={}, userId={}", qrcodeId, userId);
        return session;
    }

    /**
     * APP 端取消登录
     *
     * <p>将状态从 PENDING/SCANNED → CANCELED。</p>
     *
     * @param qrcodeId 二维码 ID
     * @return 更新后的会话，null 表示操作失败
     */
    public QrCodeSession cancel(String qrcodeId) {
        QrCodeSession session = getSession(qrcodeId);
        if (session == null) {
            return null;
        }

        if (!session.getState().canTransitionTo(QrCodeState.CANCELED)) {
            log.warn("QrCode state transition denied: {} → CANCELED, current={}", qrcodeId, session.getState());
            return null;
        }

        session.setState(QrCodeState.CANCELED);
        session.setUpdatedAt(LocalDateTime.now());

        saveSession(session);
        log.info("QrCode canceled: id={}", qrcodeId);
        return session;
    }

    /**
     * 查询二维码当前状态
     *
     * @param qrcodeId 二维码 ID
     * @return 扫码会话，null 表示不存在或已过期
     */
    public QrCodeSession getSession(String qrcodeId) {
        if (qrcodeId == null || qrcodeId.isBlank()) {
            return null;
        }

        String key = REDIS_PREFIX + qrcodeId;
        String json = RedisUtil.getValue(key);
        if (json == null) {
            return null;
        }

        try {
            return JSONUtil.toBean(json, QrCodeSession.class);
        } catch (Exception e) {
            log.error("Failed to deserialize QrCodeSession: {}", qrcodeId, e);
            return null;
        }
    }

    /**
     * 保存会话到 Redis
     */
    private void saveSession(QrCodeSession session) {
        String key = REDIS_PREFIX + session.getQrcodeId();
        try {
            String json = JSONUtil.toJsonStr(session);
            // 计算剩余 TTL
            long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), session.getExpiresAt()).getSeconds();
            if (remainingSeconds <= 0) {
                remainingSeconds = 5; // 给 CONFIRMED/CANCELED 状态保留5秒供轮询
            }
            RedisUtil.setValue(key, json, remainingSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to serialize QrCodeSession: {}", session.getQrcodeId(), e);
        }
    }

    /**
     * 生成全局唯一的二维码 ID
     */
    private String generateQrcodeId() {
        return UUID.randomUUID().toString().replace("-", "")
            + cn.hutool.core.util.RandomUtil.randomString(8);
    }

    /**
     * HMAC-SHA256 签名
     */
    private String sign(String qrcodeId, String expiresAt) {
        String payload = qrcodeId + "|" + expiresAt;
        return hmac.digestHex(payload);
    }

    /**
     * 验证二维码签名
     *
     * @param qrcodeId  二维码 ID
     * @param expiresAt 过期时间字符串
     * @param signature 签名
     * @return true-签名有效
     */
    public boolean verifySignature(String qrcodeId, String expiresAt, String signature) {
        String expected = sign(qrcodeId, expiresAt);
        return expected.equals(signature);
    }
}
