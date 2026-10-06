package com.carlos.boot.web;

import java.util.Set;

/**
 * Web Starter 常量定义。
 *
 * @author Carlos
 * @since 3.0.0
 */
public final class WebConstant {

    private WebConstant() {
    }

    /**
     * 通用忽略过滤路径（Swagger / OpenAPI / Actuator 等框架内置路径）。
     */
    public static final Set<String> COMMON_EXCLUDE_PATTERNS = Set.of(
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/doc.html",
        "/favicon.ico",
        "/swagger-resources/**",
        "/v3/api-docs/**",
        "/webjars/**",
        "/csrf",
        "/v3/api-docs-ext",
        "/actuator/**"
    );

    /**
     * 匹配所有路径。
     */
    public static final String ALL_PATH = "/**";

    /**
     * Filter init-param 中存储排除路径的键名。
     */
    public static final String FILTER_INIT_PARAM_EXCLUDES_URIS = "excludedUris";

}
