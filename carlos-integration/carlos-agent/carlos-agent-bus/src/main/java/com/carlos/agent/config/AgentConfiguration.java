package com.carlos.agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 助手配置注册
 *
 * @author carlos
 * @since 3.0.0
 */
@Configuration
@EnableConfigurationProperties(AgentBffProperties.class)
public class AgentConfiguration {
}
