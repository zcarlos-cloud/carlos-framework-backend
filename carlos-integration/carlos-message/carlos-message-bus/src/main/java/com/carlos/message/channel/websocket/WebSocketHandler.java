package com.carlos.message.channel.websocket;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 原生 WebSocket 处理器
 * </p>
 *
 * <p>
 * 用于处理非 STOMP 协议的原生 WebSocket 连接，
 * 管理用户 Session 的生命周期（上线注册 / 下线移除）。
 * 作为 STOMP 方案的降级备用。
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private static final String USER_ID_KEY = "userId";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        if (StrUtil.isNotBlank(userId)) {
            WebSocketChannelAdapter.registerSession(userId, session);
            log.info("WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());
        } else {
            log.warn("WebSocket 连接未携带 userId, 即将关闭: sessionId={}", session.getId());
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("WebSocket 收到消息: sessionId={}, payload={}", session.getId(), payload);
        // 当前仅作为消息推送通道，不处理客户端上行消息
        // 如需处理心跳，可在此扩展
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = extractUserId(session);
        if (StrUtil.isNotBlank(userId)) {
            WebSocketChannelAdapter.removeSession(userId);
            log.info("WebSocket 连接关闭: userId={}, sessionId={}, status={}",
                userId, session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = extractUserId(session);
        log.error("WebSocket 传输异常: userId={}, sessionId={}", userId, session.getId(), exception);
        if (StrUtil.isNotBlank(userId)) {
            WebSocketChannelAdapter.removeSession(userId);
        }
    }

    /**
     * 从 Session 属性或 URI 参数中提取 userId
     */
    private String extractUserId(WebSocketSession session) {
        // 优先从 attributes 获取（由拦截器设置）
        Map<String, Object> attributes = session.getAttributes();
        if (attributes != null && attributes.get(USER_ID_KEY) != null) {
            return String.valueOf(attributes.get(USER_ID_KEY));
        }

        // 从 URI 查询参数获取
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (StrUtil.isNotBlank(query)) {
            Map<String, List<String>> params = parseQuery(query);
            List<String> values = params.get(USER_ID_KEY);
            if (values != null && !values.isEmpty()) {
                return values.get(0);
            }
        }

        return null;
    }

    private Map<String, List<String>> parseQuery(String query) {
        Map<String, List<String>> result = new java.util.HashMap<>();
        for (String pair : query.split("&")) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                result.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(value);
            }
        }
        return result;
    }
}
