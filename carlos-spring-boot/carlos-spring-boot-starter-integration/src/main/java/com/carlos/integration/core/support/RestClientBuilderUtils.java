package com.carlos.integration.core.support;

import com.carlos.integration.core.interceptor.DockingLoggingInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * RestClient 构建工具
 * 提供便捷RestClient 创建方法
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Slf4j
public class RestClientBuilderUtils {

    private RestClientBuilderUtils() {
        // 工具类，禁止实例
    }

    /**
     * 创建基础 RestClient
     *
     * @param baseUrl 基础 URL
     * @return RestClient 构建
     */
    public static RestClient.Builder builder(String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl);
    }

    /**
     * 创建带超时的 RestClient
     *
     * @param baseUrl         基础 URL
     * @param connectTimeout  连接超时
     * @param readTimeout     读取超时
     * @return RestClient
     */
    public static RestClient create(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return builder(baseUrl)
            .requestFactory(requestFactory)
            .build();
    }

    /**
     * 创建带拦截器RestClient
     *
     * @param baseUrl     基础 URL
     * @param interceptors 拦截器列
     * @return RestClient
     */
    public static RestClient createWithInterceptors(String baseUrl,
                                                    List<ClientHttpRequestInterceptor> interceptors) {
        return builder(baseUrl)
            .requestInterceptors(list -> list.addAll(interceptors))
            .build();
    }

    /**
     * 创建完整RestClient
     *
     * @param baseUrl         基础 URL
     * @param connectTimeout  连接超时
     * @param readTimeout     读取超时
     * @param headers         默认请求
     * @param interceptors    拦截器列
     * @return RestClient
     */
    public static RestClient create(String baseUrl,
                                    Duration connectTimeout,
                                    Duration readTimeout,
                                    Map<String, String> headers,
                                    List<ClientHttpRequestInterceptor> interceptors) {
        // 创建请求工厂并设置超
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        RestClient.Builder builder = builder(baseUrl)
            .requestFactory(requestFactory);

        if (headers != null) {
            headers.forEach(builder::defaultHeader);
        }

        if (interceptors != null && !interceptors.isEmpty()) {
            builder.requestInterceptors(list -> list.addAll(interceptors));
        }

        return builder.build();
    }

    /**
     * 创建 HTTP 服务代理
     *
     * @param restClient     RestClient 实例
     * @param interfaceClass 接口
     * @param <T>            接口类型
     * @return 代理实例
     */
    public static <T> T createProxy(RestClient restClient, Class<T> interfaceClass) {
        org.springframework.web.service.invoker.HttpServiceProxyFactory factory =
            org.springframework.web.service.invoker.HttpServiceProxyFactory
                .builderFor(org.springframework.web.client.support.RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(interfaceClass);
    }

    /**
     * 一站式创建 HTTP 服务代理
     *
     * @param baseUrl        基础 URL
     * @param interfaceClass 接口
     * @param <T>            接口类型
     * @return 代理实例
     */
    public static <T> T createService(String baseUrl, Class<T> interfaceClass) {
        RestClient restClient = create(baseUrl, Duration.ofSeconds(5), Duration.ofSeconds(30));
        return createProxy(restClient, interfaceClass);
    }

    /**
     * 一站式创建 HTTP 服务代理（带配置
     *
     * @param baseUrl        基础 URL
     * @param interfaceClass 接口
     * @param config         配置
     * @param <T>            接口类型
     * @return 代理实例
     */
    public static <T> T createService(String baseUrl, Class<T> interfaceClass,
                                      ServiceConfig config) {
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

        // 添加日志拦截
        if (config.isEnableLogging()) {
            interceptors.add(new DockingLoggingInterceptor());
        }

        // 添加自定义拦截器
        if (config.getInterceptors() != null) {
            interceptors.addAll(config.getInterceptors());
        }

        RestClient restClient = create(
            baseUrl,
            config.getConnectTimeout(),
            config.getReadTimeout(),
            config.getHeaders(),
            interceptors
        );

        return createProxy(restClient, interfaceClass);
    }

    /**
     * 服务配置
     */
    public static class ServiceConfig {
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(30);
        private Map<String, String> headers;
        private List<ClientHttpRequestInterceptor> interceptors;
        private boolean enableLogging = true;

        // Getters and Setters
        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public List<ClientHttpRequestInterceptor> getInterceptors() {
            return interceptors;
        }

        public void setInterceptors(List<ClientHttpRequestInterceptor> interceptors) {
            this.interceptors = interceptors;
        }

        public boolean isEnableLogging() {
            return enableLogging;
        }

        public void setEnableLogging(boolean enableLogging) {
            this.enableLogging = enableLogging;
        }
    }
}
