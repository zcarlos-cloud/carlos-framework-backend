package com.carlos.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 助手微服务启动程序
 *
 * <p>Spring Cloud 版本：注册进 Nacos，由网关 {@code lb://carlos-agent} 路由转发；
 * 通过 Nacos 共享配置获取公共配置（含网关签名身份断言的验签密钥）。</p>
 *
 * @author carlos
 */
@SpringBootApplication
public class AgentCloudApplication {

    public static void main(final String[] args) {
        SpringApplication.run(AgentCloudApplication.class, args);
    }
}
