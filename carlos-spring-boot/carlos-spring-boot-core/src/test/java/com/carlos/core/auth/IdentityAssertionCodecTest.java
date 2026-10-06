package com.carlos.core.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IdentityAssertionCodec 单元测试
 *
 * <p>这是「网关签名 → 下游验签」这条信任链的核心，测试覆盖三类：正常往返、各类篡改、
 * 以及载荷编码的边界（分隔符、中文）。</p>
 */
class IdentityAssertionCodecTest {

    private static final String SECRET = "unit-test-secret-key-32-bytes-long!!";
    private static final String OTHER_SECRET = "another-secret-key-32-bytes-long!!!";
    private static final String PREVIOUS_SECRET = "previous-secret-key-32-bytes-long!!";

    private static final long IAT = 1790139300L;
    private static final long EXP = IAT + 300;

    private IdentityAssertion sampleAssertion() {
        return new IdentityAssertion()
            .setUid(2076141414136696832L)
            .setAcc("admin")
            .setRoles(List.of("SUPER_ADMIN", "ORG_MANAGER"))
            .setDept(2076141404842119168L)
            .setTen(1L)
            .setIat(IAT)
            .setExp(EXP);
    }

    // ------------------------- 正常往返 -------------------------

    @Test
    @DisplayName("编码后可验签并完整还原全部字段")
    void should_roundtrip_all_claims() {
        String value = IdentityAssertionCodec.encode(sampleAssertion(), SECRET);

        assertTrue(value.startsWith("v1."), "应以版本前缀开头");
        assertEquals(3, value.split("\\.", -1).length);

        IdentityAssertion parsed = IdentityAssertionCodec.verifyAndParse(value, SECRET, null).orElseThrow();

        assertEquals(2076141414136696832L, parsed.getUid());
        assertEquals("admin", parsed.getAcc());
        assertEquals(2, parsed.getRoles().size());
        assertTrue(parsed.getRoles().contains("SUPER_ADMIN"));
        assertTrue(parsed.getRoles().contains("ORG_MANAGER"));
        assertEquals(2076141404842119168L, parsed.getDept());
        assertEquals(1L, parsed.getTen());
        assertEquals(IAT, parsed.getIat());
        assertEquals(EXP, parsed.getExp());
    }

    @Test
    @DisplayName("只有 uid 时也能往返（其余 claim 可空）")
    void should_roundtrip_minimal_assertion() {
        String value = IdentityAssertionCodec.encode(new IdentityAssertion().setUid(1L), SECRET);

        IdentityAssertion parsed = IdentityAssertionCodec.verifyAndParse(value, SECRET, null).orElseThrow();

        assertEquals(1L, parsed.getUid());
        assertNull(parsed.getAcc());
        assertNull(parsed.getRoles());
        assertNull(parsed.getExp());
    }

    @Test
    @DisplayName("角色编码含逗号/中文/竖线时不发生字段错位")
    void should_survive_delimiter_like_role_codes() {
        List<String> tricky = List.of("ROLE,with,comma", "角色|带竖线", "a.b");
        IdentityAssertion assertion = new IdentityAssertion().setUid(9L).setRoles(tricky).setAcc("acc,with,comma");

        String value = IdentityAssertionCodec.encode(assertion, SECRET);
        IdentityAssertion parsed = IdentityAssertionCodec.verifyAndParse(value, SECRET, null).orElseThrow();

        assertEquals("acc,with,comma", parsed.getAcc());
        assertEquals(3, parsed.getRoles().size());
        assertTrue(parsed.getRoles().containsAll(tricky));
    }

    @Test
    @DisplayName("同一断言重复编码结果稳定（签名不受角色顺序影响）")
    void should_be_stable() {
        IdentityAssertion reordered = sampleAssertion().setRoles(List.of("ORG_MANAGER", "SUPER_ADMIN"));

        assertEquals(
            IdentityAssertionCodec.encode(sampleAssertion(), SECRET),
            IdentityAssertionCodec.encode(reordered, SECRET));
    }

    // ------------------------- 篡改与非法输入 -------------------------

    @Test
    @DisplayName("篡改载荷后验签失败（签名覆盖载荷）")
    void should_reject_tampered_payload() {
        String value = IdentityAssertionCodec.encode(sampleAssertion(), SECRET);
        String[] parts = value.split("\\.", -1);

        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"uid\":1}".getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        assertTrue(IdentityAssertionCodec.verifyAndParse(tampered, SECRET, null).isEmpty());
    }

    @Test
    @DisplayName("篡改签名后验签失败")
    void should_reject_tampered_signature() {
        String value = IdentityAssertionCodec.encode(sampleAssertion(), SECRET);
        String[] parts = value.split("\\.", -1);

        // 确定性翻转末位字符（不能依赖「替换成 X」，末位本来就是 X 时等于没改）
        char last = parts[2].charAt(parts[2].length() - 1);
        char replacement = last == 'A' ? 'B' : 'A';
        String tamperedSignature = parts[2].substring(0, parts[2].length() - 1) + replacement;
        String tampered = parts[0] + "." + parts[1] + "." + tamperedSignature;

        assertTrue(IdentityAssertionCodec.verifyAndParse(tampered, SECRET, null).isEmpty());
    }

    @Test
    @DisplayName("换错密钥验签失败")
    void should_reject_wrong_secret() {
        String value = IdentityAssertionCodec.encode(sampleAssertion(), SECRET);

        assertTrue(IdentityAssertionCodec.verifyAndParse(value, OTHER_SECRET, null).isEmpty());
    }

    @Test
    @DisplayName("密钥为空时不接受任何断言（避免空密钥等于无签名）")
    void should_reject_when_secret_blank() {
        String value = IdentityAssertionCodec.encode(sampleAssertion(), SECRET);

        assertTrue(IdentityAssertionCodec.verifyAndParse(value, null, null).isEmpty());
        assertTrue(IdentityAssertionCodec.verifyAndParse(value, "", null).isEmpty());
    }

    @Test
    @DisplayName("版本前缀不匹配时拒绝（为将来换算法留的保险）")
    void should_reject_unknown_version() {
        String value = IdentityAssertionCodec.encode(sampleAssertion(), SECRET);
        String tampered = "v2" + value.substring(2);

        assertTrue(IdentityAssertionCodec.verifyAndParse(tampered, SECRET, null).isEmpty());
    }

    @Test
    @DisplayName("结构非法（段数不对/为空/非 Base64）时拒绝")
    void should_reject_malformed_value() {
        assertTrue(IdentityAssertionCodec.verifyAndParse("v1.onlytwo", SECRET, null).isEmpty());
        assertTrue(IdentityAssertionCodec.verifyAndParse("v1..", SECRET, null).isEmpty());
        assertTrue(IdentityAssertionCodec.verifyAndParse("garbage", SECRET, null).isEmpty());

        // 签名合法但载荷不是合法 JSON：先验签通过，再解析失败 → 仍须拒绝
        String badJsonPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("not-json".getBytes(StandardCharsets.UTF_8));
        String signed = IdentityAssertionCodec.VERSION + "." + badJsonPayload;
        String signature = SignatureUtil.hmacSha256Base64Url(signed, SECRET);
        assertTrue(IdentityAssertionCodec.verifyAndParse(signed + "." + signature, SECRET, null).isEmpty());
    }

    @Test
    @DisplayName("载荷缺 uid 时拒绝")
    void should_reject_payload_without_uid() {
        String payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"acc\":\"admin\"}".getBytes(StandardCharsets.UTF_8));
        String signed = IdentityAssertionCodec.VERSION + "." + payload;
        String signature = SignatureUtil.hmacSha256Base64Url(signed, SECRET);

        assertTrue(IdentityAssertionCodec.verifyAndParse(signed + "." + signature, SECRET, null).isEmpty());
    }

    // ------------------------- 密钥轮换 -------------------------

    @Test
    @DisplayName("轮换宽限期：旧密钥签发的断言仍可通过（用 previousSecret 验）")
    void should_accept_previous_secret_during_rotation() {
        String signedByOldKey = IdentityAssertionCodec.encode(sampleAssertion(), PREVIOUS_SECRET);

        assertTrue(IdentityAssertionCodec.verifyAndParse(signedByOldKey, OTHER_SECRET, null).isEmpty(),
            "不给旧密钥时必须拒绝");
        Optional<IdentityAssertion> parsed =
            IdentityAssertionCodec.verifyAndParse(signedByOldKey, OTHER_SECRET, PREVIOUS_SECRET);

        assertTrue(parsed.isPresent(), "给了旧密钥后应通过");
        assertEquals(2076141414136696832L, parsed.get().getUid());
    }

    // ------------------------- 编码前置校验 -------------------------

    @Test
    @DisplayName("编码时 uid 为空抛 IllegalArgumentException")
    void should_reject_encode_without_uid() {
        assertThrows(IllegalArgumentException.class,
            () -> IdentityAssertionCodec.encode(new IdentityAssertion(), SECRET));
        assertThrows(IllegalArgumentException.class,
            () -> IdentityAssertionCodec.encode(null, SECRET));
    }

    @Test
    @DisplayName("编码时密钥为空抛 IllegalStateException（不允许签发无签名断言）")
    void should_reject_encode_without_secret() {
        assertThrows(IllegalStateException.class,
            () -> IdentityAssertionCodec.encode(sampleAssertion(), ""));
    }

    @Test
    @DisplayName("isPresent 区分「缺失」与「存在但非法」")
    void should_detect_presence() {
        assertFalse(IdentityAssertionCodec.isPresent(null));
        assertFalse(IdentityAssertionCodec.isPresent(""));
        assertFalse(IdentityAssertionCodec.isPresent("   "));
        assertTrue(IdentityAssertionCodec.isPresent("garbage"));
    }

}
