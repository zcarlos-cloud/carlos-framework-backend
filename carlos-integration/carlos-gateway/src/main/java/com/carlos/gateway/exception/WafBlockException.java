package com.carlos.gateway.exception;

import com.carlos.core.response.CommonErrorCode;

/**
 * <p>
 * WAF 拦截异常
 * 用于处理请求被 Web 应用防火墙拦截的场景
 * 返回 HTTP 403 Forbidden
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
public class WafBlockException extends GatewayException {

    private static final long serialVersionUID = 1L;

    /**
     * 拦截规则类型
     */
    private final String ruleType;

    /**
     * 拦截规则名称
     */
    private final String ruleName;

    /**
     * 匹配的恶意内容
     */
    private final String matchedContent;

    public WafBlockException(String message) {
        super(CommonErrorCode.FORBIDDEN, message);
        this.ruleType = "unknown";
        this.ruleName = "unknown";
        this.matchedContent = null;
    }

    public WafBlockException(String message, String ruleType, String ruleName) {
        super(CommonErrorCode.FORBIDDEN, message);
        this.ruleType = ruleType;
        this.ruleName = ruleName;
        this.matchedContent = null;
    }

    public WafBlockException(String message, String ruleType, String ruleName, String matchedContent) {
        super(CommonErrorCode.FORBIDDEN, message);
        this.ruleType = ruleType;
        this.ruleName = ruleName;
        this.matchedContent = matchedContent;
    }

    public String getRuleType() {
        return ruleType;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getMatchedContent() {
        return matchedContent;
    }
}
