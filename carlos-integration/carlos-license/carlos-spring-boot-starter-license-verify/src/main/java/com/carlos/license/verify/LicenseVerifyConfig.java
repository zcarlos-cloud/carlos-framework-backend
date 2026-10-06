package com.carlos.license.verify;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

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
@ConditionalOnProperty(prefix = "carlos.license.verify", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LicenseVerifyProperties.class)
public class LicenseVerifyConfig {


    /**
     * <p>项目名称: true-license-demo </p>
     * <p>文件名称: LicenseConfig.java </p>
     * <p>方法描述: 把 LicenseVerify 类添加到 Spring 容器。在 LicenseVerify 从 Spring 容器添加或移除的时候调用证书安装或卸载 </p>
     * <p>创建时间: 2025/04/10 16:07 </p>
     *
     * @return com.carlos.licensegenerate.LicenseVerify
     * @author Carlos
     * @version 1.0
     */
    @Bean(initMethod = "installLicense", destroyMethod = "unInstallLicense")
    public LicenseVerify licenseVerify(LicenseVerifyProperties properties) {
        return new LicenseVerify(properties.getSubject(), properties.getPublicAlias(), properties.getStorePass(), properties.getLicensePath(), properties.getPublicKeysStorePath());
    }
}
