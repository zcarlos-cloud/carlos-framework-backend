package com.carlos.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * AI 助手服务启动类
 * <p>
 * 管理平台与 DeepSeek Harness 运行时之间的代理层：
 * 负责对话请求的认证转发、SSE 流代理与审计落库
 *
 * @author carlos
 * @since 3.0.0
 */
@SpringBootApplication
public class AgentBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentBootApplication.class, args);
    }
}
