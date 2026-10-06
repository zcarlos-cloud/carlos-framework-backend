package com.carlos.core.spring;

/**
 * <p>
 * springboot公共常量
 * </p>
 *
 * @author carlos
 * @date 2020/4/24 12:57
 */
public interface BootConstant {

    /**
     * profile.active=dev
     */
    String PROFILES_ACTIVE = "spring.profiles.active";

    /**
     * servlet-content-path=/api
     */
    String CONTEXT_PATH = "server.servlet.context-path";

    /**
     * 监控客户端地址
     */
    String ADMIN_CLIENT_URL = "spring.boot.admin.client.url";

    /**
     * 端口配置Key
     */
    String SERVER_PORT = "server.port";
}
