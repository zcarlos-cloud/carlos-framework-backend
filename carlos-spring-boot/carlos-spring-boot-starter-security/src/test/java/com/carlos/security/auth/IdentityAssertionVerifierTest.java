package com.carlos.security.auth;

import com.carlos.core.auth.IdentityAssertion;
import com.carlos.core.auth.IdentityAssertionCodec;
import com.carlos.core.auth.IdentityAssertionProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IdentityAssertionVerifier 单元测试
 *
 * <p>覆盖验签之外的全部时间维度校验：过期、超前、生命期上限，以及密钥缺失时的降级行为。
 * 签名本身的正确性由 {@code IdentityAssertionCodecTest} 覆盖。</p>
 */
class IdentityAssertionVerifierTest {

    private static final String SECRET = "unit-test-secret-key-32-bytes-long!!";
    private static final String PREVIOUS_SECRET = "previous-secret-key-32-bytes-long!!";
    private static final long NOW = Instant.now().getEpochSecond();

    private IdentityAssertionProperties properties() {
        IdentityAssertionProperties properties = new IdentityAssertionProperties();
        properties.setSecret(SECRET);
        properties.setTtl(Duration.ofMinutes(5));
        properties.setClockSkew(Duration.ofSeconds(60));
        properties.setMaxLifetime(Duration.ofMinutes(10));
        return properties;
    }

    private String sign(long iat, long exp, String secret) {
        IdentityAssertion assertion = new IdentityAssertion()
            .setUid(2076141414136696832L)
            .setAcc("admin")
            .setRoles(List.of("SUPER_ADMIN"))
            .setIat(iat)
            .setExp(exp);
        return IdentityAssertionCodec.encode(assertion, secret);
    }

    private String valid() {
        return sign(NOW, NOW + 300, SECRET);
    }

    // ------------------------- 正常 -------------------------

    @Test
    @DisplayName("有效断言通过校验并保留全部字段")
    void should_accept_valid_assertion() {
        IdentityAssertionVerifier verifier = new IdentityAssertionVerifier(properties());

        IdentityAssertion assertion = verifier.verify(valid()).orElseThrow();

        assertEquals(2076141414136696832L, assertion.getUid());
        assertEquals("admin", assertion.getAcc());
        assertEquals(List.of("SUPER_ADMIN"), assertion.getRoles());
    }

    @Test
    @DisplayName("isEnabled 反映 verify-enabled 配置")
    void should_reflect_verify_enabled() {
        IdentityAssertionProperties properties = properties();
        IdentityAssertionVerifier verifier = new IdentityAssertionVerifier(properties);

        properties.setVerifyEnabled(true);
        assertTrue(verifier.isEnabled());

        // 每请求读取：改了配置无需重建 verifier
        properties.setVerifyEnabled(false);
        assertFalse(verifier.isEnabled());
    }

    // ------------------------- 时间校验 -------------------------

    @Test
    @DisplayName("已过期（超出时钟容差）的断言被拒绝")
    void should_reject_expired_assertion() {
        IdentityAssertionVerifier verifier = new IdentityAssertionVerifier(properties());

        // 过期 5 分钟，远超 60s 容差
        assertTrue(verifier.verify(sign(NOW - 600, NOW - 300, SECRET)).isEmpty());
    }

    @Test
    @DisplayName("过期但在时钟容差内仍接受")
    void should_accept_within_clock_skew() {
        IdentityAssertionVerifier verifier = new IdentityAssertionVerifier(properties());

        // 刚过期 10s，容差 60s
        assertTrue(verifier.verify(sign(NOW - 310, NOW - 10, SECRET)).isPresent());
    }

    @Test
    @DisplayName("签发时间超前于当前时间超过容差的断言被拒绝")
    void should_reject_future_issued_assertion() {
        IdentityAssertionVerifier verifier = new IdentityAssertionVerifier(properties());

        assertTrue(verifier.verify(sign(NOW + 600, NOW + 900, SECRET)).isEmpty());
    }

    @Test
    @DisplayName("exp 不大于 iat 的断言被拒绝")
    void should_reject_non_positive_lifetime() {
        IdentityAssertionVerifier verifier = new IdentityAssertionVerifier(properties());

        assertTrue(verifier.verify(sign(NOW, NOW, SECRET)).isEmpty());
        assertTrue(verifier.verify(sign(NOW, NOW - 100, SECRET)).isEmpty());
    }

    @Test
    @DisplayName("生命期超过 max-lifetime 的断言被拒绝（防止用超长有效期绕过撤销）")
    void should_reject_lifetime_over_max() {
        IdentityAssertionProperties properties = properties();
        properties.setMaxLifetime(Duration.ofMinutes(10));
        IdentityAssertionVerifier verifier = new IdentityAssertionVerifier(properties);

        // 生命期 60 分钟 > 上限 10 分钟
        assertTrue(verifier.verify(sign(NOW, NOW + 3600, SECRET)).isEmpty());
    }

    @Test
    @DisplayName("缺少 iat/exp 的断言被拒绝")
    void should_reject_missing_time_claims() {
        IdentityAssertionVerifier verifier = new IdentityAssertionVerifier(properties());
        String noExp = IdentityAssertionCodec.encode(
            new IdentityAssertion().setUid(1L).setIat(NOW), SECRET);
        String noTime = IdentityAssertionCodec.encode(
            new IdentityAssertion().setUid(1L), SECRET);

        assertTrue(verifier.verify(noExp).isEmpty());
        assertTrue(verifier.verify(noTime).isEmpty());
    }

    // ------------------------- 密钥 -------------------------

    @Test
    @DisplayName("未配置密钥时一律拒绝（避免空密钥等于无签名）")
    void should_reject_when_secret_missing() {
        IdentityAssertionProperties properties = properties();
        properties.setSecret(null);
        IdentityAssertionVerifier verifier = new IdentityAssertionVerifier(properties);

        assertTrue(verifier.verify(valid()).isEmpty());
    }

    @Test
    @DisplayName("轮换期：旧密钥签发的断言可通过")
    void should_accept_previous_secret() {
        IdentityAssertionProperties properties = properties();
        properties.setPreviousSecret(PREVIOUS_SECRET);
        IdentityAssertionVerifier verifier = new IdentityAssertionVerifier(properties);

        String signedByOldKey = sign(NOW, NOW + 300, PREVIOUS_SECRET);

        assertTrue(verifier.verify(signedByOldKey).isPresent());
    }

    @Test
    @DisplayName("篡改的断言被拒绝")
    void should_reject_tampered_assertion() {
        IdentityAssertionVerifier verifier = new IdentityAssertionVerifier(properties());
        String value = valid();
        String[] parts = value.split("\\.", -1);

        String tampered = parts[0] + "." + parts[1] + "." + parts[2].substring(0, 1) + "zzzz";

        assertTrue(verifier.verify(tampered).isEmpty());
    }

    @Test
    @DisplayName("非法输入不抛异常，只返回空")
    void should_not_throw_on_garbage() {
        IdentityAssertionVerifier verifier = new IdentityAssertionVerifier(properties());

        assertTrue(verifier.verify("not-an-assertion").isEmpty());
        assertTrue(verifier.verify("").isEmpty());
        assertTrue(verifier.verify(null).isEmpty());
        assertTrue(verifier.verify("v1.!!!.???").isEmpty());
    }

}
