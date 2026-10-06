package com.carlos.encrypt.config;


import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.symmetric.SM4;
import com.carlos.core.exception.ComponentException;
import com.carlos.encrypt.EncryptMode;
import com.carlos.encrypt.EncryptUtil;
import com.carlos.encrypt.health.EncryptHealthIndicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;

import java.nio.charset.StandardCharsets;

/**
 * <p>
 * 加密配置
 * </p>
 *
 * @author Carlos
 * @date 2023/3/22 13:25
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(EncryptProperties.class)
public class EncryptConfig {

    /**
     * 注册加密算法
     *
     * <p><strong>安全修复 (P0-006)：</strong></p>
     * <ul>
     *   <li>强制使用 UTF-8 编码，避免平台默认编码导致密钥不一致</li>
     *   <li>禁用 ECB 模式（不提供语义安全），强制使用 CBC</li>
     * </ul>
     *
     * @return cn.hutool.crypto.symmetric.SM4
     * @author Carlos
     * @date 2023/3/22 14:11
     */
    @Bean
    @ConditionalOnProperty(prefix = "carlos.encrypt.sm4", name = "enabled", havingValue = "true")
    public SM4 sm4(EncryptProperties properties) {
        EncryptProperties.SM4Properties sm4Config = properties.getSm4();
        String key = sm4Config.getKey();
        if (StrUtil.isBlank(key)) {
            throw new ComponentException("sm4 key is null or empty");
        }
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16) {
            throw new ComponentException("sm4 key must be exactly 16 bytes, current length: " + keyBytes.length);
        }
        EncryptMode encryptMode = sm4Config.getEncryptMode();

        // P0-006 修复：禁用 ECB 模式，强制使用 CBC
        if (encryptMode == EncryptMode.ECB) {
            throw new ComponentException("ECB mode is insecure and has been disabled. Use CBC mode instead.");
        }

        SM4 sm4 = null;
        if (encryptMode == EncryptMode.CBC) {
            String iv = sm4Config.getIv();
            if (StrUtil.isBlank(iv)) {
                throw new ComponentException("sm4 iv is null or empty");
            }
            byte[] ivBytes = iv.getBytes(StandardCharsets.UTF_8);
            if (ivBytes.length != 16) {
                throw new ComponentException("sm4 iv must be exactly 16 bytes, current length: " + ivBytes.length);
            }
            sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, keyBytes, ivBytes);
        }
        return sm4;
    }


    /**
     * 注册SM2
     *
     * @return cn.hutool.crypto.asymmetric.SM2
     * @author Carlos
     * @date 2023/3/22 14:11
     */
    @Bean
    @ConditionalOnProperty(prefix = "carlos.encrypt.sm2", name = "enabled", havingValue = "true")
    public SM2 sm2(EncryptProperties properties) {
        EncryptProperties.SM2Properties sm2 = properties.getSm2();
        if (StrUtil.isBlank(sm2.getPrivateKey())) {
            throw new ComponentException("sm2 privateKey is null");
        }
        if (StrUtil.isBlank(sm2.getPublicKey())) {
            throw new ComponentException("sm2 publicKey is null");
        }
        return SmUtil.sm2(sm2.getPrivateKey(), sm2.getPublicKey());
    }

    /**
     * 加密工具注册
     */
    @Bean
    public EncryptUtil encryptUtil(@Nullable SM4 sm4, @Nullable SM2 sm2, EncryptProperties encryptProperties) {
        return new EncryptUtil(sm4, sm2, encryptProperties);
    }

    /**
     * 加密组件健康检查指示器
     */
    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    public EncryptHealthIndicator encryptHealthIndicator(EncryptProperties encryptProperties) {
        return new EncryptHealthIndicator(encryptProperties);
    }

}
