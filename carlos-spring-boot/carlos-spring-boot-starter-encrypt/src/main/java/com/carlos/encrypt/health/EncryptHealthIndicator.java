package com.carlos.encrypt.health;

import com.carlos.encrypt.config.EncryptProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * 加密组件健康检查指示器
 *
 * @author Carlos
 * @date 2026-05-23
 */
@Slf4j
@RequiredArgsConstructor
public class EncryptHealthIndicator implements HealthIndicator {

    private final EncryptProperties encryptProperties;

    @Override
    public Health health() {
        try {
            EncryptProperties.SM4Properties sm4 = encryptProperties.getSm4();
            EncryptProperties.SM2Properties sm2 = encryptProperties.getSm2();

            boolean sm4Enabled = sm4 != null && sm4.isEnabled();
            boolean sm2Enabled = sm2 != null && sm2.isEnabled();

            Health.Builder builder = Health.up();
            builder.withDetail("sm4.enabled", sm4Enabled);
            builder.withDetail("sm2.enabled", sm2Enabled);

            if (sm4Enabled) {
                builder.withDetail("sm4.mode", sm4.getEncryptMode());
            }
            if (sm2Enabled) {
                builder.withDetail("sm2.keyConfigured",
                    sm2.getPublicKey() != null && !sm2.getPublicKey().isEmpty());
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Encrypt health check failed", e);
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
