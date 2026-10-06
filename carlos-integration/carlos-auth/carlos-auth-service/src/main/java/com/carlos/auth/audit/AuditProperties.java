package com.carlos.auth.audit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * carlos-auth 的审计日志投递配置
 * </p>
 * <p>
 * 前缀 {@code carlos.auth.audit}。注册方式遵循本模块约定：
 * 在 {@code @Configuration} 类上用 {@code @EnableConfigurationProperties}
 * （见 {@link AuditMqConfig}），不在本类上加 {@code @Component}。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
@ConfigurationProperties(prefix = "carlos.auth.audit")
public class AuditProperties {

    /**
     * 投递方式，默认 {@link AuditMode#MQ}
     */
    private AuditMode mode = AuditMode.MQ;
}
