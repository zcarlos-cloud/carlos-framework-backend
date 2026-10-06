package com.carlos.disruptor.chain;

import com.carlos.disruptor.config.DisruptorProperties;
import com.carlos.disruptor.core.DisruptorEvent;
import com.carlos.disruptor.core.DisruptorEventFactory;
import com.carlos.disruptor.core.DisruptorEventHandler;
import com.carlos.disruptor.core.DisruptorTemplate;
import com.carlos.disruptor.metrics.DisruptorMetrics;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * 事件处理器链构建器
 * <p>
 * 用于构建复杂的消费者处理链，支持串行、并行、菱形等模式
 * </p>
 *
 * @author Carlos
 * @date 2026-03-14
 */
@Slf4j
public class EventHandlerChain<T> {

    private final String name;
    private final DisruptorProperties properties;
    private final DisruptorMetrics metrics;
    private final List<ChainNode<T>> nodes = new ArrayList<>();

    private int bufferSize;
    private WaitStrategy waitStrategy;

    public EventHandlerChain(String name, DisruptorProperties properties) {
        this(name, properties, null);
    }

    public EventHandlerChain(String name, DisruptorProperties properties, DisruptorMetrics metrics) {
        this.name = name;
        this.properties = properties;
        this.metrics = metrics;
        this.bufferSize = properties.getBufferSize();
        this.waitStrategy = properties.getWaitStrategy().toStrategy();
    }

    /**
     * 设置 RingBuffer 大小
     *
     * @param bufferSize 大小（必须是2的幂）
     * @return 链构建器
     */
    public EventHandlerChain<T> bufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    /**
     * 设置等待策略
     *
     * @param waitStrategy 等待策略
     * @return 链构建器
     */
    public EventHandlerChain<T> waitStrategy(WaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
        return this;
    }

    /**
     * 添加处理器组（并行执行）
     *
     * @param handlers 处理器组
     * @return 链构建器
     */
    @SafeVarargs
    public final EventHandlerChain<T> handle(DisruptorEventHandler<T>... handlers) {
        nodes.add(new ChainNode<>(NodeType.PARALLEL, handlers));
        return this;
    }

    /**
     * 添加处理器组（并行执行）
     *
     * @param group 处理器组
     * @return 链构建器
     */
    public EventHandlerChain<T> handle(com.carlos.disruptor.chain.EventHandlerGroup<T> group) {
        nodes.add(new ChainNode<>(NodeType.PARALLEL, group.getHandlers()));
        return this;
    }

    /**
     * 添加处理器组（串行执行）
     *
     * @param handlers 处理器数组
     * @return 链构建器
     */
    @SafeVarargs
    public final EventHandlerChain<T> handleSerial(DisruptorEventHandler<T>... handlers) {
        for (DisruptorEventHandler<T> handler : handlers) {
            nodes.add(new ChainNode<>(NodeType.SERIAL, handler));
        }
        return this;
    }

    /**
     * 构建并启动 Disruptor
     *
     * @return DisruptorTemplate
     */
    @SuppressWarnings("unchecked")
    public DisruptorTemplate<T> build() {
        // 创建线程工厂
        ThreadFactory threadFactory = new CustomizableThreadFactory(
            properties.getThreadPool().getThreadNamePrefix() + name + "-");

        // 创建 Disruptor
        Disruptor<DisruptorEvent<T>> disruptor = new Disruptor<>(
            DisruptorEventFactory.getInstance(),
            bufferSize,
            threadFactory,
            ProducerType.MULTI,
            waitStrategy
        );

        // 构建消费者链
        EventHandlerGroup<DisruptorEvent<T>> lastGroup = null;

        for (ChainNode<T> node : nodes) {
            EventHandler<DisruptorEvent<T>>[] handlers = wrapHandlers(node.getHandlers());

            if (lastGroup == null) {
                // 第一个节点
                lastGroup = disruptor.handleEventsWith(handlers);
            } else {
                // 后续节点
                lastGroup = lastGroup.then(handlers);
            }
        }

        // 启动 Disruptor
        RingBuffer<DisruptorEvent<T>> ringBuffer = disruptor.start();

        if (log.isInfoEnabled()) {
            log.info("[{}] Disruptor chain started. Nodes={}, bufferSize={}",
                name, nodes.size(), bufferSize);
        }

        return new DisruptorTemplate<>(name, ringBuffer, metrics);
    }

    /**
     * 包装业务处理器
     */
    @SuppressWarnings("unchecked")
    private EventHandler<DisruptorEvent<T>>[] wrapHandlers(List<DisruptorEventHandler<T>> handlers) {
        return handlers.stream()
            .map(h -> (EventHandler<DisruptorEvent<T>>) (event, sequence, endOfBatch) -> {
                try {
                    h.onEvent(event, sequence, endOfBatch);
                } catch (Exception e) {
                    if (h.ignoreException()) {
                        log.warn("[{}] Handler ignored exception: {}", h.getHandlerName(), e.getMessage());
                        h.onException(event, sequence, e);
                    } else {
                        throw e;
                    }
                }
            })
            .toArray(EventHandler[]::new);
    }

    /**
     * 链节点类型
     */
    private enum NodeType {
        PARALLEL,  // 并行
        SERIAL     // 串行
    }

    /**
     * 链节点
     */
    private static class ChainNode<T> {
        private final NodeType type;
        private final List<DisruptorEventHandler<T>> handlers;

        @SafeVarargs
        ChainNode(NodeType type, DisruptorEventHandler<T>... handlers) {
            this.type = type;
            this.handlers = new ArrayList<>(List.of(handlers));
        }

        ChainNode(NodeType type, List<DisruptorEventHandler<T>> handlers) {
            this.type = type;
            this.handlers = handlers;
        }

        NodeType getType() {
            return type;
        }

        List<DisruptorEventHandler<T>> getHandlers() {
            return handlers;
        }
    }
}
