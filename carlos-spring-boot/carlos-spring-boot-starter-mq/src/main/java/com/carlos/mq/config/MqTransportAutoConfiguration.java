package com.carlos.mq.config;

import com.carlos.mq.client.redis.MqTopics;
import com.carlos.mq.client.redis.RedisMqConsumerContainer;
import com.carlos.mq.client.redis.RedisMqDelayDueHandler;
import com.carlos.mq.client.redis.RedisMqPendingReclaimer;
import com.carlos.mq.client.redis.RedisMqTransport;
import com.carlos.mq.consumer.MqConsumerCoordinator;
import com.carlos.mq.consumer.MqConsumerNameFactory;
import com.carlos.mq.consumer.RedisMqDeadLetterHandler;
import com.carlos.mq.core.MqPayloadCodec;
import com.carlos.mq.core.MqTemplate;
import com.carlos.mq.core.NoOpMqTemplate;
import com.carlos.mq.error.MqErrorCode;
import com.carlos.mq.exception.MqException;
import com.carlos.mq.listener.MqHandlerMethodAdapter;
import com.carlos.mq.listener.MqListenerAnnotationBeanPostProcessor;
import com.carlos.mq.listener.MqListenerRegistry;
import com.carlos.mq.metrics.MqMetrics;
import com.carlos.mq.spi.DefaultMqTemplate;
import com.carlos.mq.spi.JsonMqPayloadCodec;
import com.carlos.mq.spi.MqTransport;
import com.carlos.mq.spi.MqTransportRegistry;
import com.carlos.mq.support.MqType;
import com.carlos.redis.delay.DelayDueHandler;
import com.carlos.redis.delay.RedissonDelayedQueueManager;
import com.carlos.redis.stream.RedisStreamProperties;
import com.carlos.redis.stream.RedisStreamTemplate;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * MQ 装配（传输 + 消费侧）
 *
 * <h2>为什么合并为一个自动配置类</h2>
 * <p>
 * 传输 bean 需要注入消费容器，而消费容器若定义在<b>另一个</b>「之后处理」的自动配置里，
 * 其 bean 定义在传输 bean 创建时尚未注册，注入将失败（自动配置的处理顺序早于 bean 创建，
 * 这类问题排查成本很高）。合并后同源有序，无需依赖顺序约定。
 * </p>
 *
 * <h2>「无可用传输」的处理（fail-fast，不静默降级）</h2>
 * <ul>
 *   <li>{@code carlos.mq.type=none} —— 显式声明本次不接 MQ，装配 {@link NoOpMqTemplate}
 *       （能力集合为空，任何发送都返回明确的失败结果）。</li>
 *   <li>类型为 {@code auto}/{@code redis} 但没有任何传输实现 —— <b>直接启动失败</b>，
 *       而不是悄悄换成一个什么都不做的实现。</li>
 * </ul>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(MqTemplate.class)
@ConditionalOnProperty(prefix = "carlos.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MqTransportAutoConfiguration {

    /* ==================== 编解码与命名 ==================== */

    /**
     * 负载编解码（默认走 carlos-spring-boot-starter-json）
     */
    @Bean
    @ConditionalOnMissingBean
    public MqPayloadCodec mqPayloadCodec() {
        return new JsonMqPayloadCodec();
    }

    /**
     * 处理器方法调用适配器
     */
    @Bean
    @ConditionalOnMissingBean
    public MqHandlerMethodAdapter mqHandlerMethodAdapter(MqPayloadCodec payloadCodec) {
        return new MqHandlerMethodAdapter(payloadCodec);
    }

    /**
     * 消费者名工厂（每实例唯一）
     */
    @Bean
    @ConditionalOnMissingBean
    public MqConsumerNameFactory mqConsumerNameFactory(Environment environment) {
        return new MqConsumerNameFactory(environment);
    }

    /* ==================== 注解消费侧 ==================== */

    /**
     * 监听端点注册表
     */
    @Bean
    @ConditionalOnMissingBean
    public MqListenerRegistry mqListenerRegistry() {
        return new MqListenerRegistry();
    }

    /**
     * {@code @MqListener} 注解处理器
     * <p>
     * 声明为 {@code static} 方法：BeanPostProcessor 属于基础设施 bean，
     * 静态工厂可避免所在配置类被过早实例化。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public static MqListenerAnnotationBeanPostProcessor mqListenerAnnotationBeanPostProcessor(
        MqListenerRegistry registry) {
        return new MqListenerAnnotationBeanPostProcessor(registry);
    }

    /**
     * 启动期能力校验器
     */
    @Bean
    @ConditionalOnMissingBean
    public MqCapabilityValidator mqCapabilityValidator(MqProperties mqProperties) {
        return new MqCapabilityValidator(mqProperties);
    }

    /* ==================== Redis 传输链路 ==================== */

    /**
     * Redis 传输的命名规约（topic → key）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RedisStreamTemplate.class)
    public MqTopics mqTopics(RedisStreamProperties redisStreamProperties) {
        return new MqTopics(redisStreamProperties.getKeyPrefix());
    }

    /**
     * MQ 指标（MeterRegistry 缺失时自动降级为空操作）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RedisStreamTemplate.class)
    public MqMetrics mqMetrics(ObjectProvider<MeterRegistry> registryProvider,
                               RedisStreamTemplate redisStreamTemplate) {
        return new MqMetrics(registryProvider.getIfAvailable(), redisStreamTemplate);
    }

    /**
     * 死信处理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RedisStreamTemplate.class)
    public RedisMqDeadLetterHandler redisMqDeadLetterHandler(RedisStreamTemplate redisStreamTemplate,
                                                            MqTopics mqTopics,
                                                            MqProperties mqProperties) {
        return new RedisMqDeadLetterHandler(redisStreamTemplate, mqTopics, mqProperties);
    }

    /**
     * 消费容器（关闭时随 bean 销毁停止）
     */
    @Bean(destroyMethod = "stop")
    @ConditionalOnMissingBean
    @ConditionalOnClass(RedisStreamTemplate.class)
    public RedisMqConsumerContainer redisMqConsumerContainer(
        RedisConnectionFactory connectionFactory,
        RedisStreamTemplate redisStreamTemplate,
        MqTopics mqTopics,
        MqProperties mqProperties,
        RedisStreamProperties redisStreamProperties,
        MqConsumerNameFactory mqConsumerNameFactory,
        RedisMqDeadLetterHandler deadLetterHandler,
        MqMetrics mqMetrics) {
        return new RedisMqConsumerContainer(connectionFactory, redisStreamTemplate, mqTopics,
            mqProperties, redisStreamProperties, mqConsumerNameFactory, deadLetterHandler, mqMetrics);
    }

    /**
     * 待处理条目认领器
     */
    @Bean(destroyMethod = "stop")
    @ConditionalOnMissingBean
    @ConditionalOnClass(RedisStreamTemplate.class)
    public RedisMqPendingReclaimer redisMqPendingReclaimer(
        RedisStreamTemplate redisStreamTemplate,
        RedisStreamProperties redisStreamProperties,
        MqProperties mqProperties,
        MqTopics mqTopics,
        RedisMqDeadLetterHandler deadLetterHandler,
        MqConsumerNameFactory mqConsumerNameFactory,
        MqMetrics mqMetrics) {
        return new RedisMqPendingReclaimer(redisStreamTemplate, redisStreamProperties, mqProperties,
            mqTopics, deadLetterHandler, mqConsumerNameFactory, mqMetrics);
    }

    /**
     * 延时到期处理器：把到期条目投递回其原主题的 Stream
     * <p>
     * <b>本 bean 是延时能力真正可用的前提</b>：原语层的延时队列在缺少
     * {@link DelayDueHandler} 实现时<b>不会启动任何桶</b>，任何入队都会失败。
     * 本模块此前从未提供它——这正是 {@code DELAYED} 长期「被声明却无法兑现」的根因
     * （详见 {@link RedisMqDelayDueHandler}）。
     * </p>
     * <p>
     * 返回类型声明为<b>接口</b>而非实现类：{@code @ConditionalOnMissingBean} 检查的正是
     * 原语层查找的那个类型，业务方提供自己的 {@link DelayDueHandler} 即可整体替换。
     * </p>
     * <p>
     * <b>为什么无需声明自动配置顺序</b>：原语层的管理器通过
     * {@code ObjectProvider#getIfAvailable()} 在<b>bean 创建时</b>才查找本 bean，
     * 而全部 bean 定义在配置类处理阶段就已注册完毕，故与两个自动配置类的处理先后无关。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RedisStreamTemplate.class)
    public DelayDueHandler redisMqDelayDueHandler(RedisStreamTemplate redisStreamTemplate,
                                                  MqTopics mqTopics,
                                                  MqProperties mqProperties,
                                                  MqPayloadCodec payloadCodec) {
        return new RedisMqDelayDueHandler(redisStreamTemplate, mqTopics, mqProperties, payloadCodec);
    }

    /**
     * Redis Stream 传输实现
     * <p>
     * 延时能力取决于是否存在 {@link RedissonDelayedQueueManager}（由 redis 模块的延时自动配置
     * 提供）；消费能力取决于是否存在 {@link RedisMqConsumerContainer}。
     * 缺失时对应能力<b>不会被声明</b>，调用会得到明确的「能力不支持」。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RedisStreamTemplate.class)
    public RedisMqTransport redisMqTransport(RedisStreamTemplate redisStreamTemplate,
                                            MqTopics mqTopics,
                                            MqProperties mqProperties,
                                            MqPayloadCodec payloadCodec,
                                            ObjectProvider<RedissonDelayedQueueManager> delayProvider,
                                            ObjectProvider<RedisMqConsumerContainer> consumerProvider) {
        return new RedisMqTransport(redisStreamTemplate, mqTopics, mqProperties, payloadCodec,
            delayProvider.getIfAvailable(), consumerProvider.getIfAvailable());
    }

    /* ==================== 装配与坐标 ==================== */

    /**
     * 传输注册表（收集容器中全部 {@link MqTransport}，按配置的 type 选定当前传输）
     */
    @Bean
    @ConditionalOnMissingBean
    public MqTransportRegistry mqTransportRegistry(ObjectProvider<MqTransport> transports,
                                                   MqProperties mqProperties) {
        MqTransportRegistry registry = new MqTransportRegistry(transports, mqProperties.getType());
        log.info("MQ 传输注册表就绪：type={}, 已注册={}, 当前={}",
            mqProperties.getType(), registry.availableTypes(),
            registry.currentOrEmpty().map(MqTransport::getName).orElse("无"));
        return registry;
    }

    /**
     * 业务侧唯一依赖的 MQ 入口
     */
    @Bean
    @ConditionalOnMissingBean
    public MqTemplate mqTemplate(MqTransportRegistry registry,
                                 MqPayloadCodec payloadCodec,
                                 MqProperties mqProperties) {
        if (mqProperties.getType() == MqType.NONE) {
            log.info("carlos.mq.type=none：装配 NoOpMqTemplate，本次不投递任何消息");
            return new NoOpMqTemplate();
        }
        return registry.currentOrEmpty()
            .map(transport -> (MqTemplate) new DefaultMqTemplate(
                transport, payloadCodec, mqProperties.getMaxPayloadBytes()))
            .orElseThrow(() -> new MqException(MqErrorCode.MQ_TRANSPORT_NOT_FOUND,
                "没有可用的 MQ 传输实现，已注册：%s。请确认已引入对应传输依赖，"
                    .formatted(registry.availableTypes())
                    + "或在不需要 MQ 时显式配置 carlos.mq.type=none"));
    }

    /**
     * 消费侧协调器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RedisStreamTemplate.class)
    public MqConsumerCoordinator mqConsumerCoordinator(MqListenerRegistry listenerRegistry,
                                                       MqHandlerMethodAdapter handlerAdapter,
                                                       MqConsumerNameFactory nameFactory,
                                                       MqCapabilityValidator capabilityValidator,
                                                       MqProperties mqProperties,
                                                       MqTransportRegistry transportRegistry,
                                                       RedisMqPendingReclaimer reclaimer) {
        return new MqConsumerCoordinator(listenerRegistry, handlerAdapter, nameFactory,
            capabilityValidator, mqProperties, transportRegistry, reclaimer);
    }
}
