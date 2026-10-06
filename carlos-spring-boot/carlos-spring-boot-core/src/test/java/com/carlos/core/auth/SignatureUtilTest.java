package com.carlos.core.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SignatureUtil 单元测试
 *
 * <p>HMAC-SHA256 是身份断言的信任根基，用已知答案（KAT）锁定算法与编码，避免将来
 * 「换了实现但测试跟着改」把兼容性破坏掉。</p>
 */
class SignatureUtilTest {

    @Test
    @DisplayName("HMAC-SHA256 已知答案：hello + secret")
    void should_match_known_answer() {
        // openssl dgst -sha256 -hmac secret → base64url(no padding)
        assertEquals("iKqz7ejTrflNJquQ07r9SiCDBww7zOnAFO4EpEOEfAs",
            SignatureUtil.hmacSha256Base64Url("hello", "secret"));

        // 同一摘要的十六进制表示（网关防重放签名头用的就是 hex）
        assertEquals("88aab3ede8d3adf94d26ab90d3bafd4a2083070c3bcce9c014ee04a443847c0b",
            SignatureUtil.hmacSha256Hex("hello", "secret"));
    }

    @Test
    @DisplayName("签名结果不含 Base64 padding 与 URL 保留字符")
    void should_be_url_safe_without_padding() {
        String signature = SignatureUtil.hmacSha256Base64Url("payload", "key");

        assertFalse(signature.contains("="), "不应含 padding");
        assertFalse(signature.contains("+"), "不应含 +");
        assertFalse(signature.contains("/"), "不应含 /");
    }

    @Test
    @DisplayName("同一输入结果稳定，不同密钥结果不同")
    void should_be_deterministic_and_key_sensitive() {
        String first = SignatureUtil.hmacSha256Base64Url("payload", "key-a");

        assertEquals(first, SignatureUtil.hmacSha256Base64Url("payload", "key-a"));
        assertNotEquals(first, SignatureUtil.hmacSha256Base64Url("payload", "key-b"));
        assertNotEquals(first, SignatureUtil.hmacSha256Base64Url("payload2", "key-a"));
    }

    @Test
    @DisplayName("数据或密钥为空时返回 null（调用方据此判定密钥未配置）")
    void should_return_null_when_data_or_secret_blank() {
        assertNull(SignatureUtil.hmacSha256Base64Url(null, "key"));
        assertNull(SignatureUtil.hmacSha256Base64Url("data", null));
        assertNull(SignatureUtil.hmacSha256Base64Url("data", ""));
        assertNull(SignatureUtil.hmacSha256Hex(null, "key"));
        assertNull(SignatureUtil.hmacSha256Hex("data", null));
        assertNull(SignatureUtil.hmacSha256Hex("data", ""));
    }

    @Test
    @DisplayName("常量时间比较：相等为 true，不等/空为 false")
    void should_compare_constant_time() {
        assertTrue(SignatureUtil.constantTimeEquals("abc", "abc"));
        assertFalse(SignatureUtil.constantTimeEquals("abc", "abd"));
        assertFalse(SignatureUtil.constantTimeEquals("abc", "abcd"));
        assertFalse(SignatureUtil.constantTimeEquals("abc", null));
        assertFalse(SignatureUtil.constantTimeEquals(null, "abc"));
        assertFalse(SignatureUtil.constantTimeEquals(null, null));
    }

}
