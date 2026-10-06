package com.carlos.devkit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * 开发工具平台装配类
 * </p>
 * <p>
 * 注册 {@link DevkitProperties}；其余组件（控制器 / Service / Manager / 引擎）由包扫描装配。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Configuration
@EnableConfigurationProperties(DevkitProperties.class)
public class DevkitConfiguration {

    /**
     * 渲染超时守护线程池
     *
     * <p>FreeMarker 的 {@code Template#process} 不响应中断，因此超时只能保证「调用方及时返回」，
     * 真正兜住资源的是写入器的大小上限（见 {@code FreemarkerRenderer} 的 LimitedWriter）。</p>
     *
     * @return 渲染线程池
     */
    @Bean(name = "devkitRenderExecutor", destroyMethod = "shutdownNow")
    public ExecutorService devkitRenderExecutor() {
        AtomicInteger counter = new AtomicInteger(1);
        return new ThreadPoolExecutor(
            2,
            16,
            60L,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            runnable -> {
                Thread thread = new Thread(runnable, "devkit-render-" + counter.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
