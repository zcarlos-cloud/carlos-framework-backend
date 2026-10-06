package com.carlos.gateway.gray;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 灰度发布配置属性
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 */
@Data
@ConfigurationProperties(prefix = "carlos.gateway.gray")
public class GrayReleaseProperties {

    /**
     * 是否启用灰度发布
     */
    private boolean enabled = false;

    /**
     * 灰度策略配置（Key: 服务名，Value: 策略）
     */
    private Map<String, GrayStrategy> strategies = new HashMap<>();
}
