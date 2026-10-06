package com.carlos.auth.mfa.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * TOTP生成器
 * </p>
 *
 * <p>基于RFC 6238标准，实现时间同步一次性密码</p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Slf4j
@Component
public class TotpGenerator {

    /**
     * TOTP时间步长（30秒）
     */
    private static final long TIME_STEP = TimeUnit.SECONDS.toMillis(30);

    /**
     * 验证码位数
     */
    private static final int CODE_DIGITS = 6;

    /**
     * HMAC算法
     */
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    /**
     * 时间窗口（用于校验，±1个时间步）
     */
    private static final int TIME_WINDOW = 1;

    /**
     * 生成随机密钥
     *
     * @return Base32编码的密钥
     */
    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20]; // 160位
        random.nextBytes(bytes);

        // 使用Base32编码（Google Authenticator标准）
        String secret = Base64.getEncoder().encodeToString(bytes);

        log.debug("Generated TOTP secret: {}", secret.substring(0, 8) + "...");
        return secret;
    }

    /**
     * 获取当前TOTP验证码
     *
     * @param secret Base32编码的密钥
     * @return 6位验证码
     */
    public String getTotpCode(String secret) {
        return getTotpCode(secret, System.currentTimeMillis());
    }

    /**
     * 获取指定时间的TOTP验证码
     *
     * @param secret Base32编码的密钥
     * @param timestamp 时间戳（毫秒）
     * @return 6位验证码
     */
    public String getTotpCode(String secret, long timestamp) {
        try {
            // 解码密钥
            byte[] keyBytes = Base64.getDecoder().decode(secret);

            // 计算时间步长
            long timeStep = timestamp / TIME_STEP;

            // 生成HMAC
            byte[] hash = generateHmac(keyBytes, timeStep);

            // 动态截断
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24) |
                ((hash[offset + 1] & 0xFF) << 16) |
                ((hash[offset + 2] & 0xFF) << 8) |
                (hash[offset + 3] & 0xFF);

            // 取模得到6位数字
            int code = binary % (int) Math.pow(10, CODE_DIGITS);

            // 补零到6位
            return "%06d".formatted(code);

        } catch (Exception e) {
            log.error("Failed to generate TOTP code", e);
            throw new IllegalStateException("TOTP code generation failed", e);
        }
    }

    /**
     * 验证TOTP验证码
     *
     * @param secret Base32编码的密钥
     * @param code 用户输入的验证码
     * @return true-验证通过，false-验证失败
     */
    public boolean verifyTotpCode(String secret, String code) {
        return verifyTotpCode(secret, code, System.currentTimeMillis());
    }

    /**
     * 验证TOTP验证码（带时间窗口）
     *
     * @param secret Base32编码的密钥
     * @param code 用户输入的验证码
     * @param timestamp 当前时间戳
     * @return true-验证通过，false-验证失败
     */
    public boolean verifyTotpCode(String secret, String code, long timestamp) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) {
            return false;
        }

        // 校验时间窗口（±1个时间步）
        for (int i = -TIME_WINDOW; i <= TIME_WINDOW; i++) {
            long targetTime = timestamp + (i * TIME_STEP);
            String targetCode = getTotpCode(secret, targetTime);

            if (code.equals(targetCode)) {
                log.debug("TOTP verification successful for time offset: {}", i);
                return true;
            }
        }

        log.warn("TOTP verification failed");
        return false;
    }

    /**
     * 生成HMAC
     *
     * @param key 密钥
     * @param counter 计数器（时间步长）
     * @return HMAC哈希值
     */
    private byte[] generateHmac(byte[] key, long counter) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec signKey = new SecretKeySpec(key, HMAC_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(signKey);

        // 将计数器转换为8字节数组
        byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();

        return mac.doFinal(counterBytes);
    }

    /**
     * 获取剩余有效时间（秒）
     *
     * @return 剩余有效秒数
     */
    public long getRemainingSeconds() {
        long currentTime = System.currentTimeMillis();
        long timeInCurrentStep = currentTime % TIME_STEP;
        return (TIME_STEP - timeInCurrentStep) / 1000;
    }
}
