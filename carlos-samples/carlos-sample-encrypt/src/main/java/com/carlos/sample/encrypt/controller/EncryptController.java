package com.carlos.sample.encrypt.controller;

import com.carlos.core.response.Result;
import com.carlos.encrypt.EncryptUtil;
import com.carlos.encrypt.Sm2KeyPair;
import com.carlos.sample.encrypt.pojo.EncryptData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.GeneralSecurityException;

/**
 * <p>
 * 国密加密演示 Controller
 * 演示 SM2/SM4 加解密功能的使用
 * </p>
 *
 * @author Carlos
 * @date 2026/3/15
 */
@Slf4j
@RestController
@RequestMapping("/encrypt")
public class EncryptController {

    /**
     * SM4 对称加密
     *
     * @param plainText 原始文本
     * @return 加密后的结果
     */
    @GetMapping("/sm4/encrypt")
    public Result<String> sm4Encrypt(@RequestParam String plainText) {
        log.info("SM4 加密请求，原文: {}", plainText);
        String encrypted = EncryptUtil.encrypt(plainText);
        log.info("SM4 加密结果: {}", encrypted);
        return Result.success(encrypted);
    }

    /**
     * SM4 对称解密
     *
     * @param encryptedText 加密后的文本
     * @return 解密后的原文
     */
    @GetMapping("/sm4/decrypt")
    public Result<String> sm4Decrypt(@RequestParam String encryptedText) {
        log.info("SM4 解密请求，密文: {}", encryptedText);
        String decrypted = EncryptUtil.decrypt(encryptedText);
        log.info("SM4 解密结果: {}", decrypted);
        return Result.success(decrypted);
    }

    /**
     * SM4 加密（Hex 格式输出）
     *
     * @param plainText 原始文本
     * @return Hex 格式的加密结果
     */
    @GetMapping("/sm4/encrypt-hex")
    public Result<String> sm4EncryptHex(@RequestParam String plainText) {
        log.info("SM4 Hex 加密请求，原文: {}", plainText);
        String encrypted = EncryptUtil.sm4EncryptHex(plainText);
        log.info("SM4 Hex 加密结果: {}", encrypted);
        return Result.success(encrypted);
    }

    /**
     * SM4 加密（Base64 格式输出）
     *
     * @param plainText 原始文本
     * @return Base64 格式的加密结果
     */
    @GetMapping("/sm4/encrypt-base64")
    public Result<String> sm4EncryptBase64(@RequestParam String plainText) {
        log.info("SM4 Base64 加密请求，原文: {}", plainText);
        String encrypted = EncryptUtil.sm4EncryptBase64(plainText);
        log.info("SM4 Base64 加密结果: {}", encrypted);
        return Result.success(encrypted);
    }

    /**
     * SM2 非对称加密
     *
     * @param plainText 原始文本
     * @return 加密后的结果
     */
    @GetMapping("/sm2/encrypt")
    public Result<String> sm2Encrypt(@RequestParam String plainText) {
        log.info("SM2 加密请求，原文: {}", plainText);
        String encrypted = EncryptUtil.sm2Encrypt(plainText);
        log.info("SM2 加密结果: {}", encrypted);
        return Result.success(encrypted);
    }

    /**
     * SM2 非对称解密
     *
     * @param encryptedText 加密后的文本
     * @return 解密后的原文
     */
    @GetMapping("/sm2/decrypt")
    public Result<String> sm2Decrypt(@RequestParam String encryptedText) {
        log.info("SM2 解密请求，密文: {}", encryptedText);
        String decrypted = EncryptUtil.sm2Decrypt(encryptedText);
        log.info("SM2 解密结果: {}", decrypted);
        return Result.success(decrypted);
    }

    /**
     * SM2 加密（Hex 格式输出）
     *
     * @param plainText 原始文本
     * @return Hex 格式的加密结果
     */
    @GetMapping("/sm2/encrypt-hex")
    public Result<String> sm2EncryptHex(@RequestParam String plainText) {
        log.info("SM2 Hex 加密请求，原文: {}", plainText);
        String encrypted = EncryptUtil.sm2EncryptHex(plainText);
        log.info("SM2 Hex 加密结果: {}", encrypted);
        return Result.success(encrypted);
    }

    /**
     * SM2 加密（Base64 格式输出）
     *
     * @param plainText 原始文本
     * @return Base64 格式的加密结果
     */
    @GetMapping("/sm2/encrypt-base64")
    public Result<String> sm2EncryptBase64(@RequestParam String plainText) {
        log.info("SM2 Base64 加密请求，原文: {}", plainText);
        String encrypted = EncryptUtil.sm2EncryptBase64(plainText);
        log.info("SM2 Base64 加密结果: {}", encrypted);
        return Result.success(encrypted);
    }

    /**
     * 生成 SM2 密钥对
     *
     * @return 密钥对
     */
    @GetMapping("/sm2/generate-key-pair")
    public Result<EncryptData> generateSm2KeyPair() {
        log.info("生成 SM2 密钥对");
        try {
            Sm2KeyPair keyPair = EncryptUtil.generateSm2KeyPair();
            EncryptData data = new EncryptData();
            data.setSm2PublicKey(keyPair.getPublicKey());
            data.setSm2PrivateKey(keyPair.getPrivateKey());
            log.info("SM2 密钥对生成成功");
            return Result.success(data);
        } catch (GeneralSecurityException e) {
            log.error("生成 SM2 密钥对失败", e);
            return Result.error("生成密钥对失败: " + e.getMessage());
        }
    }

    /**
     * 完整加解密流程演示
     * 演示 SM4 和 SM2 的加解密全过程
     *
     * @param plainText 原始文本
     * @return 加解密结果
     */
    @PostMapping("/demo")
    public Result<EncryptData> demo(@RequestBody String plainText) {
        log.info("加解密演示请求，原文: {}", plainText);
        EncryptData result = new EncryptData();
        result.setPlainText(plainText);

        // SM4 加解密
        String sm4Encrypted = EncryptUtil.encrypt(plainText);
        result.setSm4Encrypted(sm4Encrypted);
        result.setSm4Decrypted(EncryptUtil.decrypt(sm4Encrypted));

        // SM2 加解密
        String sm2Encrypted = EncryptUtil.sm2Encrypt(plainText);
        result.setSm2Encrypted(sm2Encrypted);
        result.setSm2Decrypted(EncryptUtil.sm2Decrypt(sm2Encrypted));

        log.info("加解密演示完成");
        return Result.success(result);
    }

    /**
     * 健康检查接口
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("Carlos 加密示例服务运行正常");
    }
}
