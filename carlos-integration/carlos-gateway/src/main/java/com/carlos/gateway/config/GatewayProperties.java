package com.carlos.gateway.config;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * 网关相关配置
 * </p>
 *
 * @author carlos
 * @date 2022/4/13 16:00
 */

@Data
@ConfigurationProperties(prefix = "carlos.gateway")
public class GatewayProperties implements InitializingBean {

    /**
     * 是否启用前缀校验（默认开启）
     */
    private boolean prefixEnabled = true;

    /**
     * API 统一前缀（例如：/api/v1）
     * 请求需要以此前缀开头，网关会截掉此前缀后路由到后端服务
     */
    private String prefix = "/manage-api";

    /**
     * 白名单路径列表（无需添加前缀）
     * 支持 Ant 风格通配符，如 /swagger-ui/**
     */
    private Set<String> whitelist = new HashSet<>();

    /**
     * 默认白名单（Swagger/Knife4j 相关资源）
     */
    private static final Set<String> DEFAULT_WHITELIST = Set.of(
        "/doc.html",
        "/webjars/**",
        "/swagger-resources/**",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/favicon.ico",
        "/csrf",
        "/actuator/**",
        "/health",
        "/public/**",
        "/static/**"
    );


    @Override
    public void afterPropertiesSet() {
        // 合并默认白名单
        Set<String> merged = new HashSet<>(DEFAULT_WHITELIST);
        merged.addAll(whitelist);
        this.whitelist = merged;
    }


}
