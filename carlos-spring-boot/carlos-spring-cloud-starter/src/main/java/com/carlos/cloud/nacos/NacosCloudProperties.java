package com.carlos.cloud.nacos;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Nacos 扩展配置属性
 * </p>
 *
 * @author carlos
 * @date 2024/01/15
 */
@Data
@ConfigurationProperties(prefix = "carlos.cloud.nacos")
public class NacosCloudProperties {

    /**
     * 是否启用 Nacos 服务注册监听
     */
    private boolean enabled = true;

    /**
     * 服务版本号
     */
    private String version = "1.0.0";

    /**
     * 服务区域
     */
    private String region = "default";

    /**
     * 服务权重
     */
    private double weight = 1.0;

    /**
     * 自定义元数据
     */
    private Map<String, String> metadata = new HashMap<>();

    /**
     * 心跳配置
     */
    private Heartbeat heartbeat = new Heartbeat();

    /**
     * 服务订阅配置
     */
    private Subscription subscription = new Subscription();

    @Data
    public static class Heartbeat {
        /**
         * 心跳间隔（毫秒）
         */
        private int interval = 5000;

        /**
         * 心跳超时时间（毫秒）
         */
        private int timeout = 15000;

        /**
         * 服务删除超时时间（毫秒）
         */
        private int deleteTimeout = 30000;
    }

    @Data
    public static class Subscription {
        /**
         * 是否启用服务订阅监听
         */
        private boolean enabled = true;

        /**
         * 服务变更时是否打印日志
         */
        private boolean logChange = true;
    }
}
