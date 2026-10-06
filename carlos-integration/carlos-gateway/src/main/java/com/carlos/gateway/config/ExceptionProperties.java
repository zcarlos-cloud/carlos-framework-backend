package com.carlos.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * 网关异常处理配置属性
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
@Data
@ConfigurationProperties(prefix = "carlos.gateway.exception")
public class ExceptionProperties {

    /**
     * 开发模式：显示详细错误信息（包括异常详情和堆栈）
     */
    private boolean devMode = false;

    /**
     * 是否显示异常堆栈信息（生产环境建议关闭）
     */
    private boolean showStackTrace = false;
}
