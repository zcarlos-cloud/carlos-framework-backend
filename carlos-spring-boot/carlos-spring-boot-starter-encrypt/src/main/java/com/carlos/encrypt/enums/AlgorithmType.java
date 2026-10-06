package com.carlos.encrypt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 加密算法类型枚举
 * </p>
 *
 * @author Carlos
 * @date 2025/3/16
 */
@Getter
@AllArgsConstructor
public enum AlgorithmType {

    /**
     * SM2 国密非对称加密
     */
    SM2("SM2", "国密非对称加密", true),

    /**
     * SM3 国密哈希算法
     */
    SM3("SM3", "国密哈希算法", false),

    /**
     * SM4 国密对称加密
     */
    SM4("SM4", "国密对称加密", true),

    /**
     * AES 对称加密
     */
    AES("AES", "高级加密标准", true),

    /**
     * RSA 非对称加密
     */
    RSA("RSA", "非对称加密算法", true),

    /**
     * DES 对称加密
     */
    DES("DES", "数据加密标准", true),

    /**
     * 3DES 对称加密
     */
    DES3("3DES", "三重数据加密标准", true),

    /**
     * MD5 哈希算法（不推荐用于安全加密）
     */
    MD5("MD5", "消息摘要算法", false),

    /**
     * SHA-1 哈希算法
     */
    SHA1("SHA-1", "安全哈希算法1", false),

    /**
     * SHA-256 哈希算法
     */
    SHA256("SHA-256", "安全哈希算法256", false),

    /**
     * SHA-512 哈希算法
     */
    SHA512("SHA-512", "安全哈希算法512", false),

    /**
     * Base64 编码（非加密）
     */
    BASE64("Base64", "Base64编码", true);

    private final String code;
    private final String description;

    /**
     * 是否支持解密
     */
    private final boolean reversible;
}
