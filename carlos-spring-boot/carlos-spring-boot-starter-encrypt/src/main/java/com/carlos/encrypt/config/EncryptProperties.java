package com.carlos.encrypt.config;

import com.carlos.encrypt.EncryptMode;
import com.carlos.encrypt.StoreType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * <p>
 * 加密配置
 * </p>
 *
 * @author Carlos
 * @date 2023/3/22 13:26
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "carlos.encrypt")
public class EncryptProperties implements InitializingBean {


    /** sm2配置项 */
    private SM2Properties sm2 = new SM2Properties();
    /** sm4配置项 */
    private SM4Properties sm4 = new SM4Properties();


    @Override
    public void afterPropertiesSet() {
        log.info("encrypt config loaded, sm2 enabled: {}, sm4 enabled: {}, sm4 mode: {}",
            sm2.isEnabled(), sm4.isEnabled(), sm4.getEncryptMode());
        if (sm2.isEnabled()) {
            Assert.hasText(this.sm2.getPublicKey(), "'encrypt.sm2.public-key' must not be blank.");
            Assert.hasText(this.sm2.getPrivateKey(), "'encrypt.sm2.private-key' must not be blank.");
        }
        if (sm4.isEnabled()) {
            Assert.hasText(this.sm4.getKey(), "'encrypt.sm4.key' must not be blank.");
            if (sm4.getEncryptMode() == EncryptMode.CBC) {
                Assert.hasText(this.sm4.getIv(), "'encrypt.sm4.iv' must not be blank when CBC mode is used.");
            }
        }

    }


    @Data
    public static class SM2Properties {

        /**
         * 是否启用 默认不启用
         */
        private boolean enabled = false;

        /**
         * 公钥
         */
        private String publicKey;

        /**
         * 私钥
         */
        private String privateKey;


        /**
         * 密文存储形式 默认Base64
         */
        private StoreType storeType = StoreType.BASE64;

    }


    @Data
    public static class SM4Properties {

        /**
         * 是否启用 默认启用
         */
        private boolean enabled = true;

        /**
         * 秘钥
         */
        private String key;

        /**
         * iv向量 CBC模式需要
         */
        private String iv;
        /**
         * 加密模式
         */
        private EncryptMode encryptMode = EncryptMode.CBC;
        /**
         * 密文存储形式 默认Base64
         */
        private StoreType storeType = StoreType.BASE64;

    }

}
