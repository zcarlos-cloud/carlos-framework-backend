package com.carlos.license.generate;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 *   license验证配置
 * </p>
 *
 * @author Carlos
 * @date 2025-04-10 14:26
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "carlos.license.generate")
public class LicenseGenerateProperties implements InitializingBean {

    /**
     * 是否启用 默认关闭
     */
    private boolean enabled = false;

    /**
     * 密钥（私钥）库存储路径
     */
    private String filePath;


    @Override
    public void afterPropertiesSet() {
        if (enabled) {

        }
    }

}
