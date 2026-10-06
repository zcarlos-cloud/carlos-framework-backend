package com.carlos.message.channel.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * <p>
 * 原生 WebSocket 配置
 * </p>
 *
 * <p>
 * 独立于 STOMP 配置，提供原生 WebSocket 端点作为降级方案。
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class NativeWebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/native")
            .addInterceptors(webSocketAuthInterceptor)
            .setAllowedOrigins("*");
    }
}
