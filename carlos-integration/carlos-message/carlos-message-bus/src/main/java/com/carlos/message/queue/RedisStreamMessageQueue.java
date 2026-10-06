package com.carlos.message.queue;

import com.carlos.message.core.protocol.MessageContext;
import com.carlos.message.core.queue.MessageQueue;
import com.carlos.message.exception.MessageException;
import com.carlos.message.exception.MessageSendException;
import com.carlos.mq.core.MqSendOptions;
import com.carlos.mq.core.MqTemplate;
import com.carlos.mq.core.SendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

/**
 * <p>
 * 消息中心队列的<b>薄适配器</b>：把 {@link MessageContext} 的业务语义翻译为框架级 MQ 的
 * 主题与信封，投递与可靠性（消费者组、确认、重投、死信、延时）全部委托给
 * {@code carlos-spring-boot-starter-mq}。
 * </p>
 *
 * <h2>本次迁移改掉了什么</h2>
 * <p>
 * 迁移前本类是一个 309 行的自持实现（直连 {@code StringRedisTemplate} 与 Redisson
 * {@code RDelayedQueue}），与框架重复，且携带若干已确认的缺陷。迁移后：
 * </p>
 * <ul>
 *   <li><b>按渠道分流</b>：原实现的分区键只有优先级（{@code stream:message:high|normal|low}），
 *       <b>完全没有渠道维度</b>，导致短信与邮件混在同一个 Stream 里、消费端无法按渠道分组消费。
 *       现改为<b>一个渠道一个主题</b>（{@code message:{channelCode}}）。</li>
 *   <li><b>失败不再时抛时不抛</b>：原实现只捕获序列化异常（静默丢弃）而把 Redis 异常上抛，
 *       与「投递到队列」的心智不符。现在统一为「投递未成功即抛异常」，由调用方决定补偿。</li>
 *   <li><b>时区</b>：{@link #scheduleAt(MessageContext, Instant)} 用绝对时刻，
 *       不再依赖 {@code ZoneId.systemDefault()}（原实现跨时区整体偏移）。</li>
 *   <li><b>队列长度上限</b>：原实现的 {@code XADD} 无上限，Stream 只增不减；
 *       现由框架原语层默认带上 {@code MAXLEN ~ N}（共用实例下的核心保护项）。</li>
 *   <li><b>不再销毁延时结构</b>：原 {@code @PreDestroy} 调用 {@code RDelayedQueue.destroy()}，
 *       会删除内部有序集合导致未到期消息全部丢失；现由框架的延时原语管理，关闭时保留未到期项。</li>
 * </ul>
 *
 * <p>
 * <b>主题命名</b>：{@code message:{channelCode}}，最终物理键由框架加前缀构成
 * （默认 {@code framework:mq:stream:message:{channelCode}}）。
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carlos.message.queue", name = "enabled",
    havingValue = "true", matchIfMissing = true)
public class RedisStreamMessageQueue implements MessageQueue {

    /** 主题前缀（一个渠道一个主题） */
    private static final String TOPIC_PREFIX = "message:";

    /** 渠道缺失时的兜底主题段 */
    private static final String UNKNOWN_CHANNEL = "unknown";

    private final MqTemplate mqTemplate;

    // ==================== 投递 ====================

    @Override
    public void enqueue(MessageContext context) {
        SendResult result = mqTemplate.send(topicOf(context), context, optionsOf(context));
        requireSuccess(result, context, "投递");
    }

    @Override
    public void delay(MessageContext context, long delayMillis) {
        if (delayMillis <= 0) {
            // 非正延迟等价于立即投递，省掉一次延时结构往返
            enqueue(context);
            return;
        }
        SendResult result = mqTemplate.sendDelayed(topicOf(context), context,
            Duration.ofMillis(delayMillis));
        requireSuccess(result, context, "延时投递");
    }

    @Override
    public void scheduleAt(MessageContext context, Instant deliverAt) {
        if (deliverAt == null) {
            enqueue(context);
            return;
        }
        long delayMillis = Duration.between(Instant.now(), deliverAt).toMillis();
        if (delayMillis <= 0) {
            log.debug("定时投递时刻已过，按立即投递处理：messageId={}", context.getMessageId());
            enqueue(context);
            return;
        }
        delay(context, delayMillis);
    }

    // ==================== 消费（已废弃） ====================

    /**
     * {@inheritDoc}
     * <p>
     * 明确拒绝而不是静默空转：消费已统一由框架的 {@code @MqListener} 注解承担。
     * 迁移前本方法虽存在但<b>全无调用方</b>，因此拒绝不会破坏既有链路。
     * </p>
     */
    @Deprecated(since = "3.0.0")
    @Override
    public void consume(int priority, Consumer<MessageContext> handler) {
        throw new MessageException(
            "编程式消费已移除，请改用框架的 @MqListener 注解（自带消费者组、确认、重投与死信）");
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated 同 {@link #consume(int, Consumer)}。
     */
    @Deprecated(since = "3.0.0")
    @Override
    public void consumeBatch(int priority, int batchSize, Consumer<List<MessageContext>> handler) {
        throw new MessageException("编程式批量消费已移除，请改用框架的 @MqListener 注解");
    }

    // ==================== 内部 ====================

    /**
     * 主题 = 前缀 + 渠道编码
     * <p>
     * 这是本次迁移修掉的既有缺陷：原实现的分区键只有优先级，没有渠道维度。
     * </p>
     */
    private String topicOf(MessageContext context) {
        String channel = context.getChannelCode();
        if (channel == null || channel.isBlank()) {
            log.warn("消息缺少渠道编码，将投递到兜底主题：messageId={}", context.getMessageId());
            channel = UNKNOWN_CHANNEL;
        }
        return TOPIC_PREFIX + channel;
    }

    /**
     * 组装发送选项：业务键 + 便于观测的消息头
     * <p>消息头仅用于观测与排查，<b>不作为路由依据</b>——路由靠主题。</p>
     */
    private MqSendOptions optionsOf(MessageContext context) {
        MqSendOptions options = MqSendOptions.ofKey(context.getMessageId());
        options = withHeader(options, "channelCode", context.getChannelCode());
        options = withHeader(options, "messageType", context.getMessageType());
        if (context.getPriority() != null) {
            options = withHeader(options, "priority", String.valueOf(context.getPriority()));
        }
        return options;
    }

    /**
     * 只在值非空时写入消息头（不可变 Map 不接受 null 值）
     */
    private MqSendOptions withHeader(MqSendOptions options, String name, String value) {
        return value == null ? options : options.withHeader(name, value);
    }

    /**
     * 投递未成功即抛异常
     * <p>
     * 原实现「序列化失败静默丢弃、Redis 失败上抛」的不一致行为在此统一为「一律上抛」，
     * 让调用方能感知并按业务补偿（例如标记接收人失败并进入业务侧重试）。
     * </p>
     */
    private void requireSuccess(SendResult result, MessageContext context, String action) {
        if (result.isSuccess()) {
            log.debug("{}成功：messageId={}, topic={}, recordId={}",
                action, context.getMessageId(), result.getTopic(), result.getRecordId());
            return;
        }
        log.error("{}失败：messageId={}, status={}, retryable={}, detail={}",
            action, context.getMessageId(), result.getStatus(), result.isRetryable(),
            result.getMessage());
        throw new MessageSendException(
            "%s失败：messageId=%s, status=%s".formatted(action, context.getMessageId(),
                result.getStatus()));
    }
}
