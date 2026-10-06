package com.carlos.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * <p>
 * 异步执行器配置
 * </p>
 *
 * <p>激活 {@link org.springframework.scheduling.annotation.Async} 并提供虚拟线程执行器。
 * 历史上 carlos-auth 没有 {@code @EnableAsync}，所有 @Async 注解（含自调用场景）均为无效声明。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 虚拟线程执行器（JDK 21）
     */
    @Bean("virtualTaskExecutor")
    public Executor virtualTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
