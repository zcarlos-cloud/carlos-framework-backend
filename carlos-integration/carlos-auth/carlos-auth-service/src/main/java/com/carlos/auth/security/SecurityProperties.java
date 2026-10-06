package com.carlos.auth.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;

/**
 * 安全配置属性
 *
 * <p>密码加密、登录限制等安全相关配置。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
@Data
@ConfigurationProperties(prefix = "carlos.auth.security")
public class SecurityProperties {

    /**
     * 密码编码器类型
     */
    private PasswordEncoderType passwordEncoder = PasswordEncoderType.BCRYPT;

    /**
     * 登录失败限制
     */
    @NestedConfigurationProperty
    private LoginLimitProperties loginLimit = new LoginLimitProperties();

    /**
     * IP 封禁配置
     */
    @NestedConfigurationProperty
    private IpBlockProperties ipBlock = new IpBlockProperties();

    /**
     * 登录限制配置
     */
    @Data
    public static class LoginLimitProperties {

        /**
         * 是否启用登录限制
         */
        private boolean enabled = true;

        /**
         * 最大失败次数
         */
        private int maxAttempts = 5;

        /**
         * 锁定时间
         */
        private Duration lockDuration = Duration.ofMinutes(30);
    }

    /**
     * IP 封禁配置
     */
    @Data
    public static class IpBlockProperties {

        /**
         * 是否启用 IP 封禁
         */
        private boolean enabled = true;

        /**
         * 最大失败次数（默认 20 次）
         */
        private int maxAttempts = 20;

        /**
         * 时间窗口（默认 5 分钟）
         */
        private Duration timeWindow = Duration.ofMinutes(5);

        /**
         * 封禁时长（默认 1 小时）
         */
        private Duration blockDuration = Duration.ofHours(1);
    }

    public enum PasswordEncoderType {
        BCRYPT
    }

}
