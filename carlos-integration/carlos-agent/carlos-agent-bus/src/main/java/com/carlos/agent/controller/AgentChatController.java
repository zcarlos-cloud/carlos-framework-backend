package com.carlos.agent.controller;

import com.carlos.agent.pojo.param.AgentChatParam;
import com.carlos.agent.service.AgentChatService;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 助手对话接口
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("agent/chat")
@Tag(name = "AI 助手")
public class AgentChatController {

    private final AgentChatService agentChatService;

    /**
     * AI 助手对话（SSE 流式响应）
     * <p>
     * 事件：session（会话建立）/ progress（进度心跳）/ done（最终回答）/ error（失败）
     *
     * @param param 对话参数
     * @return SSE 流
     */
    // 返回 SseEmitter：必须显式关掉响应数据落库，流式结果序列化没有意义
    @Log(
        title = "AI 助手",
        logType = "AGENT_CHAT",
        businessType = BusinessType.OTHER,
        operation = "'AI 助手对话: ' + #param.sessionId",
        targetType = "AGENT_SESSION",
        targetId = "#param.sessionId",
        riskLevel = 20,
        saveResponseData = false
    )
    @PreAuthorize("hasPermission('agent:chat', 'chat')")
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 助手对话", description = "SSE 流式响应，事件：session/progress/done/error")
    public SseEmitter chat(@RequestBody @Validated AgentChatParam param) {
        return agentChatService.chat(param);
    }
}
