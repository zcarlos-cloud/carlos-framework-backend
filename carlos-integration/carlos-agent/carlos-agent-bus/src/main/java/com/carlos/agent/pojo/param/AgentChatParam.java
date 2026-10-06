package com.carlos.agent.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 助手对话参数
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Schema(description = "AI 助手对话参数")
public class AgentChatParam {

    @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "消息内容不能为空")
    private String message;

    @Schema(description = "会话 ID（续聊时传入首轮返回的 sessionId，新会话留空）")
    private String sessionId;
}
