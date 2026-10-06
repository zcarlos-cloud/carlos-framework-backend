package com.carlos.encrypt.key;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <p>
 * RSA 密钥对
 * </p>
 *
 * @author Carlos
 * @date 2025/3/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RsaKeyPair implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 公钥（Base64 编码）
     */
    private String publicKey;

    /**
     * 私钥（Base64 编码）
     */
    private String privateKey;

    /**
     * 密钥长度（位）
     */
    private int keySize;

    public RsaKeyPair(String publicKey, String privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.keySize = 2048;
    }
}
