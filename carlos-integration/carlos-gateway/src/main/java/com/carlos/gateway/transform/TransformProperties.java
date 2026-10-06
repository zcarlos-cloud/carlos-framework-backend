package com.carlos.gateway.transform;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 请求/响应转换配置属性
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 */
@Data
@ConfigurationProperties(prefix = "carlos.gateway.transform")
public class TransformProperties {

    /**
     * 是否启用转换
     */
    private boolean enabled = true;

    /**
     * 网关标识
     */
    private String gatewayId = "carlos-gateway";

    /**
     * 是否启用 API 版本控制
     */
    private boolean apiVersionEnabled = true;

    /**
     * 路径重写规则（Key: 匹配正则, Value: 替换字符串）
     */
    private Map<String, String> pathRewrites;

    /**
     * Header 转换规则
     */
    private List<HeaderTransform> headerTransforms;

    @Data
    public static class HeaderTransform {

        /**
         * 源 Header 名称
         */
        private String source;

        /**
         * 目标 Header 名称
         */
        private String target;

        /**
         * 操作类型
         */
        private Operation operation = Operation.ADD;

        /**
         * 前缀
         */
        private String prefix;

        /**
         * 后缀
         */
        private String suffix;

        /**
         * 值映射
         */
        private Map<String, String> mapping;

        public enum Operation {
            ADD,      // 添加
            REPLACE,  // 替换
            REMOVE    // 移除
        }
    }
}
