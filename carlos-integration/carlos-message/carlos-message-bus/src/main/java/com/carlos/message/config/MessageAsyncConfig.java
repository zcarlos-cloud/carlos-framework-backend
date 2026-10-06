package com.carlos.message.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 消息异步任务线程池配置
 * </p>
 *
 * @author Carlos
 * @date 2026/3/12
 */
@Configuration
@EnableAsync
public class MessageAsyncConfig {

    @Bean("messageTaskExecutor")
    public Executor messageTaskExecutor() {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        return executor;
    }
}
