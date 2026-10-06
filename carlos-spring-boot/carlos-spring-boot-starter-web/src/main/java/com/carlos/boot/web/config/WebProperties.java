package com.carlos.boot.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Web Starter 顶层配置属性 (carlos.web)。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
@ConfigurationProperties(prefix = "carlos.web")
public class WebProperties {

    /**
     * 应用名称（默认取 spring.application.name）。
     */
    private String appName;

    /**
     * 项目静态资源访问配置。
     */
    private java.util.Map<String, String> resourceHandlers;

    /**
     * 统一响应包装配置。
     */
    private ResponseWrapProperties responseWrap = new ResponseWrapProperties();

    /**
     * 过滤器配置。
     */
    private FilterProperties filter = new FilterProperties();

    /**
     * 拦截器配置。
     */
    private InterceptorProperties interceptor = new InterceptorProperties();

    /**
     * 枚举字典配置。
     */
    private EnumDictProperties enumDict = new EnumDictProperties();

    /**
     * 资源扫描配置。
     */
    private ResourceScanProperties resourceScan = new ResourceScanProperties();

    /**
     * 虚拟线程配置。
     */
    private VirtualThreadProperties virtualThread = new VirtualThreadProperties();

    /**
     * 请求配置。
     */
    private RequestProperties request = new RequestProperties();

    /**
     * 错误处理配置。
     */
    private ErrorProperties error = new ErrorProperties();

    // ==================== 内嵌配置类 ====================

    @Data
    public static class RequestProperties {
        /** 最大请求体大小 */
        private String maxBodySize = "10MB";
        /** 日志中最大 Body 长度（字符） */
        private int logMaxBodyLength = 2048;
    }

    @Data
    public static class ErrorProperties {
        /** 是否包含堆栈信息: never | always | on-param */
        private String includeStacktrace = "never";
        /** 是否包含错误消息: always | never | on-param */
        private String includeMessage = "always";
        /** 是否启用 RFC 9457 Problem Detail 格式 */
        private boolean problemDetailEnabled = false;
    }

    @Data
    public static class VirtualThreadProperties {
        /** 是否启用 Tomcat 虚拟线程 */
        private boolean tomcatEnabled = true;
        /** 是否启用 @Async 虚拟线程执行器 */
        private boolean asyncEnabled = true;
    }
}
