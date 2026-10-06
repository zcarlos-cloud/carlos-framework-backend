package com.carlos.security.auth;

import com.carlos.core.auth.IdentityAssertion;
import com.carlos.core.auth.IdentityAssertionCodec;
import com.carlos.core.auth.IdentityAssertionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Optional;

/**
 * <p>
 * 身份断言验签器
 * </p>
 *
 * <p>校验网关签发的 {@code X-User-Context} 断言，通过后返回其中的身份声明。相比「读明文
 * {@code X-User-*} 头 + 信任来源 IP」，本方式把完整性的判断交给密码学，因此与调用方所在网段无关，
 * 可以安全地随内部 Feign 调用多跳传播。</p>
 *
 * <p>校验步骤（任一步不通过即拒绝）：</p>
 * <ol>
 *   <li>密钥已配置</li>
 *   <li>格式/版本 + HMAC 签名（常量时间比较；支持 previousSecret 轮换宽限期）</li>
 *   <li>{@code iat} 不超前于当前时间 + 时钟容差</li>
 *   <li>{@code exp} 未过期（含时钟容差）</li>
 *   <li>{@code exp > iat} 且 {@code exp - iat} 不超过生命期上限</li>
 * </ol>
 *
 * <p>开关是<b>每请求读取</b>的（{@link #isEnabled()}），不是在装配期用
 * {@code @ConditionalOnProperty} 判定的 —— 条件化 Bean 不随配置刷新重建，用它在灰度期会造成
 * 「改了配置不生效」的假象，也让单侧回滚失去即时性。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-09-23
 * @see com.carlos.core.auth.IdentityAssertionCodec
 */
@Slf4j
@RequiredArgsConstructor
public class IdentityAssertionVerifier {

    private final IdentityAssertionProperties properties;

    /**
     * 密钥缺失只告警一次的标记，避免每请求刷屏
     */
    private volatile boolean secretMissingLogged = false;

    /**
     * 是否接受身份断言（灰度开关，每请求读取）
     */
    public boolean isEnabled() {
        return properties.isVerifyEnabled();
    }

    /**
     * 校验身份断言
     *
     * @param headerValue 请求头 {@code X-User-Context} 的原始值（调用方须先确认其存在）
     * @return 校验通过的身份声明；任何一步失败返回空 Optional
     */
    public Optional<IdentityAssertion> verify(String headerValue) {
        if (!properties.hasSecret()) {
            if (!secretMissingLogged) {
                secretMissingLogged = true;
                log.error("[身份断言] 未配置 carlos.identity-assertion.secret，无法验签，"
                    + "本请求降级为匿名。请在 Nacos 共享配置 common.yml 中补齐密钥（网关与下游须一致）");
            }
            return Optional.empty();
        }

        Optional<IdentityAssertion> parsed =
            IdentityAssertionCodec.verifyAndParse(headerValue, properties.getSecret(), properties.getPreviousSecret());
        if (parsed.isEmpty()) {
            // 不打印断言内容：它虽然已过期/非法，但仍是可复用的凭据材料
            log.warn("[身份断言] 验签失败（格式非法或签名不匹配），拒绝该断言；"
                + "本过滤器不存在更弱的回退通道，该请求将降级为匿名");
            return Optional.empty();
        }

        IdentityAssertion assertion = parsed.get();
        return validateTime(assertion) ? Optional.of(assertion) : Optional.empty();
    }

    private boolean validateTime(IdentityAssertion assertion) {
        if (assertion.getIat() == null || assertion.getExp() == null) {
            log.warn("[身份断言] 缺少 iat/exp，拒绝");
            return false;
        }

        long now = Instant.now().getEpochSecond();
        long skew = properties.getClockSkew().toSeconds();
        long maxLifetime = properties.getMaxLifetime().toSeconds();
        long iat = assertion.getIat();
        long exp = assertion.getExp();

        if (iat > now + skew) {
            log.warn("[身份断言] 签发时间超前于当前时间超过容差：iat={}, now={}, skew={}s", iat, now, skew);
            return false;
        }

        if (exp + skew < now) {
            log.warn("[身份断言] 已过期：exp={}, now={}, skew={}s", exp, now, skew);
            return false;
        }

        if (exp <= iat) {
            log.warn("[身份断言] exp 不大于 iat：iat={}, exp={}", iat, exp);
            return false;
        }

        if (exp - iat > maxLifetime) {
            log.warn("[身份断言] 生命期超过上限：exp-iat={}s, maxLifetime={}s", exp - iat, maxLifetime);
            return false;
        }

        return true;
    }

}
