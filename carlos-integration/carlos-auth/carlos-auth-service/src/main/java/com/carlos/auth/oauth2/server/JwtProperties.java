package com.carlos.auth.oauth2.server;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * JWT 配置属性
 *
 * <p>配置 JWT Token 的签名算法、密钥信息、Issuer 等。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
@Data
public class JwtProperties {

    /**
     * JWT 签名算法
     */
    private String algorithm = "RS256";

    /**
     * RSA 私钥路径
     */
    private String privateKeyPath;

    /**
     * RSA 公钥路径
     */
    private String publicKeyPath;

    /**
     * 密钥 ID
     */
    private String keyId = "carlos-key";

    /**
     * JWT Issuer
     *
     * <p><strong>生产环境必须配置为实际域名，禁止使用默认值。</strong></p>
     */
    private String issuer;

    /**
     * JKS 密钥库路径
     */
    private String keyStore;

    /**
     * 密钥库密码
     */
    private String keyStorePassword;

    /**
     * 密钥别名
     */
    private String keyAlias = "auth-key";

    /**
     * 密钥密码
     */
    private String keyPassword;

    /**
     * 密钥长度
     */
    private Integer keySize = 2048;

    /**
     * Token 中是否包含用户信息
     */
    private boolean includeUserInfo = true;

    /**
     * Token 自定义声明
     */
    private Map<String, Object> customClaims = new HashMap<>();

}
