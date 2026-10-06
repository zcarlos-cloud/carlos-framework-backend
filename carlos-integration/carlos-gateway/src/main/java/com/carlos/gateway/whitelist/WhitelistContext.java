package com.carlos.gateway.whitelist;

import org.springframework.web.server.ServerWebExchange;

import java.util.Optional;

/**
 * <p>
 * 白名单上下文工具类
 * 用于在 Exchange Attributes 中存取白名单检查结果
 * </p>
 *
 * @author carlos
 * @date 2026/4/10
 */
public final class WhitelistContext {

    private WhitelistContext() {
        // 工具类，禁止实例化
    }

    /**
     * Exchange Attribute 键名
     */
    private static final String WHITELIST_CHECK_RESULT_KEY = "carlos.gateway.whitelist.checkResult";

    /**
     * 设置白名单检查结果到 Exchange
     */
    public static void setCheckResult(ServerWebExchange exchange, WhitelistCheckResult result) {
        exchange.getAttributes().put(WHITELIST_CHECK_RESULT_KEY, result);
    }

    /**
     * 从 Exchange 获取白名单检查结果
     */
    public static Optional<WhitelistCheckResult> getCheckResult(ServerWebExchange exchange) {
        Object result = exchange.getAttribute(WHITELIST_CHECK_RESULT_KEY);
        if (result instanceof WhitelistCheckResult wcr) {
            return Optional.of(wcr);
        }
        return Optional.empty();
    }

    /**
     * 检查是否在全局白名单中
     */
    public static boolean isGlobalWhitelisted(ServerWebExchange exchange) {
        return getCheckResult(exchange)
            .map(WhitelistCheckResult::isGlobalWhitelisted)
            .orElse(false);
    }

    /**
     * 检查是否在认证白名单中
     */
    public static boolean isAuthWhitelisted(ServerWebExchange exchange) {
        return getCheckResult(exchange)
            .map(WhitelistCheckResult::isAuthWhitelisted)
            .orElse(false);
    }

    /**
     * 检查是否在 WAF 白名单中
     */
    public static boolean isWafWhitelisted(ServerWebExchange exchange) {
        return getCheckResult(exchange)
            .map(WhitelistCheckResult::isWafWhitelisted)
            .orElse(false);
    }

    /**
     * 检查是否在防重放白名单中
     */
    public static boolean isReplayWhitelisted(ServerWebExchange exchange) {
        return getCheckResult(exchange)
            .map(WhitelistCheckResult::isReplayWhitelisted)
            .orElse(false);
    }

    /**
     * 检查是否在限流白名单中
     */
    public static boolean isRateLimitWhitelisted(ServerWebExchange exchange) {
        return getCheckResult(exchange)
            .map(WhitelistCheckResult::isRateLimitWhitelisted)
            .orElse(false);
    }

    /**
     * 检查是否在路径前缀剥离白名单中
     */
    public static boolean isPrefixStripWhitelisted(ServerWebExchange exchange) {
        return getCheckResult(exchange)
            .map(WhitelistCheckResult::isPrefixStripWhitelisted)
            .orElse(false);
    }

    /**
     * 快速检查是否在任意白名单中
     */
    public static boolean isAnyWhitelisted(ServerWebExchange exchange) {
        return getCheckResult(exchange)
            .map(WhitelistCheckResult::isAnyWhitelisted)
            .orElse(false);
    }

    /**
     * 检查指定类型的白名单
     */
    public static boolean isWhitelisted(ServerWebExchange exchange, UnifiedWhitelistFilter.WhitelistType type) {
        return getCheckResult(exchange)
            .map(result -> result.isWhitelisted(type))
            .orElse(false);
    }

    /**
     * 清除白名单检查结果（通常不需要手动调用）
     */
    public static void clear(ServerWebExchange exchange) {
        exchange.getAttributes().remove(WHITELIST_CHECK_RESULT_KEY);
    }
}
