package com.carlos.auth.security.password;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 密码安全配置属性
 *
 * <p>统一管理密码哈希算法参数与密码强度策略，遵循 NIST SP 800-63B 规范。</p>
 *
 * <p>配置前缀：{@code carlos.auth.password}</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
@Data
@ConfigurationProperties(prefix = "carlos.auth.password")
public class PasswordProperties {

    /**
     * Argon2id 参数（OWASP 2024 推荐）
     */
    private Argon2 argon2 = new Argon2();

    /**
     * 密码强度策略
     */
    private Strength strength = new Strength();

    /**
     * 是否启用遗留 SM4 密码自动迁移
     *
     * <p>启用后，用户使用旧 SM4 密文登录成功时，自动重新用 Argon2id 哈希并更新。
     * 全部用户迁移完成后应关闭。</p>
     */
    private boolean sm4MigrationEnabled = true;

    /**
     * Pepper（额外密钥，存于数据库之外，如环境变量）
     *
     * <p>数据库泄露时仍需破 pepper 才能爆破密码。生产环境通过环境变量注入。</p>
     */
    private String pepper;

    /**
     * Argon2id 哈希参数
     *
     * <p>OWASP 2024 推荐值：内存 ≥ 46MiB，迭代 ≥ 2，并行度 1，盐 16 字节，哈希 32 字节。</p>
     */
    @Data
    public static class Argon2 {
        /**
         * 盐长度（字节）
         */
        private int saltLength = 16;

        /**
         * 哈希长度（字节）
         */
        private int hashLength = 32;

        /**
         * 并行度
         */
        private int parallelism = 1;

        /**
         * 内存（KiB），46 MiB = 47104 KiB
         */
        private int memory = 47104;

        /**
         * 迭代次数
         */
        private int iterations = 3;
    }

    /**
     * 密码强度策略（NIST 800-63B）
     */
    @Data
    public static class Strength {
        /**
         * 最小长度
         */
        private int minLength = 8;

        /**
         * 最大长度
         */
        private int maxLength = 128;

        /**
         * 是否强制复杂度组合（NIST 800-63B 已不推荐）
         */
        private boolean requireComplexity = false;

        /**
         * 是否检查泄露密码（HaveIBeenPwned k-匿名查询）
         */
        private boolean checkPwned = true;

        /**
         * 历史密码禁重用次数
         */
        private int historyCount = 5;

        /**
         * 密码强制过期天数，0 表示不强制过期（NIST 推荐）
         */
        private int maxAgeDays = 0;

        /**
         * zxcvbn 强度估算最低得分（0-4）
         */
        private int minStrengthScore = 3;
    }
}
