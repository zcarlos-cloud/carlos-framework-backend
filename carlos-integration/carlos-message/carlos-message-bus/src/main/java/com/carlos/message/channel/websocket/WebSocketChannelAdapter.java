package com.carlos.message.channel.websocket;

import cn.hutool.core.util.StrUtil;
import com.carlos.message.channel.AbstractChannelAdapter;
import com.carlos.message.core.channel.ChannelType;
import com.carlos.message.core.protocol.MessageContext;
import com.carlos.message.core.protocol.SendResult;
import com.carlos.message.manager.MessageChannelManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * WebSocket 站内信渠道适配器
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Component
public class WebSocketChannelAdapter extends AbstractChannelAdapter {

    private final SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 用户 WebSocket Session 缓存（key: userId）
     */
    private static final Map<String, WebSocketSession> USER_SESSION_MAP = new ConcurrentHashMap<>();

    public WebSocketChannelAdapter(MessageChannelManager messageChannelManager,
                                   SimpMessagingTemplate simpMessagingTemplate) {
        super(messageChannelManager);
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Override
    public String getChannelCode() {
        return "WEBSOCKET";
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.WEBSOCKET;
    }

    @Override
    public boolean supports(String messageType) {
        return true;
    }

    @Override
    public boolean healthCheck() {
        return super.isAvailable();
    }

    @Override
    protected SendResult doSend(MessageContext context) {
        String userId = context.getReceiverId();
        if (StrUtil.isBlank(userId)) {
            return SendResult.fail("接收者ID不能为空");
        }

        String content = context.getMessageContent();
        if (StrUtil.isBlank(content)) {
            return SendResult.fail("消息内容不能为空");
        }

        try {
            // 优先使用 SimpMessagingTemplate（STOMP 协议）
            if (simpMessagingTemplate != null) {
                simpMessagingTemplate.convertAndSendToUser(userId, "/queue/notification", buildPayload(context));
                return SendResult.success("WS" + System.currentTimeMillis());
            }

            // 降级使用原生 WebSocket Session
            WebSocketSession session = USER_SESSION_MAP.get(userId);
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(buildPayload(context)));
                return SendResult.success("WS" + System.currentTimeMillis());
            }

            return SendResult.fail("WS_USER_OFFLINE", "用户未在线: " + userId);
        } catch (IOException e) {
            log.error("WebSocket 发送异常: userId={}", userId, e);
            return SendResult.fail("WS_SEND_EXCEPTION", e.getMessage());
        }
    }

    private String buildPayload(MessageContext context) {
        cn.hutool.json.JSONObject payload = new cn.hutool.json.JSONObject();
        payload.set("messageId", context.getMessageId());
        payload.set("title", context.getMessageTitle());
        payload.set("content", context.getMessageContent());
        payload.set("type", context.getMessageType());
        payload.set("timestamp", System.currentTimeMillis());
        return payload.toString();
    }

    /**
     * 注册用户 Session（由 WebSocketHandler 调用）
     */
    public static void registerSession(String userId, WebSocketSession session) {
        USER_SESSION_MAP.put(userId, session);
        log.debug("WebSocket 用户上线: userId={}", userId);
    }

    /**
     * 移除用户 Session（由 WebSocketHandler 调用）
     */
    public static void removeSession(String userId) {
        USER_SESSION_MAP.remove(userId);
        log.debug("WebSocket 用户下线: userId={}", userId);
    }
}
