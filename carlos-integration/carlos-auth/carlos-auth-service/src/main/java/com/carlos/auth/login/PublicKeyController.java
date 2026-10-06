package com.carlos.auth.login;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.SM2;
import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.core.response.Result;
import com.carlos.encrypt.EncryptUtil;
import com.carlos.encrypt.Sm2KeyPair;
import com.carlos.redis.util.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.GeneralSecurityException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 密码加密公钥接口
 *
 * <p>为前端登录页面提供一次性 SM2 公钥，用于对密码进行传输加密。</p>
 *
 * <h3>流程说明：</h3>
 * <ol>
 *   <li>前端调用 {@code GET /auth/public-key} 获取公钥和 keyId</li>
 *   <li>前端使用 SM2 公钥加密密码</li>
 *   <li>提交登录请求时附带 {@code keyId} 和 {@code encrypted=true}</li>
 *   <li>后端根据 keyId 从 Redis 取出私钥解密密码</li>
 *   <li>解密成功后立即删除 Redis 中的私钥（单次使用）</li>
 * </ol>
 *
 * @author carlos
 * @version 1.0.0
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "密码加密公钥", description = "动态 SM2 公钥下发")
public class PublicKeyController {

    /**
     * 私钥在 Redis 中的 TTL（秒）
     */
    private static final long KEY_TTL_SECONDS = 300;

    /**
     * 获取 SM2 公钥
     *
     * <p>每次请求都会生成新的 SM2 密钥对，私钥暂存 Redis，公钥返回给前端。</p>
     *
     * @param request HTTP 请求
     * @return 公钥、keyId、算法信息
     */
    @Operation(summary = "获取 SM2 公钥", description = "获取一次性 SM2 公钥用于密码传输加密")
    @GetMapping("/public-key")
    public Result<PublicKeyVO> getPublicKey(HttpServletRequest request) {
        try {
            Sm2KeyPair keyPair = EncryptUtil.generateSm2KeyPair();
            String keyId = generateKeyId();

            // 私钥写入 Redis，设置 TTL
            String privateKeyRedisKey = AuthRedisKey.passwordPrivateKey(keyId);
            RedisUtil.setValue(privateKeyRedisKey, keyPair.getPrivateKey(), KEY_TTL_SECONDS, TimeUnit.SECONDS);

            log.debug("Generated SM2 key pair, keyId={}, clientIp={}", keyId, getClientIp(request));

            return Result.success(PublicKeyVO.builder()
                .keyId(keyId)
                .publicKey(keyPair.getPublicKey())
                .algorithm("SM2")
                .ttlSeconds(KEY_TTL_SECONDS)
                .build());
        } catch (GeneralSecurityException e) {
            log.error("Failed to generate SM2 key pair", e);
            return Result.error("公钥生成失败");
        }
    }

    /**
     * 根据 keyId 和私钥解密 SM2 密文
     *
     * <p>工具方法，供过滤器/服务层调用。</p>
     *
     * @param keyId    密钥 ID
     * @param cipher   SM2 密文
     * @return 明文密码，解密失败返回 null
     */
    public static String decryptPassword(String keyId, String cipher) {
        if (keyId == null || keyId.isBlank() || cipher == null || cipher.isBlank()) {
            return null;
        }

        String privateKeyRedisKey = AuthRedisKey.passwordPrivateKey(keyId);
        String privateKey = RedisUtil.getValue(privateKeyRedisKey);

        if (privateKey == null || privateKey.isBlank()) {
            log.warn("Private key not found or expired, keyId={}", keyId);
            return null;
        }

        try {
            SM2 sm2 = SmUtil.sm2(privateKey, null);
            String plain = sm2.decryptStr(cipher, KeyType.PrivateKey);
            log.debug("SM2 password decrypted successfully, keyId={}", keyId);
            return plain;
        } catch (Exception e) {
            log.warn("SM2 password decryption failed, keyId={}", keyId, e);
            return null;
        } finally {
            // 无论成功失败都删除私钥，防止重放和暴力破解
            RedisUtil.delete(privateKeyRedisKey);
        }
    }

    /**
     * 生成唯一 keyId
     */
    private String generateKeyId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
