package com.carlos.auth.util;

import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * <p>
 * QR码生成器
 * </p>
 *
 * <p>生成用于Google Authenticator的TOTP otpauth URI</p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Slf4j
@Component
public class QrCodeGenerator {

    /**
     * 应用名称（用于QR码）
     */
    private static final String APP_NAME = "CarlosAuth";

    /**
     * 生成TOTP的otpauth URI
     *
     * <p>URI 格式符合 RFC 6238 / Google Authenticator 规范：</p>
     * <pre>otpauth://totp/{issuer}:{username}?secret={secret}&issuer={issuer}</pre>
     *
     * <p><strong>注意：</strong>不会将 secret 发送到任何第三方服务，
     * 前端应使用 <code>qrcode.js</code> 等库在浏览器本地渲染 QR 码。</p>
     *
     * @param username 用户名
     * @param secret   密钥（Base32编码）
     * @return otpauth URI
     */
    public String generateOtpAuthUri(String username, String secret) {
        if (username == null || secret == null) {
            throw new BusinessException(AuthErrorCode.AUTH_MFA_QR_PARAM_INVALID);
        }

        String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
        String encodedSecret = URLEncoder.encode(secret, StandardCharsets.UTF_8);
        String encodedIssuer = URLEncoder.encode(APP_NAME, StandardCharsets.UTF_8);

        String uri = "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30".formatted(
            APP_NAME,
            encodedUsername,
            encodedSecret,
            encodedIssuer
        );

        log.debug("Generated TOTP URI for user: {}", username);
        return uri;
    }

    /**
     * 生成TOTP的QR码数据
     *
     * <p>返回 <code>otpauth://</code> URI 字符串，由前端在本地渲染为 QR 码图片。</p>
     *
     * <p><strong>安全说明：</strong>旧版实现通过 Google Chart API 在线生成 QR 码，
     * 会将密钥发送到第三方服务器。本方法已移除该行为，改为返回 URI 供客户端本地渲染。</p>
     *
     * @param username 用户名
     * @param secret   密钥（Base32编码）
     * @return otpauth URI（前端需使用 qrcode.js 等库渲染）
     * @deprecated 方法语义不变，但实现已改为本地安全模式。请直接调用 {@link #generateOtpAuthUri}。
     */
    @Deprecated(since = "3.0.0", forRemoval = false)
    public String generateQrCodeUrl(String username, String secret) {
        return generateOtpAuthUri(username, secret);
    }

    /**
     * 格式化显示密钥（便于手动输入）
     *
     * @param secret Base32编码的密钥
     * @return 格式化字符串（每4个字符一组）
     */
    public String formatSecretForDisplay(String secret) {
        if (secret == null || secret.isEmpty()) {
            return "";
        }

        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < secret.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ");
            }
            formatted.append(secret.charAt(i));
        }

        return formatted.toString();
    }
}
