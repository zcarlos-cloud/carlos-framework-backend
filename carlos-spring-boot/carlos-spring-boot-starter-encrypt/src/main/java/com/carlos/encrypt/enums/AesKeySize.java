package com.carlos.encrypt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * AES 密钥长度枚举
 * </p>
 *
 * @author Carlos
 * @date 2025/3/16
 */
@Getter
@AllArgsConstructor
public enum AesKeySize {

    /**
     * 128 位密钥
     */
    BITS_128(128),

    /**
     * 192 位密钥
     */
    BITS_192(192),

    /**
     * 256 位密钥
     */
    BITS_256(256);

    private final int bits;

    /**
     * 获取字节长度
     *
     * @return 字节长度
     */
    public int getBytes() {
        return bits / 8;
    }
}
