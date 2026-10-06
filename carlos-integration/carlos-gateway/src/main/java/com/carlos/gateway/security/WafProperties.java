package com.carlos.gateway.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * WAF 配置属性
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 */
@Data
@ConfigurationProperties(prefix = "carlos.gateway.waf")
public class WafProperties {

    /**
     * 是否启用 WAF
     */
    private boolean enabled = true;

    /**
     * SQL 注入防护
     */
    private boolean sqlInjectionProtection = true;

    /**
     * XSS 防护
     */
    private boolean xssProtection = true;

    /**
     * 路径遍历防护
     */
    private boolean pathTraversalProtection = true;

    /**
     * 敏感文件访问防护
     */
    private boolean sensitiveFileProtection = false;

    /**
     * 命令注入防护
     */
    private boolean commandInjectionProtection = true;

    /**
     * 是否检查请求体
     */
    private boolean checkBody = true;

    /**
     * CSRF 防护
     */
    private boolean csrfProtection = true;

    /**
     * 允许的 Origin（用于 CSRF 检查）
     */
    private String allowedOrigin = "http://localhost";

    /**
     * 最大请求体检查大小（字节，默认 1MB）
     */
    private int maxBodySize = 1024 * 1024;
}
