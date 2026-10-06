package com.carlos.integration.core.client;

import org.springframework.core.ParameterizedTypeReference;

import java.util.Map;

/**
 * <p>
 * 动??HTTP 客户端接??
 * 用于完全动态配置的第三方系统对??
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
public abstract class DynamicHttpClient {

    /**
     * 执行通用请求
     *
     * @param endpoint 端点名称（配置中定义??key??
     * @param params   请求参数
     * @return 响应结果（Map 形式??
     */
    public abstract Map<String, Object> execute(String endpoint, Map<String, Object> params);

    /**
     * 执行通用请求（带返回类型??
     *
     * @param endpoint    端点名称
     * @param params      请求参数
     * @param returnType  返回类型
     * @param <T>         返回类型参数
     * @return 响应结果
     */
    public <T> T execute(String endpoint, Map<String, Object> params, Class<T> returnType) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * 执行通用请求（带泛型返回类型??
     *
     * @param endpoint    端点名称
     * @param params      请求参数
     * @param returnType  泛型返回类型引用
     * @param <T>         返回类型参数
     * @return 响应结果
     */
    public <T> T execute(String endpoint, Map<String, Object> params,
                         ParameterizedTypeReference<T> returnType) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * 获取请求构建??
     *
     * @param endpoint 端点名称
     * @return 请求构建??
     */
    public abstract RequestBuilder request(String endpoint);

    /**
     * GET 请求快捷方法
     *
     * @param path   请求路径
     * @param params 查询参数
     * @return 响应结果
     */
    public Map<String, Object> get(String path, Map<String, Object> params) {
        return execute("GET:" + path, params);
    }

    /**
     * POST 请求快捷方法
     *
     * @param path   请求路径
     * @param body   请求??
     * @return 响应结果
     */
    public Map<String, Object> post(String path, Map<String, Object> body) {
        return execute("POST:" + path, body);
    }

    /**
     * PUT 请求快捷方法
     *
     * @param path   请求路径
     * @param body   请求??
     * @return 响应结果
     */
    public Map<String, Object> put(String path, Map<String, Object> body) {
        return execute("PUT:" + path, body);
    }

    /**
     * DELETE 请求快捷方法
     *
     * @param path   请求路径
     * @param params 查询参数
     * @return 响应结果
     */
    public Map<String, Object> delete(String path, Map<String, Object> params) {
        return execute("DELETE:" + path, params);
    }
}
