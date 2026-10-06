package com.carlos.core.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

/**
 * <p>
 * 签名工具
 * </p>
 *
 * <p>提供 HMAC-SHA256 签名与常量时间比较，供内部身份断言（{@link IdentityAssertionCodec}）以及
 * 其它需要对称签名的场景使用。</p>
 *
 * <p>本类不依赖 Spring，密钥由调用方传入，因此可脱离容器直接单元测试。</p>
 *
 * <p><b>为什么比较必须用常量时间：</b>{@code String.equals} 在首个不同字节处提前返回，比较耗时
 * 与「猜对了几个字节」相关，理论上可被计时侧信道逐字节爆破签名。{@link MessageDigest#isEqual}
 * 对等长输入做无分支比较。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-09-23
 */
public final class SignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private SignatureUtil() {
        // 工具类禁止实例化
    }

    /**
     * HMAC-SHA256 签名，结果为 URL 安全的 Base64（无 padding）
     *
     * @param data   待签数据
     * @param secret 密钥（UTF-8 原始字节）
     * @return Base64Url 编码的签名；密钥或数据为空时返回 null
     */
    public static String hmacSha256Base64Url(String data, String secret) {
        byte[] raw = hmacSha256(data, secret);
        return raw == null ? null : Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    /**
     * HMAC-SHA256 签名，结果为小写十六进制
     *
     * <p>与 {@link #hmacSha256Base64Url} 只是编码不同，用于兼容已按 hex 约定的调用方
     * （如网关的防重放签名头）。</p>
     *
     * @param data   待签数据
     * @param secret 密钥（UTF-8 原始字节）
     * @return 小写 hex 签名；密钥或数据为空时返回 null
     */
    public static String hmacSha256Hex(String data, String secret) {
        byte[] raw = hmacSha256(data, secret);
        return raw == null ? null : HexFormat.of().formatHex(raw);
    }

    /**
     * HMAC-SHA256 原始字节
     *
     * @return 32 字节摘要；数据或密钥为空时返回 null
     */
    private static byte[] hmacSha256(String data, String secret) {
        if (data == null || secret == null || secret.isEmpty()) {
            return null;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 算法固定、初始化参数固定，正常情况下不会走到这里；抛异常说明运行环境异常
            throw new IllegalStateException("HMAC-SHA256 签名失败", e);
        }
    }

    /**
     * 常量时间比较两个字符串
     *
     * @param expected 期望值
     * @param actual   实际值
     * @return true 表示相等；任一为 null 返回 false
     */
    public static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            actual.getBytes(StandardCharsets.UTF_8));
    }

}
