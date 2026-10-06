package com.carlos.auth.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * 登录租户校验配置
 * </p>
 *
 * <p>S2 租户管理：登录流程租户校验的 feature flag。
 * 默认关闭（{@code enabled=false}），关闭时登录行为与现状完全一致；
 * 开启后三个登录入口（/auth/user/login、/oauth2/token、OIDC formLogin）
 * 均按租户编码校验租户状态/有效期，并在租户内定位用户。</p>
 *
 * @author Carlos
 * @since 3.0.0
 * @date 2026年9月22日
 */
@Data
@ConfigurationProperties(prefix = "carlos.auth.tenant")
public class TenantProperties {

    /**
     * 是否启用登录租户校验（默认关闭，零行为变化）
     */
    private boolean enabled = false;

    /**
     * 未提供租户编码时的默认租户ID（0 为平台租户）
     */
    private Long defaultTenantId = 0L;

}
