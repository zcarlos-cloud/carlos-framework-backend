package com.carlos.cloud.feign;

import feign.Logger;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Feign 扩展配置属性
 * </p>
 *
 * @author carlos
 * @date 2019-09-28
 */
@Data
@ConfigurationProperties(prefix = "carlos.feign")
public class FeignProperties {

    /**
     * 日志配置
     */
    private LogProperties log = new LogProperties();

    /**
     * 重试配置
     */
    private RetryProperties retry = new RetryProperties();

    /**
     * 请求头传递配置
     */
    private HeaderProperties header = new HeaderProperties();

    /**
     * 连接池配置
     */
    private PoolProperties pool = new PoolProperties();

    /**
     * FallbackFactory 自动注册配置
     */
    private FallbackProperties fallback = new FallbackProperties();

    /**
     * 日志配置
     */
    @Data
    public static class LogProperties {

        /**
         * 是否启用 默认启用
         */
        private boolean enable = true;

        /**
         * 日志级别 Feign支持4中级别： NONE：不记录任何日志，默认值 BASIC：仅记录请求的方法，URL以及响应状态码和执行时间 HEADERS：在BASIC基础上，额外记录了请求和响应的头信息 FULL：记录所有请求和响应的明细，包括头信息、请求体、元数据
         */
        private Logger.Level level = Logger.Level.FULL;
    }

    /**
     * 重试配置
     */
    @Data
    public static class RetryProperties {

        /**
         * 是否启用重试
         */
        private boolean enabled = true;

        /**
         * 重试间隔（毫秒）
         */
        private long period = 100;

        /**
         * 最大重试间隔（毫秒）
         */
        private long maxPeriod = 1000;

        /**
         * 最大重试次数
         */
        private int maxAttempts = 3;
    }

    /**
     * 请求头传递配置
     */
    @Data
    public static class HeaderProperties {

        /**
         * 需要传递的请求头
         */
        private Set<String> passHeaders = new HashSet<>();

        /**
         * 是否传递 Authorization 头
         */
        private boolean passAuthorization = true;

        /**
         * 是否传递 X-Request-Id
         */
        private boolean passRequestId = true;

        /**
         * 是否传递 X-Tenant-Id
         */
        private boolean passTenantId = true;

        /**
         * 是否传递 X-User-Id
         */
        private boolean passUserId = true;

        /**
         * 需要忽略的请求头
         */
        private Set<String> ignoreHeaders = new HashSet<>();

        public HeaderProperties() {
            // 默认需要忽略的请求头
            ignoreHeaders.add("content-length");
            ignoreHeaders.add("content-type");
            ignoreHeaders.add("host");
            ignoreHeaders.add("connection");
            ignoreHeaders.add("accept-encoding");
            ignoreHeaders.add("accept");
            ignoreHeaders.add("referer");
            ignoreHeaders.add("origin");
            ignoreHeaders.add("x-forwarded-for");
        }
    }

    /**
     * 连接池配置
     */
    @Data
    public static class PoolProperties {

        /**
         * 是否启用连接池
         */
        private boolean enabled = true;

        /**
         * 最大空闲连接数
         */
        private int maxIdle = 50;

        /**
         * 保持连接时长（分钟）
         */
        private long keepAliveDuration = 5;

        /**
         * 连接超时（毫秒）
         */
        private long connectTimeout = 5000;

        /**
         * 读取超时（毫秒）
         */
        private long readTimeout = 10000;
    }

    /**
     * FallbackFactory 自动注册配置
     */
    @Data
    public static class FallbackProperties {

        /**
         * 是否启用自动注册
         */
        private boolean enabled = true;

        /**
         * 扫描包路径列表
         * <p>
         * 如果不配置，默认扫描 {@code com.carlos} 包及其子包
         * </p>
         */
        private List<String> scanPackages = new ArrayList<>();
    }
}
