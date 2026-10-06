package com.carlos.auth.oauth2.config;

import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.security.password.LegacySm4PasswordEncoder;
import com.carlos.auth.security.password.PasswordProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * 密码编码器配置
 *
 * <p>使用 {@link DelegatingPasswordEncoder} 实现多算法兼容与渐进式迁移策略。</p>
 *
 * <h3>密码存储格式：</h3>
 * <pre>
 * {argon2}$argon2id$v=19$m=47104,t=3,p=1$xxx$yyy  → 新格式（默认）
 * {bcrypt}$2a$10$xxxxx                              → 过渡格式
 * {sm4}xxxxx                                        → 旧格式（仅解密，登录后自动迁移）
 * {noop}plaintext                                   → 明文（仅开发环境）
 * </pre>
 *
 * <h3>迁移策略：</h3>
 * <p>用户下次登录时，{@code DelegatingPasswordEncoder} 验证旧格式密码正确后，
 * 通过 {@code upgradeEncoding} 机制触发密码升级，自动用 Argon2id 重新哈希并更新。
 * 无需强制所有用户改密。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 * @see PasswordProperties
 * @see LegacySm4PasswordEncoder
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({OAuth2Properties.class, PasswordProperties.class})
@ConditionalOnProperty(
    prefix = "carlos.auth.authorization-server",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class OAuth2PasswordEncoderConfig {

    /**
     * 密码编码器 - DelegatingPasswordEncoder
     *
     * <p>默认使用 Argon2id（OWASP 2024 首选），兼容 BCrypt 和遗留 SM4。</p>
     *
     * @param passwordProperties 密码配置属性
     * @return PasswordEncoder 委托密码编码器
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder(PasswordProperties passwordProperties) {
        PasswordProperties.Argon2 argon2Config = passwordProperties.getArgon2();

        // 构建 Argon2id 编码器（OWASP 2024 推荐参数）
        Argon2PasswordEncoder argon2Encoder = new Argon2PasswordEncoder(
            argon2Config.getSaltLength(),
            argon2Config.getHashLength(),
            argon2Config.getParallelism(),
            argon2Config.getMemory(),
            argon2Config.getIterations()
        );

        // 注册所有支持的编码器
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("argon2", argon2Encoder);
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("sm4", new LegacySm4PasswordEncoder());
        encoders.put("noop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance());

        // 默认使用 argon2 编码新密码
        DelegatingPasswordEncoder delegating = new DelegatingPasswordEncoder("argon2", encoders);
        // 兜底：如果密码没有前缀，尝试用 BCrypt 匹配（兼容 Spring Security 早期无前缀的 BCrypt 密文）
        delegating.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());

        log.info("Password encoder configured: default=Argon2id (memory={}KiB, iterations={}, parallelism={}), " +
                "with BCrypt/SM4/noop fallback",
            argon2Config.getMemory(), argon2Config.getIterations(), argon2Config.getParallelism());

        return delegating;
    }
}
