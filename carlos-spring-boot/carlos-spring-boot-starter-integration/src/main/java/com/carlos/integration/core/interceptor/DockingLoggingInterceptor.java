package com.carlos.integration.core.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <p>
 * 对接请求日志拦截
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Slf4j
public class DockingLoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        long startTime = System.currentTimeMillis();

        // 记录请求
        logRequest(request, body);

        // 执行请求
        ClientHttpResponse response = execution.execute(request, body);

        // 记录响应
        long duration = System.currentTimeMillis() - startTime;
        logResponse(response, duration);

        return response;
    }

    private void logRequest(HttpRequest request, byte[] body) {
        log.debug("[Docking Request] URI: {}, Method: {}, Headers: {}, Body: {}",
            request.getURI(),
            request.getMethod(),
            request.getHeaders(),
            body.length > 0 ? new String(body, StandardCharsets.UTF_8) : "<empty>");
    }

    private void logResponse(ClientHttpResponse response, long duration) throws IOException {
        log.debug("[Docking Response] Status: {}, Headers: {}, Duration: {}ms",
            response.getStatusCode(),
            response.getHeaders(),
            duration);
    }
}
