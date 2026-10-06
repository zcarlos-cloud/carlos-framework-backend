package com.carlos.sample.encrypt.pojo;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 加密数据示例类
 * </p>
 *
 * @author Carlos
 * @date 2026/3/15
 */
@Data
public class EncryptData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 原始数据
     */
    private String plainText;

    /**
     * SM4 加密后的数据
     */
    private String sm4Encrypted;

    /**
     * SM2 加密后的数据
     */
    private String sm2Encrypted;

    /**
     * SM4 解密后的数据
     */
    private String sm4Decrypted;

    /**
     * SM2 解密后的数据
     */
    private String sm2Decrypted;

    /**
     * SM2 公钥
     */
    private String sm2PublicKey;

    /**
     * SM2 私钥
     */
    private String sm2PrivateKey;
}
