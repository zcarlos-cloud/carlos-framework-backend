package com.carlos.integration.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * RestClient 对接配置属
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "carlos.integration.restclient")
public class DockingRestClientProperties {

    /**
     * 是否启用 RestClient 对接模块
     */
    private boolean enabled = true;

    /**
     * 全局默认配置
     */
    private Defaults defaults = new Defaults();

    /**
     * 客户端配置列
     */
    private Map<String, ClientConfig> clients = new HashMap<>();

    @Data
    public static class Defaults {
        /**
         * 连接超时
         */
        private Duration connectTimeout = Duration.ofSeconds(5);

        /**
         * 读取超时
         */
        private Duration readTimeout = Duration.ofSeconds(30);

        /**
         * 是否跟随重定
         */
        private boolean followRedirects = true;

        /**
         * 默认请求
         */
        private Map<String, String> headers = new HashMap<>();
    }

    @Data
    public static class ClientConfig {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 客户端类型：predefined（预定义接口）、dynamic（动态接口）
         */
        private String type = "predefined";

        /**
         * 基础 URL
         */
        private String baseUrl;

        /**
         * 预定义接口类全名
         */
        private String interfaceClass;

        /**
         * 连接超时
         */
        private Duration connectTimeout;

        /**
         * 读取超时
         */
        private Duration readTimeout;

        /**
         * 默认请求
         */
        private Map<String, String> headers = new HashMap<>();

        /**
         * 拦截器类名列
         */
        private List<String> interceptors = new ArrayList<>();

        /**
         * 认证配置
         */
        private AuthConfig auth = new AuthConfig();
    }

    @Data
    public static class AuthConfig {
        /**
         * 认证类型：none、basic、bearer、apikey、custom
         */
        private String type = "none";

        /**
         * Basic 认证用户
         */
        private String username;

        /**
         * Basic 认证密码
         */
        private String password;

        /**
         * Bearer Token
         */
        private String token;

        /**
         * API Key 名称
         */
        private String apiKeyName;

        /**
         * API Key
         */
        private String apiKeyValue;

        /**
         * API Key 位置：header、query
         */
        private String apiKeyIn = "header";

        /**
         * 自定义认证拦截器
         */
        private String customInterceptor;
    }
}
