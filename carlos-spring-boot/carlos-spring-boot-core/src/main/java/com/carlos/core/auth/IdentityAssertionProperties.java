package com.carlos.core.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * <p>
 * 身份断言配置（网关与全部下游服务共用）
 * </p>
 *
 * <p>放在 <b>Nacos 共享配置 {@code common.yml}</b> 里由网关与所有下游服务共同读取，从根上保证
 * 「签名方」与「验签方」用的密钥、生命周期、时钟容差完全一致。</p>
 *
 * <pre>
 * carlos:
 *   identity-assertion:
 *     sign-enabled: true        # 网关是否签发断言
 *     verify-enabled: true      # 下游是否接受断言
 *     secret: xxx               # 共享密钥，32 字节以上随机值
 *     previous-secret: ""       # 密钥轮换宽限期用（仅验签接受）
 *     ttl: 5m
 *     clock-skew: 60s
 *     max-lifetime: 10m
 * </pre>
 *
 * <p><b>两个开关默认值的方向是刻意选的：</b>{@code sign-enabled} 默认 {@code false}
 * （网关不主动签发，需要显式开启），{@code verify-enabled} 默认 {@code true}
 * （下游接受断言，但没有断言时行为不变）。这样灰度切换只需在 Nacos 改一个键，且任何一侧
 * 单独回滚都不会让服务不可用。</p>
 *
 * <p><b>开关是在请求时读取的，不是在装配时用 {@code @ConditionalOnProperty} 判定的</b> ——
 * 条件化 Bean 不会随配置刷新而重建，用它在灰度期会造成「改了配置不生效」的假象。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-09-23
 */
@Data
@ConfigurationProperties(prefix = "carlos.identity-assertion")
public class IdentityAssertionProperties {

    /**
     * 是否签发身份断言（网关侧读取）
     */
    private boolean signEnabled = false;

    /**
     * 是否接受身份断言（下游侧读取）
     */
    private boolean verifyEnabled = true;

    /**
     * 签名密钥
     */
    private String secret;

    /**
     * 旧签名密钥（密钥轮换宽限期：当前密钥验不过时再试它）
     */
    private String previousSecret;

    /**
     * 断言有效期
     */
    private Duration ttl = Duration.ofMinutes(5);

    /**
     * 允许的时钟偏移容差（签发方与验签方时钟不一致时用）
     */
    private Duration clockSkew = Duration.ofSeconds(60);

    /**
     * 断言生命期上限：{@code exp - iat} 超过它即判为非法，防止有人用超长有效期绕过撤销
     */
    private Duration maxLifetime = Duration.ofMinutes(10);

    /**
     * 密钥是否可用
     */
    public boolean hasSecret() {
        return secret != null && !secret.isBlank();
    }

}
