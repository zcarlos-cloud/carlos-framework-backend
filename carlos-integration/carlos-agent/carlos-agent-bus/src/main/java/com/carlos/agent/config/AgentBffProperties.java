package com.carlos.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 助手运行服务（agent-bff，DeepSeek Harness 翻译层）连接配置
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@ConfigurationProperties(prefix = "carlos.agent.bff")
public class AgentBffProperties {

    /**
     * agent-bff 服务地址（翻译层 HTTP 入口）
     */
    private String baseUrl = "http://127.0.0.1:8787";

    /**
     * 单次对话最长等待时间（分钟），复杂诊断需要较长时间
     */
    private long timeoutMinutes = 10;
}
