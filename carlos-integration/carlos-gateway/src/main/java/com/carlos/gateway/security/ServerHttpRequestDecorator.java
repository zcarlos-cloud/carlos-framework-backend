package com.carlos.gateway.security;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Flux;

/**
 * 用于包装 ServerHttpRequest 的装饰器类
 * 继承自 Spring 的 ServerHttpRequestDecorator
 */
public class ServerHttpRequestDecorator extends org.springframework.http.server.reactive.ServerHttpRequestDecorator {

    private final Flux<DataBuffer> body;

    public ServerHttpRequestDecorator(ServerHttpRequest delegate) {
        super(delegate);
        this.body = null;
    }

    public ServerHttpRequestDecorator(ServerHttpRequest delegate, Flux<DataBuffer> body) {
        super(delegate);
        this.body = body;
    }

    @Override
    public Flux<DataBuffer> getBody() {
        if (body != null) {
            return body;
        }
        return super.getBody();
    }
}
