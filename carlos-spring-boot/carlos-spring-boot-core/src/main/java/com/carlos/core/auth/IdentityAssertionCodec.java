package com.carlos.core.auth;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * <p>
 * 身份断言编解码
 * </p>
 *
 * <p>线格式（HTTP 头 {@code X-User-Context}）：</p>
 * <pre>
 * v1.&lt;payloadBase64Url&gt;.&lt;signatureBase64Url&gt;
 * </pre>
 *
 * <p>签名串为 {@code "v1." + payloadBase64Url}，签名算法 HMAC-SHA256（Base64Url 无 padding）。
 * 载荷是紧凑 JSON：{@code {"uid":..,"acc":..,"roles":[..],"dept":..,"ten":..,"iat":..,"exp":..}}，
 * 其中 {@code iat}/{@code exp} 为 epoch <b>秒</b>。</p>
 *
 * <p><b>为什么签「编码后的字节」而不是「JSON 语义」：</b>验签方校验的是收到的原始字节，不做任何
 * 重新序列化，因此两侧不需要共享字段顺序、空格、转义等规范化约定 —— 也就没有「改了一侧忘了改另一侧」
 * 导致验签静默失效的空间。这是本设计刻意规避的缺陷类型。</p>
 *
 * <p><b>为什么载荷用 JSON 而不是分隔符拼串：</b>拼接方案下，若某字段（如账号、角色编码）含分隔符，
 * 解析出的字段边界会错位 —— 那将是一个「签名有效但身份被错解」的漏洞。JSON 没有这个歧义。</p>
 *
 * <p><b>校验顺序固定为「先验签、后解析」</b>，{@link #verifyAndParse} 把这两步封在一起，
 * 调用方无法把顺序弄反（绝不解析未验签的载荷）。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-09-23
 * @see IdentityAssertion
 * @see SignatureUtil
 */
public final class IdentityAssertionCodec {

    /**
     * 线格式版本，为将来换算法/加字段留路
     */
    public static final String VERSION = "v1";

    private static final String SEPARATOR = ".";
    private static final int PART_COUNT = 3;

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_ACC = "acc";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_DEPT = "dept";
    private static final String CLAIM_TENANT = "ten";
    private static final String CLAIM_IAT = "iat";
    private static final String CLAIM_EXP = "exp";

    private IdentityAssertionCodec() {
        // 工具类禁止实例化
    }

    /**
     * 生成身份断言头值
     *
     * @param assertion 断言载荷，{@code uid} 必填
     * @param secret    签名密钥
     * @return {@code v1.<payload>.<signature>}
     * @throws IllegalArgumentException uid 为空
     * @throws IllegalStateException    密钥为空或签名失败
     */
    public static String encode(IdentityAssertion assertion, String secret) {
        if (assertion == null || assertion.getUid() == null) {
            throw new IllegalArgumentException("身份断言的 uid 不能为空");
        }

        String payloadB64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(toJson(assertion).getBytes(StandardCharsets.UTF_8));

        String signed = VERSION + SEPARATOR + payloadB64;
        String signature = SignatureUtil.hmacSha256Base64Url(signed, secret);
        if (signature == null) {
            throw new IllegalStateException("身份断言签名失败：密钥为空");
        }

        return signed + SEPARATOR + signature;
    }

    /**
     * 判断请求头里是否存在身份断言
     *
     * <p>调用方据此区分「断言缺失」（灰度期可回退旧通道）与「断言存在但非法」（必须拒绝，
     * 不得回退）。</p>
     */
    public static boolean isPresent(String headerValue) {
        return StrUtil.isNotBlank(headerValue);
    }

    /**
     * 验签并解析身份断言
     *
     * <p>先校验签名（常量时间比较），签名通过后才解析载荷。支持传入一个用于密钥轮换的旧密钥：
     * 当前密钥验签失败时会再尝试旧密钥，两者都不通过才算失败。</p>
     *
     * @param headerValue    请求头原始值
     * @param primarySecret  当前签名密钥
     * @param previousSecret 旧签名密钥（轮换宽限期用，可为空）
     * @return 验签通过的断言；格式非法 / 版本不匹配 / 验签失败 / 载荷无法解析时返回空 Optional
     */
    public static Optional<IdentityAssertion> verifyAndParse(String headerValue,
                                                            String primarySecret,
                                                            String previousSecret) {
        if (!isPresent(headerValue)) {
            return Optional.empty();
        }

        String[] parts = headerValue.split("\\" + SEPARATOR, -1);
        if (parts.length != PART_COUNT || !VERSION.equals(parts[0])) {
            return Optional.empty();
        }

        String signed = parts[0] + SEPARATOR + parts[1];
        String actualSignature = parts[2];

        if (!signatureMatches(signed, actualSignature, primarySecret)
            && !signatureMatches(signed, actualSignature, previousSecret)) {
            return Optional.empty();
        }

        return parsePayload(parts[1]);
    }

    private static boolean signatureMatches(String signed, String actualSignature, String secret) {
        String expected = SignatureUtil.hmacSha256Base64Url(signed, secret);
        return SignatureUtil.constantTimeEquals(expected, actualSignature);
    }

    private static String toJson(IdentityAssertion assertion) {
        JSONObject json = JSONUtil.createObj();
        json.set(CLAIM_UID, assertion.getUid());
        if (assertion.getAcc() != null) {
            json.set(CLAIM_ACC, assertion.getAcc());
        }
        if (assertion.getRoles() != null) {
            // 排序只为可读性与日志稳定性；验签校验的是原始字节，与顺序无关
            json.set(CLAIM_ROLES, new ArrayList<>(new TreeSet<>(assertion.getRoles())));
        }
        if (assertion.getDept() != null) {
            json.set(CLAIM_DEPT, assertion.getDept());
        }
        if (assertion.getTen() != null) {
            json.set(CLAIM_TENANT, assertion.getTen());
        }
        if (assertion.getIat() != null) {
            json.set(CLAIM_IAT, assertion.getIat());
        }
        if (assertion.getExp() != null) {
            json.set(CLAIM_EXP, assertion.getExp());
        }
        return json.toString();
    }

    private static Optional<IdentityAssertion> parsePayload(String payloadB64) {
        try {
            String json = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
            JSONObject obj = JSONUtil.parseObj(json);

            Long uid = obj.getLong(CLAIM_UID);
            if (uid == null) {
                return Optional.empty();
            }

            IdentityAssertion assertion = new IdentityAssertion()
                .setUid(uid)
                .setAcc(obj.getStr(CLAIM_ACC))
                .setDept(obj.getLong(CLAIM_DEPT))
                .setTen(obj.getLong(CLAIM_TENANT))
                .setIat(obj.getLong(CLAIM_IAT))
                .setExp(obj.getLong(CLAIM_EXP));

            JSONArray roles = obj.getJSONArray(CLAIM_ROLES);
            if (roles != null && !roles.isEmpty()) {
                List<String> roleCodes = new ArrayList<>(roles.size());
                for (Object role : roles) {
                    if (role != null) {
                        roleCodes.add(role.toString());
                    }
                }
                assertion.setRoles(roleCodes);
            }

            return Optional.of(assertion);
        } catch (Exception e) {
            // 载荷不是合法 Base64Url 或不是 JSON；调用方无需区分，统一按无效断言处理
            return Optional.empty();
        }
    }

}
