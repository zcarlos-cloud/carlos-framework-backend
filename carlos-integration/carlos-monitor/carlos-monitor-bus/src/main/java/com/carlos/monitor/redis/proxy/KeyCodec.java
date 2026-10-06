package com.carlos.monitor.redis.proxy;

import java.nio.charset.StandardCharsets;

/**
 * <p>
 * Redis key 二进制安全编解码（UTF-8 + 不可见字符转义）
 * 展示层统一走 encode，执行层按原样字符串下发（lettuce String codec 即 UTF-8）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
public final class KeyCodec {

    private KeyCodec() {
    }

    /**
     * 编码：不可见字符（控制字符、DEL、非 BMP 之外的代理项）转义为 \\xNN / \\uNNNN
     *
     * @param key 原始 key
     * @return 可安全展示的 key
     */
    public static String encode(String key) {
        if (key == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                sb.append(String.format("\\x%02X", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 解码：\\xNN 转义还原（展示层回传的 key 还原为执行层 key）
     *
     * @param encoded 编码后的 key
     * @return 原始 key
     */
    public static String decode(String encoded) {
        if (encoded == null || encoded.indexOf('\\') < 0) {
            return encoded;
        }
        StringBuilder sb = new StringBuilder(encoded.length());
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c == '\\' && i + 3 < encoded.length() && encoded.charAt(i + 1) == 'x') {
                try {
                    int code = Integer.parseInt(encoded.substring(i + 2, i + 4), 16);
                    sb.append((char) code);
                    i += 3;
                    continue;
                } catch (NumberFormatException e) {
                    // 非合法转义，按原样输出
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * key 转 UTF-8 字节（lettuce String codec 内部即此编码，显式提供便于二进制场景）
     */
    public static byte[] toBytes(String key) {
        return key == null ? new byte[0] : key.getBytes(StandardCharsets.UTF_8);
    }
}
