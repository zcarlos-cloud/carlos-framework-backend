package com.carlos.license.generate;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * <p>
 *   证书验证配置
 * </p>
 *
 * @author Carlos
 * @date 2025-04-10 14:29
 */
@Data
@AutoConfiguration
@ConditionalOnProperty(prefix = "carlos.license.generate", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LicenseGenerateProperties.class)
public class LicenseGenerateConfig {


}
