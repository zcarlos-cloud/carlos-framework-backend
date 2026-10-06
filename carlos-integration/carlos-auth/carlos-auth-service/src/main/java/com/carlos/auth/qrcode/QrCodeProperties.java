package com.carlos.auth.qrcode;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 扫码登录配置属性
 *
 * <p>配置前缀：{@code carlos.auth.qrcode}</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
@Data
@ConfigurationProperties(prefix = "carlos.auth.qrcode")
public class QrCodeProperties {

    /**
     * 是否启用扫码登录
     */
    private boolean enabled = false;

    /**
     * 二维码有效期（秒），默认 120 秒
     */
    private long ttlSeconds = 120;

    /**
     * 二维码内容基础 URL（APP 扫码后打开的 URL 前缀）
     */
    private String qrcodeBaseUrl = "https://auth.carlos.com/q/";

    /**
     * 二维码签名密钥（HMAC-SHA256），生产环境通过环境变量注入
     */
    private String signSecret = "${AUTH_QRCODE_SIGN_SECRET:carlos-qrcode-default-secret}";

    /**
     * SSE 推送间隔（毫秒），状态变更时立即推送
     */
    private long ssePollIntervalMs = 2000;
}
