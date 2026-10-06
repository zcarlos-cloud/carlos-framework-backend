package com.carlos.auth.security.password;

import com.carlos.core.auth.SignatureUtil;
import com.carlos.encrypt.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 遗留 SM4 密码编码器
 *
 * <p><strong>⚠️ 仅用于迁移期解密旧密码，禁止用于新密码编码。</strong></p>
 *
 * <p>历史遗留问题：早期版本使用 SM4 对称加密存储密码，SM4 是双向可逆的对称加密，
 * 数据库泄露后所有密码明文可还原，违反 OWASP 密码存储规范。本类作为
 * {@link org.springframework.security.crypto.password.DelegatingPasswordEncoder}
 * 的兼容适配器，仅在用户首次登录时解密旧密码，验证通过后自动迁移到 Argon2id。</p>
 *
 * <p>迁移策略：</p>
 * <ol>
 *   <li>用户登录时，{@code DelegatingPasswordEncoder} 根据前缀 {@code {sm4}} 路由到本类</li>
 *   <li>本类调用 {@link EncryptUtil#decrypt(String)} 解密 SM4 密文，与明文比对</li>
 *   <li>验证通过后，调用方使用 {@link #upgradePassword} 用 Argon2id 重新编码并更新</li>
 *   <li>迁移完成后，旧 SM4 密文被覆盖，逐步消除对称加密存储</li>
 * </ol>
 *
 * <p><strong>注意：</strong>本类的 {@link #encode} 方法直接抛出异常，禁止用于新密码编码。
 * 新密码一律使用 {@code Argon2id}。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 * @see org.springframework.security.crypto.password.DelegatingPasswordEncoder
 * @see org.springframework.security.crypto.argon2.Argon2PasswordEncoder
 */
@Slf4j
public class LegacySm4PasswordEncoder implements PasswordEncoder {

    /**
     * SM4 编码前缀，用于 {@link org.springframework.security.crypto.password.DelegatingPasswordEncoder} 识别
     */
    public static final String PREFIX = "{sm4}";

    @Override
    public String encode(CharSequence rawPassword) {
        throw new UnsupportedOperationException(
            "LegacySm4PasswordEncoder 仅用于迁移期解密旧密码，禁止用于新密码编码。请使用 Argon2id。"
        );
    }

    /**
     * 校验明文密码与 SM4 密文是否匹配
     *
     * <p>支持两种格式：</p>
     * <ul>
     *   <li>带前缀：{@code {sm4}xxxxx}</li>
     *   <li>不带前缀：{@code xxxxx}（兼容历史数据）</li>
     * </ul>
     *
     * @param rawPassword    前端传入的明文密码（已解密 SM2 传输加密）
     * @param encodedPassword 数据库存储的 SM4 密文
     * @return true-匹配，false-不匹配或解密失败
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }

        String cipher = stripPrefix(encodedPassword);

        try {
            String decrypted = EncryptUtil.decrypt(cipher);
            if (decrypted == null || decrypted.isEmpty()) {
                return false;
            }
            // 使用常量时间比较，防时序侧信道（实现统一收敛到 SignatureUtil）
            return SignatureUtil.constantTimeEquals(rawPassword.toString(), decrypted);
        } catch (Exception e) {
            log.warn("SM4 密码解密失败，可能已损坏或格式错误: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 是否支持密码升级（迁移）
     *
     * <p>{@link org.springframework.security.crypto.password.DelegatingPasswordEncoder}
     * 在 {@code matches} 成功后会调用此方法判断是否需要升级。SM4 始终需要升级到 Argon2id。</p>
     *
     * @param encodedPassword 数据库存储的密文
     * @return true-始终需要升级
     */
    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return true;
    }

    /**
     * 去除前缀
     */
    private String stripPrefix(String encodedPassword) {
        if (encodedPassword.startsWith(PREFIX)) {
            return encodedPassword.substring(PREFIX.length());
        }
        return encodedPassword;
    }
}
