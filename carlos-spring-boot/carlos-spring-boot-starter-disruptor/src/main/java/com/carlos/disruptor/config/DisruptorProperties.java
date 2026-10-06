package com.carlos.disruptor.config;

import com.carlos.disruptor.core.DisruptorWaitStrategyType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Disruptor配置属性
 *
 * @author Carlos
 * @date 2026-01-27
 */
@Data
@ConfigurationProperties(prefix = "carlos.disruptor")
public class DisruptorProperties {

    /**
     * 是否启用Disruptor
     */
    private boolean enabled = true;

    /**
     * RingBuffer大小，必须是2的幂
     */
    private int bufferSize = 1024;

    /**
     * 等待策略: blocking, sleeping, yielding, busy-spin
     */
    private DisruptorWaitStrategyType waitStrategy = DisruptorWaitStrategyType.BLOCKING;

    /**
     * 线程池配置
     */
    private ThreadPoolConfig threadPool = new ThreadPoolConfig();

    @Data
    public static class ThreadPoolConfig {

        /**
         * 线程名称前缀
         */
        private String threadNamePrefix = "disruptor-";
    }


}
