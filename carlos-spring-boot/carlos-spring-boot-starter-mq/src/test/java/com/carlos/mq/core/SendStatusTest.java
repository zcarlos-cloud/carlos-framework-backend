package com.carlos.mq.core;

import com.carlos.core.response.CommonErrorCode;
import com.carlos.core.response.ErrorCode;
import com.carlos.mq.error.MqErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 发送状态码表护栏测试
 * <p>
 * {@link SendStatus} 与 {@link MqErrorCode} 是同一套码值的两处呈现：
 * <b>MQ 专属状态以 {@code MqErrorCode} 为唯一真源</b>，通用状态复用
 * {@link CommonErrorCode}。本测试断言两者不漂移——否则会出现「日志里是 30003、
 * 监控按 20002 聚合」这类无法对齐的问题。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("SendStatus 码表一致性")
class SendStatusTest {

    @Test
    @DisplayName("每个状态码必须能在 MqErrorCode 或 CommonErrorCode 中找到同码定义")
    void should_map_to_existing_error_codes() {
        Map<String, String> known = new HashMap<>();
        Arrays.stream(MqErrorCode.values())
            .forEach(code -> known.put(code.getCode(), "MqErrorCode." + code.name()));
        Arrays.stream(CommonErrorCode.values())
            .forEach(code -> known.put(code.getCode(), "CommonErrorCode." + code.name()));

        assertAll(Arrays.stream(SendStatus.values())
            .map(status -> () -> assertTrue(known.containsKey(status.getCode()),
                status.name() + " 的码 " + status.getCode()
                    + " 在 MqErrorCode 与 CommonErrorCode 中都不存在（码表漂移）")));
    }

    @Test
    @DisplayName("与 MqErrorCode 同名项必须同码（单一真源）")
    void should_agree_with_mq_error_code_codes() {
        Map<String, String> mqCodes = Arrays.stream(MqErrorCode.values())
            .collect(Collectors.toMap(Enum::name, ErrorCode::getCode));

        // 逐项核对：SendStatus 里凡与 MqErrorCode 语义对应的项，码值必须一致
        assertEquals(mqCodes.get("MQ_CAPABILITY_NOT_SUPPORTED"), SendStatus.CAPABILITY_NOT_SUPPORTED.getCode());
        assertEquals(mqCodes.get("MQ_MESSAGE_DROPPED"), SendStatus.DROPPED.getCode());
        assertEquals(mqCodes.get("MQ_BACKPRESSURE_REJECTED"), SendStatus.REJECTED.getCode());
        assertEquals(mqCodes.get("MQ_TOPIC_INVALID"), SendStatus.INVALID_ARGUMENT.getCode());
        assertEquals(mqCodes.get("MQ_PAYLOAD_TOO_LARGE"), SendStatus.PAYLOAD_TOO_LARGE.getCode());
        assertEquals(mqCodes.get("MQ_DELAY_EXCEEDS_MAX"), SendStatus.DELAY_EXCEEDS_MAX.getCode());
        assertEquals(mqCodes.get("MQ_DELAY_QUEUE_FULL"), SendStatus.DELAY_QUEUE_FULL.getCode());
        assertEquals(mqCodes.get("MQ_SEND_FAILED"), SendStatus.SEND_FAILED.getCode());
        assertEquals(mqCodes.get("MQ_SEND_TIMEOUT"), SendStatus.SEND_TIMEOUT.getCode());
        assertEquals(mqCodes.get("MQ_TRANSPORT_UNAVAILABLE"), SendStatus.TRANSPORT_UNAVAILABLE.getCode());
        assertEquals(mqCodes.get("MQ_NOT_INITIALIZED"), SendStatus.NOT_INITIALIZED.getCode());
    }

    @Test
    @DisplayName("序列化状态复用通用码而非新增同义码")
    void should_reuse_common_code_for_serialization() {
        assertEquals(CommonErrorCode.SERIALIZATION_ERROR.getCode(),
            SendStatus.SERIALIZATION_ERROR.getCode());
    }

    @Test
    @DisplayName("成功与主动丢弃计为成功，其余为失败")
    void should_classify_success() {
        assertTrue(SendResult.builder().status(SendStatus.SUCCESS).build().isSuccess());
        assertTrue(SendResult.builder().status(SendStatus.DROPPED).build().isSuccess(),
            "主动丢弃不应被当成需要重试的失败");
        assertFalse(SendResult.builder().status(SendStatus.SEND_FAILED).build().isSuccess());
    }

    @Test
    @DisplayName("仅传输层临时故障可重试；能力不支持与参数错误重试无意义")
    void should_classify_retryable() {
        assertTrue(SendResult.builder().status(SendStatus.TRANSPORT_UNAVAILABLE).build().isRetryable());
        assertTrue(SendResult.builder().status(SendStatus.REJECTED).build().isRetryable());
        assertFalse(SendResult.builder().status(SendStatus.CAPABILITY_NOT_SUPPORTED).build().isRetryable());
        assertFalse(SendResult.builder().status(SendStatus.INVALID_ARGUMENT).build().isRetryable());
        assertFalse(SendResult.builder().status(SendStatus.PAYLOAD_TOO_LARGE).build().isRetryable());
    }

    @Test
    @DisplayName("asErrorCode 应携带兼容的码值与状态码")
    void should_expose_error_code() {
        ErrorCode code = SendStatus.CAPABILITY_NOT_SUPPORTED.asErrorCode();
        assertNotNull(code);
        assertEquals("20002", code.getCode());
        assertEquals(501, code.getHttpStatus());
    }
}
