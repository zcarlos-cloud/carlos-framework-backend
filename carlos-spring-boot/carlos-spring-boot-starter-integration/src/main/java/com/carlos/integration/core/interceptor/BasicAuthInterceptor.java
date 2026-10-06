package com.carlos.integration.core.interceptor;

import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * <p>
 * 基础认证拦截
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
public record BasicAuthInterceptor(String username, String password)
    implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
        org.springframework.http.HttpRequest request, byte[] body,
        ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().setBasicAuth(username, password);
        return execution.execute(request, body);
    }
}
