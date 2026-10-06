package com.carlos.gateway.whitelist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 白名单检查结果
 * 封装各类白名单的检查结果
 * </p>
 *
 * @author carlos
 * @date 2026/4/10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhitelistCheckResult {

    /**
     * 是否在全局白名单中
     */
    private boolean globalWhitelisted;

    /**
     * 是否在认证白名单中
     */
    private boolean authWhitelisted;

    /**
     * 是否在 WAF 白名单中
     */
    private boolean wafWhitelisted;

    /**
     * 是否在防重放白名单中
     */
    private boolean replayWhitelisted;

    /**
     * 是否在限流白名单中
     */
    private boolean rateLimitWhitelisted;

    /**
     * 是否在路径前缀剥离白名单中
     */
    private boolean prefixStripWhitelisted;

    /**
     * 快速检查是否在全局白名单中
     */
    public boolean isAnyWhitelisted() {
        return globalWhitelisted || authWhitelisted || wafWhitelisted ||
            replayWhitelisted || rateLimitWhitelisted || prefixStripWhitelisted;
    }

    /**
     * 获取指定类型的白名单检查结果
     */
    public boolean isWhitelisted(UnifiedWhitelistFilter.WhitelistType type) {
        return switch (type) {
            case GLOBAL -> globalWhitelisted;
            case AUTH -> authWhitelisted;
            case WAF -> wafWhitelisted;
            case REPLAY -> replayWhitelisted;
            case RATE_LIMIT -> rateLimitWhitelisted;
            case PREFIX_STRIP -> prefixStripWhitelisted;
        };
    }
}
