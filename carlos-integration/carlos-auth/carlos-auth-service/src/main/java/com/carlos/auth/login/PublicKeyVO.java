package com.carlos.auth.login;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SM2 公钥响应 VO
 *
 * @author carlos
 * @version 1.0.0
 * @since 2026-07-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SM2 公钥响应")
public class PublicKeyVO {

    /**
     * 密钥 ID，用于后续解密时定位私钥
     */
    @Schema(description = "密钥 ID", example = "a1b2c3d4e5f6...")
    private String keyId;

    /**
     * SM2 公钥（Hex 格式，130 字符，含 04 前缀）
     */
    @Schema(description = "SM2 公钥（Hex）", example = "04a1b2c3...")
    private String publicKey;

    /**
     * 加密算法，固定为 SM2
     */
    @Schema(description = "加密算法", example = "SM2")
    private String algorithm;

    /**
     * 私钥有效期（秒）
     */
    @Schema(description = "私钥有效期（秒）", example = "300")
    private Long ttlSeconds;
}
