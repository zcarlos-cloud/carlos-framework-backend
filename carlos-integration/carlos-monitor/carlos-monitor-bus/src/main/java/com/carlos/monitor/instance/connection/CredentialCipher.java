package com.carlos.monitor.instance.connection;

import com.carlos.encrypt.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 实例凭据加解密（SM4）
 * 委托 starter-encrypt 的 {@link EncryptUtil}（SM4-CBC），仅在本包内使用；
 * 明文密码只在 encrypt/decrypt 方法边界出现，passwordCipher 密文不出本包外的任何 VO/日志
 * </p>
 * <p>
 * 注意：starter-encrypt 未配置 SM4 密钥（carlos.encrypt.sm4.enabled=false）时
 * EncryptUtil 原样返回输入 —— 即密文=明文，属于「加密未启用」的显式降级，
 * 生产环境必须开启 carlos.encrypt.sm4
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@Component
public class CredentialCipher {

    /**
     * 明文密码加密为 SM4 密文
     *
     * @param plainPassword 明文密码
     * @return SM4 密文（原样返回空值）
     */
    public String encrypt(String plainPassword) {
        return EncryptUtil.encrypt(plainPassword);
    }

    /**
     * SM4 密文解密为明文密码
     *
     * @param passwordCipher SM4 密文
     * @return 明文密码（原样返回空值）
     */
    public String decrypt(String passwordCipher) {
        return EncryptUtil.decrypt(passwordCipher);
    }
}
