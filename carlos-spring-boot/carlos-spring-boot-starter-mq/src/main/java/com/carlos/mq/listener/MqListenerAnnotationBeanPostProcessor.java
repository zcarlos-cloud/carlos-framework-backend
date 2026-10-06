package com.carlos.mq.listener;

import com.carlos.mq.annotation.MqListener;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.error.MqErrorCode;
import com.carlos.mq.exception.MqException;
import com.carlos.mq.support.ConsumeMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * {@code @MqListener} 注解处理器
 *
 * <h2>这是本次重构补上的最大缺口</h2>
 * <p>
 * 在此之前，{@code @MqListener} 注解<b>没有任何处理器</b>——写成注解的方法永远不会被调用，
 * 且整个模块的消费链路（{@code subscribe}）也只是把监听器塞进一个无人读取的 Map。
 * 本类是「注解第一次真正生效」的关键。
 * </p>
 *
 * <h2>支持的处理器形状（3 + 1 种）</h2>
 * <ul>
 *   <li>{@code void handle(OrderMessage msg)} —— 只接收业务负载（推荐）</li>
 *   <li>{@code void handle(OrderMessage msg, MqMessage envelope)} —— 负载 + 信封（需要投递次数等元信息时）</li>
 *   <li>{@code void handle(MqMessage envelope)} —— 只接收信封（负载类型不定时）</li>
 *   <li>{@code void handle()} —— 无参</li>
 * </ul>
 * <p>返回值允许 {@code void} 或 {@link com.carlos.mq.core.ConsumeResult}。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class MqListenerAnnotationBeanPostProcessor implements BeanPostProcessor {

    private final MqListenerRegistry registry;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        for (Method method : bean.getClass().getMethods()) {
            MqListener annotation = method.getAnnotation(MqListener.class);
            if (annotation == null) {
                continue;
            }
            registerEndpoint(bean, beanName, method, annotation);
        }
        return bean;
    }

    private void registerEndpoint(Object bean, String beanName, Method method, MqListener annotation) {
        validateReturnType(beanName, method);

        MqListenerEndpoint.HandlerShape shape = resolveShape(beanName, method);
        Class<?> payloadType = resolvePayloadType(shape, method);

        for (String topic : annotation.topic()) {
            if (topic == null || topic.isBlank()) {
                throw new MqException(MqErrorCode.MQ_LISTENER_CONFIG_INVALID,
                    "%s 的 @MqListener.topic 存在空白值".formatted(beanName + "#" + method.getName()));
            }
            String group = annotation.group() == null || annotation.group().isBlank()
                ? topic + ":default" : annotation.group();
            registry.register(new MqListenerEndpoint(
                beanName,
                bean,
                method,
                topic,
                group,
                annotation.consumerId(),
                annotation.consumeMode() == null ? ConsumeMode.CLUSTER : annotation.consumeMode(),
                Math.max(1, annotation.concurrency()),
                Math.max(1, annotation.batchSize()),
                annotation.maxDeliveryCount(),
                shape,
                payloadType));
            log.info("注册 MQ 监听端点：{} → topic={}, group={}, concurrency={}",
                beanName + "#" + method.getName(), topic, group, annotation.concurrency());
        }
    }

    /**
     * 校验返回类型
     *
     * @param beanName bean 名
     * @param method   方法
     */
    private void validateReturnType(String beanName, Method method) {
        Class<?> returnType = method.getReturnType();
        boolean allowed = returnType == void.class
            || returnType == Void.class
            || com.carlos.mq.core.ConsumeResult.class.isAssignableFrom(returnType);
        if (!allowed) {
            throw new MqException(MqErrorCode.MQ_LISTENER_CONFIG_INVALID,
                "%s 的返回类型 %s 不受支持：只允许 void 或 ConsumeResult"
                    .formatted(beanName + "#" + method.getName(), returnType.getName()));
        }
    }

    /**
     * 判定入参形状
     *
     * @param beanName bean 名
     * @param method   方法
     * @return 形状
     */
    private MqListenerEndpoint.HandlerShape resolveShape(String beanName, Method method) {
        Class<?>[] types = method.getParameterTypes();
        String display = beanName + "#" + method.getName();
        return switch (types.length) {
            case 0 -> MqListenerEndpoint.HandlerShape.NO_ARGS;
            case 1 -> MqMessage.class.isAssignableFrom(types[0])
                ? MqListenerEndpoint.HandlerShape.MESSAGE
                : MqListenerEndpoint.HandlerShape.PAYLOAD;
            case 2 -> {
                if (MqMessage.class.isAssignableFrom(types[1])) {
                    yield MqListenerEndpoint.HandlerShape.PAYLOAD_WITH_MESSAGE;
                }
                throw new MqException(MqErrorCode.MQ_LISTENER_CONFIG_INVALID,
                    "%s 的双参形式必须是 (负载, MqMessage)".formatted(display));
            }
            default -> throw new MqException(MqErrorCode.MQ_LISTENER_CONFIG_INVALID,
                "%s 的参数个数 %d 不受支持（最多 2 个）".formatted(display, types.length));
        };
    }

    /**
     * 判定负载类型（仅 PAYLOAD 形状需要）
     *
     * @param shape  形状
     * @param method 方法
     * @return 负载类型；不需要时为 null
     */
    private Class<?> resolvePayloadType(MqListenerEndpoint.HandlerShape shape, Method method) {
        return switch (shape) {
            case PAYLOAD, PAYLOAD_WITH_MESSAGE -> method.getParameterTypes()[0];
            default -> null;
        };
    }

    /**
     * 便于诊断：端点形状名
     *
     * @param endpoint 端点
     * @return 形如 {@code PAYLOAD(com.x.OrderMessage)}
     */
    public static String describe(MqListenerEndpoint endpoint) {
        return endpoint.shape() + "(%s)".formatted(
            endpoint.payloadType() == null ? "-" : endpoint.payloadType().getName());
    }

    /**
     * 便于诊断：主题列表
     *
     * @param annotation 注解
     * @return 主题串
     */
    public static String topicsOf(MqListener annotation) {
        return Arrays.toString(annotation.topic());
    }
}
