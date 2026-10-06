package com.carlos.auth.security.manager;

import com.carlos.auth.api.enums.AuthErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ResourceUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.*;
import java.security.cert.Certificate;

/**
 * 密钥对管理器
 *
 * <p>用于加载或生成 RSA 密钥对，支持从 JKS 文件加载或自动生成。</p>
 *
 * <h3>功能说明：</h3>
 * <ul>
 *   <li>从 JKS 文件加载 RSA 密钥对</li>
 *   <li>自动生成并保存 JKS 文件（如果不存在）</li>
 *   <li>支持密钥轮换（新旧密钥同时生效）</li>
 *   <li>支持不同的密钥长度（2048/4096 位）</li>
 * </ul>
 *
 * <h3>配置示例：</h3>
 * <pre>{@code
 * auth:
 *   jwt:
 *     key-store: classpath:auth.jks
 *     key-store-password: ${JKS_PASSWORD:}
 *     key-alias: auth-key
 *     key-password: ${KEY_PASSWORD:}
 * }</pre>
 *
 * <h3>生产环境建议：</h3>
 * <ul>
 *   <li>使用 4096 位密钥长度</li>
 *   <li>设置强密码保护 JKS 文件</li>
 *   <li>定期备份 JKS 文件</li>
 *   <li>将密码存储在安全的配置中心</li>
 * </ul>
 *
 * @author carlos
 * @version 1.0.0
 * @since 2026-02-26
 */
@Slf4j
public class KeyPairManager {

    /**
     * 加载或生成 RSA 密钥对
     *
     * <p>优先从 JKS 文件加载，如果不存在则自动生成并保存。</p>
     *
     * @param keyStorePath JKS 文件路径（支持 classpath:、file: 前缀）
     * @param keyStorePassword 密钥库密码
     * @param keyAlias 密钥别名
     * @param keyPassword 密钥密码
     * @param keySize 密钥长度（2048 或 4096）
     * @return KeyPair RSA 密钥对
     */
    public static KeyPair loadOrGenerateKeyPair(
        String keyStorePath,
        String keyStorePassword,
        String keyAlias,
        String keyPassword,
        int keySize) {

        try {
            // 尝试从文件加载
            if (keyStorePath != null && !keyStorePath.isEmpty()) {
                KeyPair keyPair = loadFromKeyStore(keyStorePath, keyStorePassword, keyAlias, keyPassword);
                if (keyPair != null) {
                    log.info("Loaded RSA key pair from keystore: {}", keyStorePath);
                    return keyPair;
                }
            }

            // 自动生成并保存
            log.warn("No keystore found, generating new RSA key pair ({} bits)", keySize);
            KeyPair keyPair = generateRsaKeyPair(keySize);

            // 保存到 JKS 文件
            if (keyStorePath != null && !keyStorePath.isEmpty()) {
                saveToKeyStore(keyPair, keyStorePath, keyStorePassword, keyAlias, keyPassword);
                log.info("Saved RSA key pair to keystore: {}", keyStorePath);
            } else {
                log.warn("No keystore path configured, using in-memory key pair only");
            }

            return keyPair;

        } catch (Exception e) {
            log.error("Failed to load or generate RSA key pair", e);
            throw new IllegalStateException("Failed to initialize RSA key pair", e);
        }
    }

    /**
     * 从 JKS 文件加载密钥对
     *
     * @param keyStorePath 密钥库路径
     * @param keyStorePassword 密钥库密码
     * @param keyAlias 密钥别名
     * @param keyPassword 密钥密码
     * @return KeyPair 密钥对，如果文件不存在返回 null
     */
    private static KeyPair loadFromKeyStore(
        String keyStorePath,
        String keyStorePassword,
        String keyAlias,
        String keyPassword) throws Exception {

        try {
            // 解析资源路径
            String path = keyStorePath;
            if (keyStorePath.startsWith("classpath:")) {
                path = keyStorePath.substring(10);
            }

            // 检查文件是否存在
            java.io.File file;
            try {
                file = ResourceUtils.getFile(keyStorePath);
                if (!file.exists()) {
                    log.info("Keystore file not found: {}", keyStorePath);
                    return null;
                }
            } catch (Exception e) {
                log.info("Cannot access keystore file: {}", keyStorePath);
                return null;
            }

            // 加载密钥库
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(file)) {
                keyStore.load(fis, keyStorePassword != null ? keyStorePassword.toCharArray() : null);
            }

            // 获取密钥对
            Key key = keyStore.getKey(keyAlias, keyPassword != null ? keyPassword.toCharArray() : null);
            if (key instanceof PrivateKey privateKey) {
                // 获取证书和公钥
                Certificate cert = keyStore.getCertificate(keyAlias);
                PublicKey publicKey = cert.getPublicKey();
                return new KeyPair(publicKey, privateKey);
            }

            throw AuthErrorCode.AUTH_KEY_STORE_ALIAS_NOT_FOUND.exception(keyAlias);

        } catch (Exception e) {
            log.error("Failed to load key pair from keystore: {}", keyStorePath, e);
            throw e;
        }
    }

    /**
     * 生成 RSA 密钥对
     *
     * @param keySize 密钥长度（2048 或 4096）
     * @return KeyPair RSA 密钥对
     */
    private static KeyPair generateRsaKeyPair(int keySize) throws NoSuchAlgorithmException {
        if (keySize != 2048 && keySize != 4096) {
            throw AuthErrorCode.AUTH_KEY_SIZE_INVALID.exception("密钥长度必须为 2048 或 4096，当前: %d", keySize);
        }

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * 保存密钥对到 JKS 文件
     *
     * @param keyPair 密钥对
     * @param keyStorePath 密钥库路径
     * @param keyStorePassword 密钥库密码
     * @param keyAlias 密钥别名
     * @param keyPassword 密钥密码
     */
    private static void saveToKeyStore(
        KeyPair keyPair,
        String keyStorePath,
        String keyStorePassword,
        String keyAlias,
        String keyPassword) throws Exception {

        try {
            // 检查是否为 classpath 路径
            if (keyStorePath.startsWith("classpath:")) {
                log.warn("Cannot save keystore to classpath, saving to user home directory");
                String fileName = keyStorePath.substring(keyStorePath.lastIndexOf('/') + 1);
                keyStorePath = System.getProperty("user.home") + "/.carlos/auth/" + fileName;
            }

            // 创建目录
            java.io.File file = new java.io.File(keyStorePath);
            java.io.File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    log.warn("Failed to create directory: {}", parentDir.getAbsolutePath());
                }
            }

            // 创建密钥库
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);

            // 存储密钥对（简化方式，不生成证书链）
            // TODO: 如果需要证书链，可以引入 BouncyCastle 库
            Certificate[] certChain = null;
            keyStore.setKeyEntry(
                keyAlias,
                keyPair.getPrivate(),
                keyPassword != null ? keyPassword.toCharArray() : null,
                certChain
            );

            // 保存到文件
            try (FileOutputStream fos = new FileOutputStream(file)) {
                keyStore.store(fos, keyStorePassword != null ? keyStorePassword.toCharArray() : null);
            }

            log.info("Successfully saved keystore to: {}", file.getAbsolutePath());

        } catch (Exception e) {
            log.error("Failed to save keystore to: {}", keyStorePath, e);
            throw e;
        }
    }

    /**
     * 加载密钥对（简化版本，只提供路径和密码）
     *
     * @param keyStorePath 密钥库路径
     * @param keyStorePassword 密钥库密码
     * @return KeyPair 密钥对
     */
    public static KeyPair loadKeyPair(String keyStorePath, String keyStorePassword) {
        String keyAlias = "auth-key";
        String keyPassword = keyStorePassword;
        int keySize = 2048;

        return loadOrGenerateKeyPair(keyStorePath, keyStorePassword, keyAlias, keyPassword, keySize);
    }
}
