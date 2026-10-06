package com.carlos.mq.config;

import com.carlos.mq.spi.MqTransport;
import com.carlos.mq.spi.MqTransportRegistry;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

/**
 * MQ 传输生命周期
 * <p>
 * <b>为什么不用 {@code @Bean(initMethod = "start")}</b>：{@code @ConditionalOnProperty}
 * 不支持否定条件，难以表达「type=none 时不装配传输」。改为在配置齐全后统一决策：
 * 注册表能选出传输才启动，{@code type=none} 时 {@code currentOrEmpty()} 为空，
 * 自然什么也不启动。
 * </p>
 * <p>
 * <b>为什么在 {@link ApplicationReadyEvent} 而非 {@code @PostConstruct} 启动</b>：
 * 该阶段 Redis 拓扑发现已完成，且把「应用启动失败」与「Redis 抖动」解耦。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class MqTransportLifecycle {

    private final MqTransportRegistry registry;

    /**
     * 应用就绪后启动当前传输
     * <p>
     * <b>{@code @Order(0)} 是必需的，不是装饰</b>：消费侧协调器为
     * {@code @Order(10)}，且其 javadoc 声明「晚于传输启动」。此前本方法未标注顺序，
     * 实际落到 {@code LOWEST_PRECEDENCE}（{@code Ordered} 的默认值），
     * <b>反而排在协调器之后</b>——
     * 即「订阅先于传输启动」，与约定相反（当前 Redis 传输恰好能容忍，但顺序是错的）。
     * </p>
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(0)
    public void onReady() {
        registry.currentOrEmpty().ifPresent(transport -> {
            transport.start();
            log.info("MQ 传输已启动：{} capabilities={}",
                transport.getName(), transport.capabilities());
        });
    }

    /**
     * 关闭时停止传输
     */
    @PreDestroy
    public void onShutdown() {
        registry.currentOrEmpty().ifPresent(MqTransport::stop);
    }
}
