package com.carlos.openapi;

import org.apache.commons.collections4.SetUtils;

import java.util.Set;

/**
 * <p>
 * 公共常量
 * </p>
 *
 * @author carlos
 * @date 2020/4/24 12:57
 */
public interface OpenApiConstant {

    /**
     * swagger地址
     */
    String HTML_SWAGGER = "swagger-ui.html";

    /**
     * Knife4J地址
     */
    String HTML_KNIFE4J = "doc.html";

    /**
     * 默认的排除路径，排除Spring Boot默认的错误处理路径和端点
     */
    Set<String> DEFAULT_EXCLUDE_PATH = SetUtils.hashSet("/error", "/actuator/**");

    String DEFAULT_BASE_PATH = "/**";

    /**
     * servlet-content-path=/api
     */
    String CONTEXT_PATH = "server.servlet.context-path";

    /**
     * 端口配置Key
     */
    String SERVER_PORT = "server.port";
}
