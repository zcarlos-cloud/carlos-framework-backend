package com.carlos.gateway.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * <p>
 * 防重放攻击配置属性
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 */
@Data
@ConfigurationProperties(prefix = "carlos.gateway.replay")
public class ReplayProtectionProperties {

    /**
     * 是否启用防重放
     */
    private boolean enabled = true;

    /**
     * 严格模式（必须验证签名）
     */
    private boolean strictMode = false;

    /**
     * 签名密钥
     */
    private String secretKey;

    /**
     * 白名单路径
     */
    private Set<String> whitelist;
}
