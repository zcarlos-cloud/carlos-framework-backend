package com.carlos.agent.service;

import com.alibaba.fastjson2.JSON;
import com.carlos.agent.config.AgentBffProperties;
import com.carlos.agent.enums.AgentErrorCode;
import com.carlos.agent.pojo.param.AgentChatParam;
import com.carlos.audit.api.ApiAuditLogMain;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * AI 助手对话服务
 * <p>
 * 代理转发：管理后台 ↔ agent-bff（DeepSeek Harness 翻译层）。
 * 使用 JDK HttpClient 以行为单位消费 BFF 的 SSE 流，逐事件转发给前端 SseEmitter；
 * 对话结束后落一条审计日志（AGENT_CHAT）。
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChatService {

    private final AgentBffProperties properties;
    private final ApiAuditLogMain auditLogClient;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 发起一轮 AI 对话，返回 SSE 流（事件：session / progress / done / error）
     *
     * @param param 对话参数
     * @return SSE 流
     */
    public SseEmitter chat(AgentChatParam param) {
        long timeoutMillis = Duration.ofMinutes(properties.getTimeoutMinutes()).toMillis();
        SseEmitter emitter = new SseEmitter(timeoutMillis);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + "/v1/chat"))
                .timeout(Duration.ofMillis(timeoutMillis))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(param)))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        fail(emitter, param, null, "运行服务返回 HTTP " + response.statusCode());
                        return;
                    }
                    SseForwarder forwarder = new SseForwarder(emitter, param);
                    try {
                        response.body().forEach(forwarder::onLine);
                        forwarder.onStreamComplete();
                    } catch (Exception e) {
                        forwarder.onStreamError(e);
                    }
                })
                .exceptionally(ex -> {
                    log.warn("[agent] 调用运行服务失败: {}", ex.getMessage());
                    fail(emitter, param, null, "运行服务连接失败");
                    return null;
                });
        return emitter;
    }

    private void fail(SseEmitter emitter, AgentChatParam param, String sessionId, String detail) {
        try {
            emitter.send(SseEmitter.event().name("error").data("{\"message\":\"" + detail + "\"}"));
        } catch (Exception ignored) {
            // 客户端可能已断开
        }
        audit(param.getMessage(), sessionId, false, detail);
        emitter.complete();
    }

    private void audit(String message, String sessionId, boolean success, String detail) {
        try {
            String brief = message == null ? "" : message.length() > 100 ? message.substring(0, 100) : message;
            String operation = "AI助手对话：" + brief + (success ? "" : "（失败：" + detail + "）");
            auditLogClient.saveSimpleAuditLog("AGENT_CHAT", AuditLogCategoryEnum.BUSINESS,
                    operation, null, sessionId, success ? AuditLogStatusEnum.SUCCESS : AuditLogStatusEnum.FAIL);
        } catch (Exception e) {
            // 审计失败不影响对话主流程
            log.warn("[agent] 审计日志记录失败: {}", e.getMessage());
        }
    }

    /**
     * SSE 行解析 + 转发器：把 BFF 的 SSE 流按事件转发给前端 SseEmitter
     */
    private final class SseForwarder {

        private final SseEmitter emitter;
        private final AgentChatParam param;
        private String pendingEvent;
        private final StringBuilder pendingData = new StringBuilder();
        private String sessionId;
        private boolean audited;
        private boolean cancelled;

        private SseForwarder(SseEmitter emitter, AgentChatParam param) {
            this.emitter = emitter;
            this.param = param;
        }

        void onLine(String line) {
            if (cancelled || line == null) {
                return;
            }
            if (line.isEmpty()) {
                dispatch();
                return;
            }
            if (line.startsWith(":")) {
                // SSE 注释行，忽略
                return;
            }
            if (line.startsWith("event:")) {
                pendingEvent = line.substring("event:".length()).trim();
                return;
            }
            if (line.startsWith("data:")) {
                String value = line.substring("data:".length());
                if (value.startsWith(" ")) {
                    value = value.substring(1);
                }
                if (pendingData.length() > 0) {
                    pendingData.append('\n');
                }
                pendingData.append(value);
            }
        }

        void onStreamComplete() {
            dispatch();
            if (!audited) {
                // 流结束但没有 done 事件（异常兜底）
                audited = true;
                audit(param.getMessage(), sessionId, false, "运行服务未返回结果");
            }
            emitter.complete();
        }

        void onStreamError(Exception e) {
            if (!audited) {
                audited = true;
                audit(param.getMessage(), sessionId, false, "运行服务流中断");
            }
            emitter.completeWithError(AgentErrorCode.AGENT_BFF_ERROR.exception());
        }

        /**
         * 一个 SSE 事件块结束（空行），转发给前端
         */
        private void dispatch() {
            if (pendingEvent == null && pendingData.length() == 0) {
                return;
            }
            String event = pendingEvent == null ? "message" : pendingEvent;
            String data = pendingData.toString();
            pendingEvent = null;
            pendingData.setLength(0);

            if ("session".equals(event)) {
                sessionId = JSON.parseObject(data).getString("sessionId");
            }
            try {
                emitter.send(SseEmitter.event().name(event).data(data));
            } catch (Exception e) {
                // 前端断开：停止消费上游流
                cancelled = true;
                return;
            }
            if ("done".equals(event) && !audited) {
                audited = true;
                audit(param.getMessage(), sessionId, true, null);
            }
            if ("error".equals(event) && !audited) {
                audited = true;
                audit(param.getMessage(), sessionId, false, data);
            }
        }
    }
}
