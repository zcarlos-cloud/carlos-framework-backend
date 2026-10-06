package com.carlos.license.verify;

import com.google.common.collect.Sets;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

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
@ConfigurationProperties(prefix = "carlos.license.verify")
public class LicenseVerifyProperties implements InitializingBean {

    /**
     * 是否启用 默认启用
     */
    private boolean enabled = true;

    /**
     * 证书 subject
     */
    private String subject;

    /**
     * 公钥别称
     */
    private String publicAlias;

    /**
     * 访问公钥库的密码
     */
    private String storePass;

    /**
     * 证书生成路径
     */
    private String licensePath;

    /**
     * 密钥库存储路径
     */
    private String publicKeysStorePath;

    /**
     * 验证内容 默认验证全部
     */
    private Set<ValidateContent> validateContents = Sets.newHashSet(ValidateContent.IP, ValidateContent.MAC, ValidateContent.CPU, ValidateContent.MAIN_BOARD);


    @Override
    public void afterPropertiesSet() {
        if (!enabled) {
            return;
        }
    }

}
