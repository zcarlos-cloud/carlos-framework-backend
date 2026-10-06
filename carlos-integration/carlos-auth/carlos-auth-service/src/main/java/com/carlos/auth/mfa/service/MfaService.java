package com.carlos.auth.mfa.service;

import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.auth.mfa.manager.MfaManager;
import com.carlos.auth.mfa.pojo.dto.MfaSetupDTO;
import com.carlos.auth.mfa.pojo.entity.TrustedDevice;
import com.carlos.auth.provider.UserInfo;
import com.carlos.auth.provider.UserProvider;
import com.carlos.auth.util.QrCodeGenerator;
import com.carlos.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MFA 业务服务
 *
 * <p>处理多因素认证相关业务逻辑，包括 TOTP 生成、验证、启用/禁用、
 * MFA step-up 临时令牌签发与验证、受信任设备管理。</p>
 *
 * <h3>MFA Step-Up 流程：</h3>
 * <ol>
 *   <li>用户密码认证成功后，风控/策略判定需要 MFA</li>
 *   <li>签发 mfaToken（Redis，5min TTL），返回前端</li>
 *   <li>前端携带 mfaToken + TOTP code 调用 /auth/mfa/step-up-verify</li>
 *   <li>验证通过后签发正式 Access Token + Refresh Token</li>
 * </ol>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

    private static final String MFA_TOKEN_PREFIX = "auth:mfa:token:";
    private static final long MFA_TOKEN_TTL_SECONDS = 300; // 5 minutes

    private final TotpGenerator totpGenerator;
    private final QrCodeGenerator qrCodeGenerator;
    private final UserProvider userProvider;
    private final MfaRecoveryCodeService recoveryCodeService;
    private final MfaManager mfaManager;

    /**
     * 生成MFA设置信息（密钥和QR码）
     *
     * @param username 用户名
     * @return MFA设置信息 DTO
     */
    public MfaSetupDTO generateMfaSetup(String username) {
        log.info("Generating MFA setup for user: {}", username);

        UserInfo user = userProvider.loadUserByIdentifier(username);
        if (user == null) {
            throw AuthErrorCode.AUTH_USER_NOT_FOUND.exception("用户不存在: %s", username);
        }

        String secret = totpGenerator.generateSecret();
        String qrCodeUrl = qrCodeGenerator.generateQrCodeUrl(username, secret);
        List<String> recoveryCodes = recoveryCodeService.generateRecoveryCodes();

        log.info("Generated MFA QR code for user: {}", username);

        return MfaSetupDTO.builder()
            .secret(secret)
            .qrCodeUrl(qrCodeUrl)
            .formattedSecret(qrCodeGenerator.formatSecretForDisplay(secret))
            .recoveryCodes(recoveryCodes)
            .build();
    }

    /**
     * 验证并启用MFA
     *
     * @param username 用户名
     * @param secret MFA密钥
     * @param code 验证器显示的6位验证码
     * @return true-验证成功并启用
     */
    public boolean enableMfa(String username, String secret, String code) {
        log.info("Attempting to enable MFA for user: {}", username);

        boolean isValid = totpGenerator.verifyTotpCode(secret, code);
        if (!isValid) {
            log.warn("MFA verification failed for user: {}", username);
            throw AuthErrorCode.AUTH_MFA_CODE_ERROR.exception();
        }

        UserInfo user = userProvider.loadUserByIdentifier(username);
        if (user == null) {
            throw AuthErrorCode.AUTH_USER_NOT_FOUND.exception("用户不存在: %s", username);
        }

        // 启用 MFA 并保存密钥
        userProvider.updateMfaStatus(user.getUserId(), true, secret);

        // 保存恢复码
        List<String> recoveryCodes = recoveryCodeService.generateRecoveryCodes();
        recoveryCodeService.saveRecoveryCodes(user.getUserId(), recoveryCodes);

        log.info("MFA enabled successfully for user: {}", username);
        return true;
    }

    /**
     * 验证MFA验证码
     *
     * <p>支持 TOTP 验证码和恢复码双通道验证。</p>
     *
     * @param username 用户名
     * @param code 验证器显示的6位验证码 或 恢复码
     * @return true-验证成功，false-验证失败
     */
    public boolean verifyMfaCode(String username, String code) {
        UserInfo user = userProvider.loadUserByIdentifier(username);
        if (user == null) {
            throw AuthErrorCode.AUTH_USER_NOT_FOUND.exception("用户不存在: %s", username);
        }

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            log.warn("MFA not enabled for user: {}", username);
            return false;
        }

        String mfaSecret = user.getMfaSecret();
        if (mfaSecret == null || mfaSecret.isBlank()) {
            log.error("MFA secret is null for user with MFA enabled: {}", username);
            return false;
        }

        // 尝试 TOTP 验证
        boolean isValid = totpGenerator.verifyTotpCode(mfaSecret, code);
        if (isValid) {
            log.debug("MFA TOTP verification successful for user: {}", username);
            return true;
        }

        // TOTP 失败，尝试恢复码
        boolean recoveryUsed = mfaManager.verifyAndUseRecoveryCode(user.getUserId(), code);
        if (recoveryUsed) {
            log.info("MFA recovery code used for user: {}", username);
            return true;
        }

        log.warn("MFA verification failed for user: {} (TOTP and recovery code both failed)", username);
        return false;
    }

    /**
     * 签发 MFA step-up 临时令牌
     *
     * <p>密码认证通过后，如果需要 MFA，签发一个 5 分钟有效的临时令牌，
     * 前端携带此令牌 + MFA code 完成第二步验证。</p>
     *
     * @param username 用户名（已通过第一因素验证）
     * @param clientId 客户端 ID
     * @return mfaToken 临时令牌
     */
    public String issueMfaToken(String username, String clientId) {
        String mfaToken = UUID.randomUUID().toString().replace("-", "");
        String redisKey = MFA_TOKEN_PREFIX + mfaToken;

        // 存储：mfaToken → username:clientId
        String value = username + ":" + (clientId != null ? clientId : "default");
        RedisUtil.setValue(redisKey, value, MFA_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        log.info("MFA token issued for user: {}, ttl={}s", username, MFA_TOKEN_TTL_SECONDS);
        return mfaToken;
    }

    /**
     * 验证 MFA step-up 临时令牌 + MFA code
     *
     * <p>验证通过后立即删除 mfaToken（一次性使用）。</p>
     *
     * @param mfaToken 临时令牌
     * @param code     MFA 验证码（TOTP 或恢复码）
     * @return 验证通过返回 username:clientId，失败返回 null
     */
    public String verifyMfaStepUp(String mfaToken, String code) {
        if (mfaToken == null || mfaToken.isBlank() || code == null || code.isBlank()) {
            return null;
        }

        String redisKey = MFA_TOKEN_PREFIX + mfaToken;
        String value = RedisUtil.getValue(redisKey);

        if (value == null) {
            log.warn("MFA token not found or expired: ", mfaToken);
            return null;
        }

        // 解析 username
        String username = value.contains(":") ? value.substring(0, value.indexOf(':')) : value;

        // 验证 MFA code
        boolean verified = verifyMfaCode(username, code);
        if (!verified) {
            log.warn("MFA step-up verification failed for user: {}", username);
            return null;
        }

        // 验证成功，删除 mfaToken（一次性）
        RedisUtil.delete(redisKey);
        log.info("MFA step-up verification successful for user: {}", username);

        return value;
    }

    /**
     * 禁用MFA
     *
     * @param username 用户名
     */
    public void disableMfa(String username) {
        log.info("Disabling MFA for user: {}", username);

        UserInfo user = userProvider.loadUserByIdentifier(username);
        if (user == null) {
            throw AuthErrorCode.AUTH_USER_NOT_FOUND.exception("用户不存在: %s", username);
        }

        // 禁用 MFA 并清除密钥
        userProvider.updateMfaStatus(user.getUserId(), false, null);

        // 删除恢复码
        mfaManager.clearRecoveryCodes(user.getUserId());

        log.info("MFA disabled successfully for user: {}", username);
    }

    /**
     * 重置MFA密钥
     *
     * @param username 用户名
     * @return 新的MFA设置信息 DTO
     */
    public MfaSetupDTO resetMfaSecret(String username) {
        log.info("Resetting MFA secret for user: {}", username);
        disableMfa(username);
        return generateMfaSetup(username);
    }

    /**
     * 检查用户是否启用MFA
     *
     * @param username 用户名
     * @return true-已启用，false-未启用
     */
    public boolean isMfaEnabled(String username) {
        UserInfo user = userProvider.loadUserByIdentifier(username);
        if (user == null) {
            return false;
        }
        return Boolean.TRUE.equals(user.getMfaEnabled());
    }

    /**
     * 检查是否为受信任设备（跳过 MFA）
     *
     * @param userId            用户 ID
     * @param deviceFingerprint 设备指纹
     * @return true-受信任设备，可跳过 MFA
     */
    public boolean isTrustedDevice(Long userId, String deviceFingerprint) {
        if (userId == null || deviceFingerprint == null || deviceFingerprint.isBlank()) {
            return false;
        }

        TrustedDevice device = mfaManager.getTrustedDevice(userId, deviceFingerprint);
        if (device == null) {
            return false;
        }

        // 检查是否过期（30天）
        if (device.getTrustedTime() != null
            && device.getTrustedTime().plusDays(30).isBefore(LocalDateTime.now())) {
            log.info("Trusted device expired for user: {}, device: {}", userId, deviceFingerprint);
            return false;
        }

        // 更新最后使用时间
        device.setLastUsedTime(LocalDateTime.now());
        mfaManager.saveTrustedDevice(device);

        return true;
    }

    /**
     * 添加受信任设备
     *
     * @param userId            用户 ID
     * @param deviceFingerprint 设备指纹
     * @param deviceName        设备名称（如 "Chrome on Windows"）
     * @param ipAddress         IP 地址
     * @param location          地理位置
     * @param userAgent         User-Agent
     */
    public void addTrustedDevice(Long userId, String deviceFingerprint, String deviceName,
                                  String ipAddress, String location, String userAgent) {
        TrustedDevice device = new TrustedDevice();
        device.setUserId(userId);
        device.setDeviceFingerprint(deviceFingerprint);
        device.setDeviceName(deviceName);
        device.setIpAddress(ipAddress);
        device.setLocation(location);
        device.setUserAgent(userAgent);
        device.setTrustedTime(LocalDateTime.now());
        device.setLastUsedTime(LocalDateTime.now());

        mfaManager.saveTrustedDevice(device);
        log.info("Trusted device added for user: {}, device: {}", userId, deviceName);
    }

    /**
     * 获取用户的所有受信任设备
     *
     * @param userId 用户 ID
     * @return 受信任设备列表
     */
    public List<TrustedDevice> getTrustedDevices(Long userId) {
        return mfaManager.getTrustedDevicesByUserId(userId);
    }

    /**
     * 移除受信任设备
     *
     * @param userId   用户 ID
     * @param deviceId 设备 ID
     */
    public void removeTrustedDevice(Long userId, Long deviceId) {
        mfaManager.removeTrustedDevice(userId, deviceId);
        log.info("Trusted device removed for user: {}, deviceId: {}", userId, deviceId);
    }

    /**
     * 生成新的MFA验证码（用于测试）
     *
     * @param secret MFA密钥
     * @return 6位验证码
     */
    public String generateTotpCode(String secret) {
        return totpGenerator.getTotpCode(secret);
    }
}
