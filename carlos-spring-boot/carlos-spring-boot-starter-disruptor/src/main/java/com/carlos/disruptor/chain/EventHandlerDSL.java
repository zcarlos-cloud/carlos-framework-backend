package com.carlos.disruptor.chain;

import com.carlos.disruptor.config.DisruptorProperties;
import com.carlos.disruptor.core.DisruptorEventHandler;
import com.carlos.disruptor.core.DisruptorTemplate;
import com.carlos.disruptor.metrics.DisruptorMetrics;
import com.lmax.disruptor.WaitStrategy;

/**
 * 事件处理器链 DSL
 * <p>
 * 提供流式 API 构建复杂的消费者处理链
 * </p>
 *
 * @author Carlos
 * @date 2026-03-14
 */
public class EventHandlerDSL {

    private EventHandlerDSL() {
        // 工具类
    }

    /**
     * 开始构建处理器链
     *
     * @param name       链名称
     * @param properties 配置属性
     * @param <T>        业务数据类型
     * @return 链构建器
     */
    public static <T> EventHandlerChainBuilder<T> chain(String name, DisruptorProperties properties) {
        return new EventHandlerChainBuilder<>(name, properties, null);
    }

    /**
     * 开始构建处理器链
     *
     * @param name       链名称
     * @param properties 配置属性
     * @param metrics    指标收集器
     * @param <T>        业务数据类型
     * @return 链构建器
     */
    public static <T> EventHandlerChainBuilder<T> chain(String name, DisruptorProperties properties,
                                                        DisruptorMetrics metrics) {
        return new EventHandlerChainBuilder<>(name, properties, metrics);
    }

    /**
     * 创建并行处理器组
     *
     * @param handlers 处理器数组
     * @param <T>      业务数据类型
     * @return 处理器组
     */
    @SafeVarargs
    public static <T> EventHandlerGroup<T> parallel(DisruptorEventHandler<T>... handlers) {
        return new EventHandlerGroup<>(handlers);
    }

    /**
     * 链构建器
     */
    public static class EventHandlerChainBuilder<T> {
        private final EventHandlerChain<T> chain;

        EventHandlerChainBuilder(String name, DisruptorProperties properties, DisruptorMetrics metrics) {
            this.chain = new EventHandlerChain<>(name, properties, metrics);
        }

        /**
         * 设置 RingBuffer 大小
         */
        public EventHandlerChainBuilder<T> bufferSize(int bufferSize) {
            chain.bufferSize(bufferSize);
            return this;
        }

        /**
         * 设置等待策略
         */
        public EventHandlerChainBuilder<T> waitStrategy(WaitStrategy waitStrategy) {
            chain.waitStrategy(waitStrategy);
            return this;
        }

        /**
         * 添加处理器组（并行）
         */
        @SafeVarargs
        public final EventHandlerChainBuilder<T> then(DisruptorEventHandler<T>... handlers) {
            chain.handle(handlers);
            return this;
        }

        /**
         * 添加处理器组（并行）
         */
        public EventHandlerChainBuilder<T> then(EventHandlerGroup<T> group) {
            chain.handle(group);
            return this;
        }

        /**
         * 添加处理器（串行）
         */
        @SafeVarargs
        public final EventHandlerChainBuilder<T> thenSerial(DisruptorEventHandler<T>... handlers) {
            chain.handleSerial(handlers);
            return this;
        }

        /**
         * 构建并启动
         */
        public DisruptorTemplate<T> build() {
            return chain.build();
        }
    }
}
