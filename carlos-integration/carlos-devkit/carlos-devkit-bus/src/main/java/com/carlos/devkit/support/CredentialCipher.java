package com.carlos.devkit.support;

import com.carlos.encrypt.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 数据源口令加解密（SM4）
 * </p>
 * <p>
 * 委托 starter-encrypt 的 {@link EncryptUtil}（SM4-CBC）。明文口令只在
 * {@code encrypt} / {@code decrypt} 方法边界出现，密文不出 {@code DataSourceService} 边界，
 * 更不会进入任何 VO 或日志。
 * </p>
 * <p>
 * 注意：starter-encrypt 未配置 SM4 密钥（{@code carlos.encrypt.sm4.enabled=false}）时
 * EncryptUtil 原样返回输入 —— 即密文=明文，属「加密未启用」的显式降级，
 * <b>生产环境必须开启 carlos.encrypt.sm4</b>。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Component
public class CredentialCipher {

    /**
     * 明文口令加密为 SM4 密文
     *
     * @param plainPassword 明文口令（空值原样返回）
     * @return SM4 密文
     */
    public String encrypt(final String plainPassword) {
        return EncryptUtil.encrypt(plainPassword);
    }

    /**
     * SM4 密文解密为明文口令
     *
     * @param passwordCipher SM4 密文（空值原样返回）
     * @return 明文口令
     */
    public String decrypt(final String passwordCipher) {
        return EncryptUtil.decrypt(passwordCipher);
    }
}
