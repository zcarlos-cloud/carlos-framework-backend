package com.carlos.auth.config;

import com.carlos.auth.oauth2.client.config.ClientAuthProperties;
import com.carlos.auth.oauth2.server.AuthorizationServerProperties;
import com.carlos.auth.oauth2.server.JwtProperties;
import com.carlos.auth.security.SecurityProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Carlos OAuth2 统一配置属性
 *
 * <p>本配置类提供了 OAuth2 授权服务器和资源服务器的完整配置选项。
 * 各子配置按功能模块分散在对应包中，实现高内聚：</p>
 * <ul>
 *   <li>授权服务器配置 → {@link com.carlos.auth.oauth2.server.AuthorizationServerProperties}</li>
 *   <li>JWT 配置 → {@link com.carlos.auth.oauth2.server.JwtProperties}</li>
 *   <li>客户端配置 → {@link com.carlos.auth.oauth2.client.OAuth2ClientProperties}</li>
 *   <li>客户端认证配置 → {@link ClientAuthProperties}</li>
 *   <li>安全配置 → {@link com.carlos.auth.security.SecurityProperties}</li>
 * </ul>
 *
 * <p><strong>配置前缀：</strong>{@code carlos.auth}</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-02-22
 */
@Data
@ConfigurationProperties(prefix = "carlos.auth")
public class OAuth2Properties {

    /**
     * 是否启用 OAuth2 功能
     */
    private boolean enabled = true;

    /**
     * 授权服务器配置
     */
    @NestedConfigurationProperty
    private AuthorizationServerProperties authorizationServer = new AuthorizationServerProperties();

    /**
     * JWT 配置
     */
    @NestedConfigurationProperty
    private JwtProperties jwt = new JwtProperties();

    /**
     * 安全配置
     */
    @NestedConfigurationProperty
    private SecurityProperties security = new SecurityProperties();

    /**
     * 客户端认证配置
     */
    @NestedConfigurationProperty
    private ClientAuthProperties client = new ClientAuthProperties();

    /**
     * 登录配置
     */
    @NestedConfigurationProperty
    private Login login = new Login();

    /**
     * 告警通知配置
     */
    @NestedConfigurationProperty
    private AlertNotification alertNotification = new AlertNotification();

    /**
     * SSO 单点登录配置
     */
    @NestedConfigurationProperty
    private Sso sso = new Sso();

    @Data
    public static class Login {
        /**
         * 默认客户端ID
         */
        private String defaultClientId = "carlos-client";

        /**
         * 访问令牌有效期（秒）- 15分钟，遵循业界最佳实践
         */
        private long accessTokenTtl = 900;

        /**
         * 刷新令牌有效期（秒）- 7天，滑动续期
         */
        private long refreshTokenTtl = 604800;

        /**
         * 是否启用内部登录端点 /auth/login
         *
         * <p>关闭后，客户端应使用标准 OAuth2 /oauth2/token 端点获取令牌。</p>
         */
        private boolean internalEndpointEnabled = true;

        /**
         * 登录方式开关配置
         *
         * <p>控制 SSO 登录页面展示哪些登录方式。未启用的方式不会在页面上渲染。</p>
         */
        @NestedConfigurationProperty
        private Methods methods = new Methods();
    }

    /**
     * 登录方式开关配置
     *
     * <p>控制 SSO 登录页面各登录方式的可见性，包含：</p>
     * <ul>
     *   <li>本地登录：账号密码、短信验证码、扫码登录、Passkey</li>
     *   <li>第三方社交登录：钉钉、微信</li>
     * </ul>
     *
     * <p><strong>配置前缀：</strong>{@code carlos.auth.login.methods}</p>
     */
    @Data
    public static class Methods {
        /**
         * 是否启用账号密码登录
         */
        private boolean password = true;

        /**
         * 是否启用短信验证码登录
         */
        private boolean smsCode = true;

        /**
         * 是否启用扫码登录（需要 Carlos APP 配合）
         */
        private boolean qrCode = false;

        /**
         * 是否启用 Passkey/WebAuthn 登录（需要浏览器支持 WebAuthn）
         */
        private boolean passkey = false;

        /**
         * Passkey/WebAuthn Relying Party 配置
         */
        @NestedConfigurationProperty
        private PasskeyConfig passkeyConfig = new PasskeyConfig();

        /**
         * 第三方社交登录配置
         */
        @NestedConfigurationProperty
        private Social social = new Social();

        /**
         * 获取启用的本地登录方式数量（不含社交登录）
         */
        public long enabledCount() {
            return Stream.of(password, smsCode, qrCode, passkey).filter(Boolean::booleanValue).count();
        }
    }

    /**
     * Passkey/WebAuthn Relying Party 配置
     *
     * <p><strong>配置前缀：</strong>{@code carlos.auth.login.methods.passkey-config}</p>
     */
    @Data
    public static class PasskeyConfig {
        /**
         * RP ID，凭据绑定的域名，如 carlos.com
         */
        private String rpId;

        /**
         * RP 显示名称，展示在设备生物识别弹窗中
         */
        private String rpName = "Carlos Framework";

        /**
         * 允许的 Origins（scheme + host + port），如 https://auth.carlos.com
         */
        private List<String> origins = new ArrayList<>();

        /**
         * challenge 缓存 TTL（秒），默认 120 秒
         */
        private long challengeTtl = 120;

        /**
         * 证明要求，默认 none（不验证认证器证明）
         */
        private String attestation = "none";

        /**
         * 是否优先使用可发现凭据（resident key / discoverable credential）
         */
        private boolean residentKeyPreferred = true;
    }

    /**
     * 第三方社交登录配置
     *
     * <p>控制钉钉、微信等 OAuth2 登录方式的开关与凭据。</p>
     *
     * <p><strong>配置前缀：</strong>{@code carlos.auth.login.methods.social}</p>
     */
    @Data
    public static class Social {

        /**
         * 钉钉登录配置
         */
        @NestedConfigurationProperty
        private DingTalk dingtalk = new DingTalk();

        /**
         * 微信登录配置
         */
        @NestedConfigurationProperty
        private WeChat wechat = new WeChat();

        /**
         * 社交登录回调基础地址（用于构建 OAuth2 redirect_uri）
         *
         * <p>示例：{@code https://auth.example.com}，不要带末尾斜杠。</p>
         */
        private String callbackBaseUrl;
    }

    /**
     * 钉钉登录配置
     */
    @Data
    public static class DingTalk {
        /**
         * 是否启用钉钉登录
         */
        private boolean enabled = false;

        /**
         * 钉钉应用 appKey（复用 integration 模块配置时可为空）
         */
        private String appKey;

        /**
         * 钉钉应用 appSecret（复用 integration 模块配置时可为空）
         */
        private String appSecret;

        /**
         * 企业 corpId
         */
        private String corpId;

        /**
         * 扫码登录回调地址（相对路径，如 /auth/oauth2/callback/dingtalk）
         */
        private String callbackPath = "/auth/oauth2/callback/dingtalk";
    }

    /**
     * 微信登录配置
     */
    @Data
    public static class WeChat {
        /**
         * 是否启用微信登录
         */
        private boolean enabled = false;

        /**
         * 微信开放平台 appId
         */
        private String appId;

        /**
         * 微信开放平台 appSecret
         */
        private String appSecret;

        /**
         * 微信登录回调地址（相对路径，如 /auth/oauth2/callback/wechat）
         */
        private String callbackPath = "/auth/oauth2/callback/wechat";
    }

    @Data
    public static class AlertNotification {
        /**
         * 短信告警模板编码
         */
        private String smsTemplateCode;

        /**
         * 钉钉 Webhook URL
         */
        private String dingtalkWebhookUrl;

        /**
         * 短信验证码模板编码
         */
        private String captchaSmsTemplateCode;
    }

    @Data
    public static class Sso {
        /**
         * 是否启用 SSO Token 交换
         */
        private boolean tokenExchangeEnabled = true;

        /**
         * 是否启用 SSO 全局登出
         */
        private boolean globalLogoutEnabled = true;

        /**
         * 用户会话索引 TTL（秒），默认 7 天
         */
        private long userSessionTtl = 604800;

        /**
         * Token 交换时允许的源客户端与目标客户端 scope 映射
         * 如果为空，则使用目标客户端默认 scopes
         */
        private List<String> exchangeAllowedScopes = new ArrayList<>();
    }

}
