package com.carlos.integration.core.client;

import com.carlos.integration.core.support.DockingRestClientException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpMethod.*;


/**
 * <p>
 * HTTP 请求构建
 * 提供流式 API 构建请求
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
public class RequestBuilder {

    private final RestClient restClient;
    private final String path;

    private HttpMethod method = GET;
    private final Map<String, Object> queryParams = new HashMap<>();
    private Object body;
    private final Map<String, String> headers = new HashMap<>();
    private MediaType contentType = MediaType.APPLICATION_JSON;

    public RequestBuilder(RestClient restClient, String path) {
        this.restClient = restClient;
        this.path = path;
    }

    /**
     * 设置请求方法
     */
    public RequestBuilder method(HttpMethod method) {
        this.method = method;
        return this;
    }

    public RequestBuilder get() {
        this.method = GET;
        return this;
    }

    public RequestBuilder post() {
        this.method = POST;
        return this;
    }

    public RequestBuilder put() {
        this.method = PUT;
        return this;
    }

    public RequestBuilder delete() {
        this.method = HttpMethod.DELETE;
        return this;
    }

    public RequestBuilder patch() {
        this.method = HttpMethod.PATCH;
        return this;
    }

    /**
     * 添加查询参数
     */
    public RequestBuilder queryParam(String name, Object value) {
        this.queryParams.put(name, value);
        return this;
    }

    /**
     * 添加多个查询参数
     */
    public RequestBuilder queryParams(Map<String, Object> params) {
        this.queryParams.putAll(params);
        return this;
    }

    /**
     * 设置请求
     */
    public RequestBuilder body(Object body) {
        this.body = body;
        return this;
    }

    /**
     * 添加请求
     */
    public RequestBuilder header(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    /**
     * 添加多个请求
     */
    public RequestBuilder headers(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * 设置 Content-Type
     */
    public RequestBuilder contentType(MediaType contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * 执行请求（返Map
     */
    public Map<String, Object> execute() {
        return execute(new ParameterizedTypeReference<>() {
        });
    }

    /**
     * 执行请求（指定返回类型）
     */
    public <T> T execute(Class<T> returnType) {
        RestClient.RequestHeadersSpec<?> spec = buildRequest();

        // 对于支持 body 的方法，设置请求体
        if (body != null && spec instanceof RestClient.RequestBodySpec bodySpec) {
            bodySpec.body(body);
        }

        return spec.retrieve()
            .onStatus(
                HttpStatusCode::isError,
                (request, response) -> {
                    throw new DockingRestClientException(
                        "Request failed: " + response.getStatusText());
                }
            )
            .body(returnType);
    }

    /**
     * 执行请求（指定泛型返回类型）
     */
    public <T> T execute(ParameterizedTypeReference<T> returnType) {
        RestClient.RequestHeadersSpec<?> spec = buildRequest();

        // 对于支持 body 的方法，设置请求体
        if (body != null && spec instanceof RestClient.RequestBodySpec bodySpec) {
            bodySpec.body(body);
        }

        return spec.retrieve()
            .onStatus(
                HttpStatusCode::isError,
                (request, response) -> {
                    throw new DockingRestClientException(
                        "Request failed: " + response.getStatusText());
                }
            )
            .body(returnType);
    }

    /**
     * 执行请求（无返回值）
     */
    public void executeVoid() {
        RestClient.RequestHeadersSpec<?> spec = buildRequest();

        // 对于支持 body 的方法，设置请求体
        if (body != null && spec instanceof RestClient.RequestBodySpec bodySpec) {
            bodySpec.body(body);
        }

        spec.retrieve()
            .onStatus(
                HttpStatusCode::isError,
                (request, response) -> {
                    throw new DockingRestClientException(
                        "Request failed: " + response.getStatusText());
                }
            )
            .toBodilessEntity();
    }

    /**
     * 构建请求
     */
    private RestClient.RequestHeadersSpec<?> buildRequest() {
        RestClient.RequestHeadersSpec<?> spec;

        // 构建 URI
        if (queryParams.isEmpty()) {
            if (method == HttpMethod.GET) {
                spec = restClient.get().uri(path);
            } else if (method == HttpMethod.POST) {
                spec = restClient.post().uri(path);
            } else if (method == HttpMethod.PUT) {
                spec = restClient.put().uri(path);
            } else if (method == HttpMethod.DELETE) {
                spec = restClient.delete().uri(path);
            } else if (method == HttpMethod.PATCH) {
                spec = restClient.patch().uri(path);
            } else {
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }
        } else {
            if (method == HttpMethod.GET) {
                spec = restClient.get().uri(uriBuilder -> {
                    uriBuilder.path(path);
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                });
            } else if (method == HttpMethod.POST) {
                spec = restClient.post().uri(uriBuilder -> {
                    uriBuilder.path(path);
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                });
            } else if (method == HttpMethod.PUT) {
                spec = restClient.put().uri(uriBuilder -> {
                    uriBuilder.path(path);
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                });
            } else if (method == HttpMethod.DELETE) {
                spec = restClient.delete().uri(uriBuilder -> {
                    uriBuilder.path(path);
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                });
            } else if (method == HttpMethod.PATCH) {
                spec = restClient.patch().uri(uriBuilder -> {
                    uriBuilder.path(path);
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                });
            } else {
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }
        }

        // 添加请求头
        headers.forEach(spec::header);
        spec.header("Content-Type", contentType.toString());

        return spec;
    }
}
