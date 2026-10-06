package com.carlos.encrypt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * RSA 密钥长度枚举
 * </p>
 *
 * @author Carlos
 * @date 2025/3/16
 */
@Getter
@AllArgsConstructor
public enum RsaKeySize {

    /**
     * 1024 位密钥（不推荐，安全性较低）
     */
    BITS_1024(1024),

    /**
     * 2048 位密钥（推荐）
     */
    BITS_2048(2048),

    /**
     * 3072 位密钥
     */
    BITS_3072(3072),

    /**
     * 4096 位密钥（高安全级别）
     */
    BITS_4096(4096);

    private final int bits;
}
