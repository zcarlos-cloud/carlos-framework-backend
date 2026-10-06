package com.carlos.mq.config;

import com.carlos.mq.core.MqTemplate;
import com.carlos.mq.health.MqHealthIndicator;
import com.carlos.mq.spi.MqTransportRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * MQ 自动配置入口
 * <p>
 * 传输装配见 {@link MqTransportAutoConfiguration}。本类负责整体开关、生命周期与健康检查。
 * </p>
 * <p>
 * <b>本次修正</b>：移除旧实现两处问题 —— ① 配置类自身构造依赖 {@code MqTemplate}
 * 而该 bean 又由同一个类定义（自引用环）；② 「检测不到传输就装配 NoOp」的隐式兜底
 * 属静默降级，现改为：无传输时启动失败，仅 {@code type=none} 才装配 NoOp。
 * </p>
 *
 * @author Carlos
 * @date 2026/3/14
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(MqProperties.class)
@ConditionalOnProperty(prefix = "carlos.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(MqTransportAutoConfiguration.class)
public class MqAutoConfiguration {

    /**
     * 传输生命周期（就绪后启动、关闭时停止）
     */
    @Bean
    @ConditionalOnMissingBean
    public MqTransportLifecycle mqTransportLifecycle(MqTransportRegistry registry) {
        return new MqTransportLifecycle(registry);
    }

    /**
     * MQ 健康检查
     */
    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnMissingBean
    public MqHealthIndicator mqHealthIndicator(MqTemplate mqTemplate) {
        return new MqHealthIndicator(mqTemplate);
    }
}
