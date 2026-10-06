package com.carlos.mq.listener;

import com.carlos.mq.core.ConsumeResult;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.core.MqPayloadCodec;
import com.carlos.mq.error.MqErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;

/**
 * 处理器方法调用适配器
 * <p>
 * 负责把「信封」适配成业务方法的入参，并把返回值/异常翻译成
 * {@link ConsumeResult}（即框架的四个可靠性出口）。
 * </p>
 *
 * <h2>异常与返回值的映射（保守优先：不丢消息）</h2>
 * <ul>
 *   <li>返回 {@code void} 或无返回 → {@code SUCCESS}（确认）</li>
 *   <li>返回 {@link ConsumeResult} → 原样采用（业务显式控制）</li>
 *   <li>返回其它对象 → {@code SUCCESS}（约定：无声明即视为成功）</li>
 *   <li>业务抛异常 → {@code RETRY}（<b>不确认</b>）。理由：「代码缺陷」与「下游暂时不可用」
 *       在框架视角无法区分，保守选择是不丢——由投递次数上限兜住无限重试。</li>
 *   <li>负载反序列化失败 → {@code DROP}（确认并写死信）。这类消息<b>无法被任何重试修复</b>，
 *       留在待处理队列只会阻塞后续消息。</li>
 * </ul>
 * <p>
 * 反序列化被刻意放在方法调用<b>之前</b>、并单独捕获，从而无需引入任何自定义运行时异常
 * 来表达「转换失败」——转换失败只是一条分支，不是异常控制流。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class MqHandlerMethodAdapter {

    private final MqPayloadCodec payloadCodec;

    /**
     * 调用处理器方法
     *
     * @param endpoint 端点
     * @param message  消息信封
     * @return 消费结果（永不为 null）
     */
    public ConsumeResult handle(MqListenerEndpoint endpoint, MqMessage message) {
        long begin = System.currentTimeMillis();

        Object[] args;
        try {
            args = buildArgs(endpoint, message);
        } catch (Exception e) {
            log.error("负载反序列化失败，转入放弃分支：listener={}, topic={}, messageId={}, payloadType={}",
                endpoint.display(), message.topic(), message.messageId(), endpoint.payloadType(), e);
            ConsumeResult dropped = ConsumeResult.drop(message.messageId(), message.topic(),
                "反序列化失败：" + e.getMessage());
            dropped.setErrorCode(MqErrorCode.MQ_MESSAGE_DESERIALIZE_FAILED.getCode());
            return finish(dropped, message, begin);
        }

        ConsumeResult result;
        try {
            Object returned = endpoint.method().invoke(endpoint.bean(), args);
            result = toResult(returned, message);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            log.error("处理器执行异常，转重试：listener={}, topic={}, messageId={}",
                endpoint.display(), message.topic(), message.messageId(), cause);
            result = ConsumeResult.retry(message.messageId(), message.topic(),
                cause == null ? "未知异常" : cause.getClass().getSimpleName() + ": " + cause.getMessage());
            result.setErrorCode(MqErrorCode.MQ_CONSUME_FAILED.getCode());
        } catch (Exception e) {
            log.error("处理器调用失败，转重试：listener={}", endpoint.display(), e);
            result = ConsumeResult.retry(message.messageId(), message.topic(),
                "调用失败：" + e.getMessage());
            result.setErrorCode(MqErrorCode.MQ_CONSUME_FAILED.getCode());
        }
        return finish(result, message, begin);
    }

    /**
     * 构建方法入参
     *
     * @param endpoint 端点
     * @param message  信封
     * @return 入参数组
     */
    private Object[] buildArgs(MqListenerEndpoint endpoint, MqMessage message) {
        return switch (endpoint.shape()) {
            case NO_ARGS -> new Object[0];
            case MESSAGE -> new Object[]{message};
            case PAYLOAD -> new Object[]{deserialize(endpoint, message)};
            case PAYLOAD_WITH_MESSAGE -> new Object[]{deserialize(endpoint, message), message};
        };
    }

    private Object deserialize(MqListenerEndpoint endpoint, MqMessage message) {
        Class<?> type = endpoint.payloadType();
        if (type == null || type == MqMessage.class) {
            return message;
        }
        if (type == String.class) {
            return message.payload();
        }
        return payloadCodec.deserialize(message.payload(), type);
    }

    private ConsumeResult toResult(Object returned, MqMessage message) {
        if (returned instanceof ConsumeResult consumeResult) {
            return consumeResult;
        }
        return ConsumeResult.success(message.messageId(), message.topic());
    }

    private ConsumeResult finish(ConsumeResult result, MqMessage message, long begin) {
        result.setConsumeCostTime(System.currentTimeMillis() - begin);
        result.setDeliveryCount(message.deliveryCount());
        return result;
    }
}
