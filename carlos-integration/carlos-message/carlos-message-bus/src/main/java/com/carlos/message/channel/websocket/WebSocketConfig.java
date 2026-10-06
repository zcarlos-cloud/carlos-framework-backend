package com.carlos.message.channel.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * <p>
 * WebSocket 配置类（STOMP 协议）
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单内存消息代理，前缀为 /topic（广播）和 /queue（点对点）
        config.enableSimpleBroker("/topic", "/queue");
        // 应用前缀，客户端发送消息时使用
        config.setApplicationDestinationPrefixes("/app");
        // 用户点对点前缀
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP 端点，客户端连接地址
        registry.addEndpoint("/ws/message")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}
