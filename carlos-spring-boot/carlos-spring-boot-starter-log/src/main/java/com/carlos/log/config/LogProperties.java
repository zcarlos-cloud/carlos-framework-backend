package com.carlos.log.config;

import com.carlos.disruptor.core.DisruptorWaitStrategyType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 日志模块配置属性
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@ConfigurationProperties(prefix = "carlos.log")
public class LogProperties {

    /**
     * 是否启用日志记录
     */
    private boolean enabled = true;

    /**
     * 是否异步记录
     */
    private boolean async = true;

    /**
     * Disruptor 配置
     */
    private Disruptor disruptor = new Disruptor();

    /**
     * 存储配置
     */
    private Storage storage = new Storage();

    @Data
    public static class Disruptor {

        /**
         * RingBuffer 大小（必须是 2 的幂）
         */
        private int ringBufferSize = 1024;

        /**
         * 线程名称
         */
        private String name = "log-disruptor";

        /**
         * 等待策略类型
         * BLOCKING、BUSY_SPIN、SLEEPING、YIELDING
         */
        private DisruptorWaitStrategyType waitStrategy = DisruptorWaitStrategyType.BLOCKING;

    }

    @Data
    public static class Storage {

        /**
         * 默认存储器
         * logging - 仅输出到日志
         * composite - 组合存储（多个存储器）
         */
        private String defaultStorage = "logging";

        /**
         * 是否启用日志存储兜底方案
         */
        private boolean fallbackEnabled = true;

        /**
         * 批量存储大小
         */
        private int batchSize = 100;

        /**
         * 批量存储超时（毫秒）
         */
        private long batchTimeout = 100;
    }
}
