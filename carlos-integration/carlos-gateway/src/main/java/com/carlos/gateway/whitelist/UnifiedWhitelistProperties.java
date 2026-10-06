package com.carlos.gateway.whitelist;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * 统一白名单配置属性
 * 集中管理所有过滤器的白名单路径，避免重复检查
 * </p>
 *
 * @author carlos
 * @date 2026/4/10
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "carlos.gateway.whitelist")
public class UnifiedWhitelistProperties implements InitializingBean {

    /**
     * 是否启用统一白名单检查
     */
    private boolean enabled = true;

    /**
     * 全局白名单路径（适用于所有过滤器）
     */
    private Set<String> globalPaths = new HashSet<>();

    /**
     * 认证/鉴权白名单（OAuth2、JWT 等跳过）
     */
    private Set<String> authPaths = new HashSet<>();

    /**
     * WAF 白名单（安全防护跳过）
     */
    private Set<String> wafPaths = new HashSet<>();

    /**
     * 防重放攻击白名单
     */
    private Set<String> replayPaths = new HashSet<>();

    /**
     * 限流白名单
     */
    private Set<String> rateLimitPaths = new HashSet<>();

    /**
     * 路径前缀剥离白名单
     */
    private Set<String> prefixStripPaths = new HashSet<>();

    // ========== 默认白名单 ==========

    /**
     * 默认全局白名单（健康检查、静态资源等）
     */
    private static final Set<String> DEFAULT_GLOBAL_PATHS = Set.of(
            "/actuator/**",
            "/actuator",
            "/health",
            "/health/**",
            "/info",
            "/metrics",
            "/prometheus",
            "/**/favicon.ico",
            "/robots.txt",
            "/**/swagger-ui.html",
            "/**/swagger-ui/**",
            "/**/swagger-resources/**",
            "/**/v3/api-docs/**",
            "/**/v3/api-docs",
            "/**/doc.html",
            "/**/webjars/**",
            "/**/error"
    );

    /**
     * 默认认证白名单（登录、授权相关）
     *
     * <p>⚠️ 禁止加入「任意深度 + public 段」的宽松模式：PG 默认 schema 名就是 public，
     * 元数据/业务路径一旦包含 /public/ 段（如 /monitor/db/{"{id}"}/metadata/public/tables）
     * 会被误判白名单而跳过认证，身份断言不再签发，下游 @PreAuthorize 表现为莫名其妙的
     * 50001 Access Denied（2026-10-02 实测踩坑）。需要匿名公开端点请在配置里显式声明。</p>
     */
    private static final Set<String> DEFAULT_AUTH_PATHS = Set.of(
            "/**/oauth2/authorize",
            "/**/oauth2/token",
            "/**/oauth2/introspect",
            "/**/oauth2/revoke",
            "/**/oauth2/jwks",
            "/**/auth/public-key",
            "/**/auth/user/login",
            "/**/auth/user/pre-check",
            "/**/auth/user/logout",
            "/**/auth/captcha/**",
            "/**/auth/mfa/step-up-verify",
            "/**/auth/login/**"
    );

    /**
     * 默认 WAF 白名单（允许访问的敏感资源）
     */
    private static final Set<String> DEFAULT_WAF_PATHS = Set.of();

    @Override
    public void afterPropertiesSet() {
        // 合并默认白名单
        Set<String> mergedGlobal = new HashSet<>(DEFAULT_GLOBAL_PATHS);
        mergedGlobal.addAll(globalPaths);
        this.globalPaths = mergedGlobal;

        Set<String> mergedAuth = new HashSet<>(DEFAULT_AUTH_PATHS);
        mergedAuth.addAll(authPaths);
        this.authPaths = mergedAuth;

        // WAF 默认使用全局白名单
        Set<String> mergedWaf = new HashSet<>(DEFAULT_GLOBAL_PATHS);
        mergedWaf.addAll(wafPaths);
        this.wafPaths = mergedWaf;

        // 其他类型默认继承全局白名单
        if (replayPaths.isEmpty()) {
            this.replayPaths = mergedGlobal;
        } else {
            Set<String> mergedReplay = new HashSet<>(mergedGlobal);
            mergedReplay.addAll(replayPaths);
            this.replayPaths = mergedReplay;
        }

        if (rateLimitPaths.isEmpty()) {
            this.rateLimitPaths = mergedGlobal;
        } else {
            Set<String> mergedRateLimit = new HashSet<>(mergedGlobal);
            mergedRateLimit.addAll(rateLimitPaths);
            this.rateLimitPaths = mergedRateLimit;
        }

        if (prefixStripPaths.isEmpty()) {
            this.prefixStripPaths = mergedGlobal;
        } else {
            Set<String> mergedPrefix = new HashSet<>(mergedGlobal);
            mergedPrefix.addAll(prefixStripPaths);
            this.prefixStripPaths = mergedPrefix;
        }

        if (log.isInfoEnabled()) {
            log.info("Unified whitelist initialized: global={}, auth={}, waf={}, replay={}, rateLimit={}, prefixStrip={}",
                    globalPaths.size(), authPaths.size(), wafPaths.size(),
                    replayPaths.size(), rateLimitPaths.size(), prefixStripPaths.size());
        }
    }

    /**
     * 获取所有类型的白名单（去重）
     */
    public Set<String> getAllWhitelistPaths() {
        Set<String> all = new HashSet<>();
        all.addAll(globalPaths);
        all.addAll(authPaths);
        all.addAll(wafPaths);
        all.addAll(replayPaths);
        all.addAll(rateLimitPaths);
        all.addAll(prefixStripPaths);
        return all;
    }
}
