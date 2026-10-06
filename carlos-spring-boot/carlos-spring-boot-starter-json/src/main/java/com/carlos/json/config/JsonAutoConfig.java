package com.carlos.json.config;

import com.carlos.json.JsonFactory;
import com.carlos.json.config.JsonProperties.JsonEngineType;
import com.carlos.json.core.JsonService;
import com.carlos.json.fastjson.FastjsonJsonService;
import com.carlos.json.gson.GsonJsonService;
import com.carlos.json.jackson.JacksonJsonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

/**
 * <p>
 * JSON 自动配置主类
 * </p>
 *
 * <p>
 * 支持多引擎切换（Jackson、Fastjson2、Gson），默认使用 Jackson。
 * 通过 {@code carlos.json.engine} 配置切换引擎。
 * </p>
 *
 * <p><strong>Jackson 模式（默认）：</strong></p>
 * <ul>
 *   <li>使用 Spring Boot 标准 ObjectMapper（Jackson 3）</li>
 *   <li>保留 spring.jackson.* 配置有效性</li>
 *   <li>通过 JacksonJsonService 添加 carlos 特有配置（Long转String等）</li>
 * </ul>
 *
 * @author Carlos
 * @date 2026-03-24 13:47
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(JsonProperties.class)
public class JsonAutoConfig {

    /**
     * Jackson JSON 服务（默认引擎）
     *
     * <p>注入 Spring Boot 自动配置的 ObjectMapper（已应用 spring.jackson.* 配置），
     * 在此基础上添加 carlos 特有的序列化配置（Long转String、空字符串过滤等）。</p>
     */
    @Bean
    @ConditionalOnMissingBean(JsonService.class)
    @ConditionalOnClass(ObjectMapper.class)
    @ConditionalOnProperty(prefix = "carlos.json", name = "engine", havingValue = "jackson", matchIfMissing = true)
    public JsonService jacksonJsonService(ObjectMapper objectMapper, JsonProperties jsonProperties) {
        log.info("Creating JacksonJsonService (default engine)");
        // JacksonJsonService 会在 Spring 的 ObjectMapper 基础上应用 carlos.json.* 配置
        JsonService service = new JacksonJsonService(objectMapper, jsonProperties);
        // 注册到 JsonFactory 供静态方法使用
        JsonFactory.registerService(JsonEngineType.JACKSON, service);
        return service;
    }

    /**
     * Fastjson2 JSON 服务
     */
    @Bean
    @ConditionalOnMissingBean(JsonService.class)
    @ConditionalOnClass(name = "com.alibaba.fastjson2.JSON")
    @ConditionalOnProperty(prefix = "carlos.json", name = "engine", havingValue = "fastjson2")
    public JsonService fastjsonJsonService(JsonProperties jsonProperties) {
        log.info("Creating FastjsonJsonService");
        JsonService service = new FastjsonJsonService(jsonProperties);
        JsonFactory.registerService(JsonEngineType.FASTJSON2, service);
        return service;
    }

    /**
     * Gson JSON 服务
     */
    @Bean
    @ConditionalOnMissingBean(JsonService.class)
    @ConditionalOnClass(name = "com.google.gson.Gson")
    @ConditionalOnProperty(prefix = "carlos.json", name = "engine", havingValue = "gson")
    public JsonService gsonJsonService(JsonProperties jsonProperties) {
        log.info("Creating GsonJsonService");
        JsonService service = new GsonJsonService(jsonProperties);
        JsonFactory.registerService(JsonEngineType.GSON, service);
        return service;
    }
}
