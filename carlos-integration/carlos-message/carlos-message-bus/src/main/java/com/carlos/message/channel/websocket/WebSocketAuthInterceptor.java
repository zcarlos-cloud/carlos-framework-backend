package com.carlos.message.channel.websocket;

import cn.hutool.core.util.StrUtil;
import com.carlos.security.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * <p>
 * WebSocket 握手拦截器
 * </p>
 *
 * <p>
 * 在 WebSocket 握手阶段从请求参数或 Header 中提取 userId，
 * 并存入 WebSocketSession attributes 供后续使用。
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final String USER_ID_KEY = "userId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String userId = extractUserId(servletRequest);
            if (StrUtil.isNotBlank(userId)) {
                attributes.put(USER_ID_KEY, userId);
                log.debug("WebSocket 握手成功, userId={}", userId);
                return true;
            }
        }
        log.warn("WebSocket 握手失败, 未提供 userId");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }

    /**
     * 提取握手请求对应的用户 ID
     *
     * <p><b>优先取已验签的登录身份</b>：WebSocket 握手会走完整的 servlet 过滤器链，
     * {@code UserContextAuthenticationFilter} 已据 {@code X-User-Context} 断言建立 SecurityContext，
     * 因此经网关发起的连接总能在此命中，客户端无法伪装。</p>
     *
     * <p>查询参数 {@code userId} 仅作为兜底保留给无法携带凭证的客户端。<b>它可被任意客户端伪造</b>——
     * 之所以只算兜底，是因为经网关的连接必然先命中上面的可信身份；兜底路径实际只对「直连本服务」的
     * 客户端生效，而那要求下游端口在网络上只对网关可达。</p>
     */
    private String extractUserId(ServletServerHttpRequest servletRequest) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null) {
            return String.valueOf(currentUserId);
        }

        String userId = servletRequest.getServletRequest().getParameter(USER_ID_KEY);
        if (StrUtil.isNotBlank(userId)) {
            log.warn("WebSocket handshake 未取得可信登录身份，回退到可伪造的查询参数 userId={}", userId);
            return userId;
        }

        return null;
    }
}
