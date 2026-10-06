package com.carlos.mq.client.redis;

import com.carlos.mq.config.MqProperties;
import com.carlos.mq.core.MqCapability;
import com.carlos.mq.core.MqMessage;
import com.carlos.mq.core.MqSendOptions;
import com.carlos.mq.core.SendResult;
import com.carlos.mq.core.SendStatus;
import com.carlos.mq.error.MqErrorCode;
import com.carlos.mq.exception.MqException;
import com.carlos.mq.spi.JsonMqPayloadCodec;
import com.carlos.redis.delay.RedisDelayErrorCode;
import com.carlos.redis.delay.RedisDelayException;
import com.carlos.redis.delay.RedissonDelayedQueue;
import com.carlos.redis.delay.RedissonDelayedQueueManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 传输的延时能力声明与桶解析
 *
 * <h2>本测试守卫的三个缺陷</h2>
 * <ol>
 *   <li><b>能力声明说谎</b>：原先只要延时管理器 bean 在场就声明
 *       {@link MqCapability#DELAYED}。但管理器在<b>没有到期处理器</b>时不会启动任何桶，
 *       于是每次延时发送都失败——且被映射为<b>可重试</b>的 {@code SEND_FAILED}，
 *       调用方会对一个永久性配置错误无限重试。</li>
 *   <li><b>默认桶冲突</b>：{@code TopicConfig.bucket} 曾自带默认值 {@code "default"}，
 *       与原语层默认的 {@code [normal]} 冲突；未显式配置的主题会落到一个从未启动的桶。</li>
 *   <li><b>错误码硬编码</b>：映射曾内联 {@code "50021"} / {@code "50023"} 字面量，
 *       而它们是 Redis 原语层的码，原语层改号即静默失效。</li>
 * </ol>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("RedisMqTransport 延时能力与桶解析")
class RedisMqTransportDelayCapabilityTest {

    private static final String TOPIC = "order-delay-topic";

    /**
     * 构造被测传输
     * <p>延时路径不触及 Stream 原语与消费容器，故这两个依赖传 {@code null}，让失败只可能来自
     * 延时逻辑本身；但<b>编解码器必须是真的</b>——入队前要把载荷序列化成可路由的形状。</p>
     */
    private RedisMqTransport transport(RedissonDelayedQueueManager delayManager,
                                       MqProperties properties) {
        return new RedisMqTransport(null, null, properties, new JsonMqPayloadCodec(),
            delayManager, null);
    }

    private static MqMessage message() {
        return MqMessage.of(TOPIC, "{}", null, MqSendOptions.none());
    }

    private static MqProperties propertiesWithBucket(String bucket) {
        MqProperties.TopicConfig topic = new MqProperties.TopicConfig();
        topic.setBucket(bucket);
        MqProperties properties = new MqProperties();
        properties.setTopics(Map.of(TOPIC, topic));
        return properties;
    }

    /* ==================== 能力声明 ==================== */

    @Test
    @DisplayName("延时管理器缺失时不声明 DELAYED")
    void should_not_declare_delayed_when_manager_absent() {
        Set<MqCapability> capabilities = transport(null, new MqProperties()).capabilities();

        assertFalse(capabilities.contains(MqCapability.DELAYED));
        assertTrue(capabilities.contains(MqCapability.BASIC));
    }

    @Test
    @DisplayName("管理器在场但未启动（无到期处理器）时不声明 DELAYED")
    void should_not_declare_delayed_when_manager_not_running() {
        RedissonDelayedQueueManager manager = mock(RedissonDelayedQueueManager.class);
        when(manager.isRunning()).thenReturn(false);

        assertFalse(transport(manager, new MqProperties()).capabilities()
                .contains(MqCapability.DELAYED),
            "未启动却声明 DELAYED，会让调用方重试一个永久性的配置错误");
    }

    @Test
    @DisplayName("管理器已启动时声明 DELAYED")
    void should_declare_delayed_when_manager_running() {
        RedissonDelayedQueueManager manager = mock(RedissonDelayedQueueManager.class);
        when(manager.isRunning()).thenReturn(true);

        assertTrue(transport(manager, new MqProperties()).capabilities()
            .contains(MqCapability.DELAYED));
    }

    /* ==================== 桶解析 ==================== */

    @Test
    @DisplayName("未配置主题跟随原语层默认桶，而非自带的 default")
    void should_fall_back_to_primitive_default_bucket() {
        RedissonDelayedQueueManager manager = mock(RedissonDelayedQueueManager.class);
        when(manager.isRunning()).thenReturn(true);
        when(manager.defaultBucket()).thenReturn("normal");
        when(manager.bucket("normal")).thenReturn(mock(RedissonDelayedQueue.class));

        SendResult result = transport(manager, new MqProperties())
            .sendDelayed(message(), Duration.ofMinutes(5));

        assertTrue(result.isSuccess());
        verify(manager).bucket("normal");
    }

    @Test
    @DisplayName("主题显式配置的桶优先于原语层默认桶")
    void should_prefer_explicitly_configured_bucket() {
        RedissonDelayedQueueManager manager = mock(RedissonDelayedQueueManager.class);
        when(manager.isRunning()).thenReturn(true);
        when(manager.bucket("high")).thenReturn(mock(RedissonDelayedQueue.class));

        SendResult result = transport(manager, propertiesWithBucket("high"))
            .sendDelayed(message(), Duration.ofMinutes(5));

        assertTrue(result.isSuccess());
        verify(manager).bucket("high");
    }

    @Test
    @DisplayName("原语层未配置任何桶时返回不可重试的「能力不支持」")
    void should_report_capability_not_supported_when_no_bucket_configured() {
        RedissonDelayedQueueManager manager = mock(RedissonDelayedQueueManager.class);
        when(manager.isRunning()).thenReturn(true);
        when(manager.defaultBucket()).thenReturn(null);

        SendResult result = transport(manager, new MqProperties())
            .sendDelayed(message(), Duration.ofMinutes(5));

        assertEquals(SendStatus.CAPABILITY_NOT_SUPPORTED, result.getStatus());
        assertFalse(result.isRetryable(), "配置缺失不该被报成可重试");
    }

    @Test
    @DisplayName("入队载荷携带目标主题与 Stream 字段（此前只入队裸 payload，topic 丢失）")
    void should_offer_routable_payload() {
        RedissonDelayedQueueManager manager = mock(RedissonDelayedQueueManager.class);
        RedissonDelayedQueue queue = mock(RedissonDelayedQueue.class);
        when(manager.isRunning()).thenReturn(true);
        when(manager.defaultBucket()).thenReturn("normal");
        when(manager.bucket("normal")).thenReturn(queue);

        transport(manager, new MqProperties()).sendDelayed(message(), Duration.ofMinutes(5));

        ArgumentCaptor<String> offered = ArgumentCaptor.forClass(String.class);
        verify(queue).offer(offered.capture(), any());

        MqDelayPayload payload = new JsonMqPayloadCodec()
            .deserialize(offered.getValue(), MqDelayPayload.class);
        assertEquals(TOPIC, payload.topic(), "载荷不带目标主题，到期后就无从路由");
        assertEquals("{}", payload.fields().get(MqRecordMapper.FIELD_DATA));
        assertTrue(payload.fields().containsKey(MqRecordMapper.FIELD_MESSAGE_ID));
    }

    /* ==================== 错误码映射 ==================== */

    @Test
    @DisplayName("原语层的延时错误码被正确映射（此前是硬编码字面量）")
    void should_map_primitive_delay_error_codes() {
        assertMapped(RedisDelayErrorCode.DELAY_EXCEEDS_MAX, SendStatus.DELAY_EXCEEDS_MAX);
        assertMapped(RedisDelayErrorCode.DELAY_QUEUE_FULL, SendStatus.DELAY_QUEUE_FULL);
        assertMapped(RedisDelayErrorCode.DELAY_OFFER_FAILED, SendStatus.SEND_FAILED);
    }

    private void assertMapped(RedisDelayErrorCode code, SendStatus expected) {
        RedissonDelayedQueueManager manager = mock(RedissonDelayedQueueManager.class);
        RedissonDelayedQueue queue = mock(RedissonDelayedQueue.class);
        when(manager.isRunning()).thenReturn(true);
        when(manager.defaultBucket()).thenReturn("normal");
        when(manager.bucket("normal")).thenReturn(queue);
        when(queue.offer(any(), any())).thenThrow(new RedisDelayException(code, "stub"));

        SendResult result = transport(manager, new MqProperties())
            .sendDelayed(message(), Duration.ofMinutes(5));

        assertEquals(expected, result.getStatus(), code.getCode() + " 的映射不符");
    }

    /* ==================== 启动期桶校验 ==================== */

    @Test
    @DisplayName("启动期：主题显式配置的桶不存在即拒绝启动")
    void should_fail_fast_when_configured_bucket_missing() {
        RedissonDelayedQueueManager manager = mock(RedissonDelayedQueueManager.class);
        when(manager.isRunning()).thenReturn(true);
        when(manager.bucketNames()).thenReturn(Set.of("normal"));

        MqException ex = assertThrows(MqException.class,
            () -> transport(manager, propertiesWithBucket("typo-bucket")).start());

        assertEquals(MqErrorCode.MQ_DELAY_BUCKET_NOT_FOUND.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("启动期：桶配置正确时不报错")
    void should_pass_validation_when_buckets_exist() {
        RedissonDelayedQueueManager manager = mock(RedissonDelayedQueueManager.class);
        when(manager.isRunning()).thenReturn(true);
        when(manager.bucketNames()).thenReturn(Set.of("normal"));

        assertDoesNotThrow(() -> transport(manager, propertiesWithBucket("normal")).start());
    }

    @Test
    @DisplayName("启动期：延时未启用时跳过桶校验（否则会把空桶集合误判为「全都不存在」）")
    void should_skip_bucket_validation_when_delay_disabled() {
        RedissonDelayedQueueManager manager = mock(RedissonDelayedQueueManager.class);
        when(manager.isRunning()).thenReturn(false);

        assertDoesNotThrow(() -> transport(manager, propertiesWithBucket("anything")).start());
    }
}
