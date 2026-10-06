package com.carlos.audit.api.log;

import com.carlos.audit.api.constant.AuditMqTopics;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.log.entity.OperationLog;
import com.carlos.log.storage.LoggingLogStorage;
import com.carlos.mq.core.MqSendOptions;
import com.carlos.mq.core.MqTemplate;
import com.carlos.mq.core.SendResult;
import com.carlos.mq.core.SendStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuditMqLogStorage} 单元测试
 *
 * @author Carlos
 */
@ExtendWith(MockitoExtension.class)
class AuditMqLogStorageTest {

    @Mock
    private MqTemplate mqTemplate;

    @Captor
    private ArgumentCaptor<Object> payloadCaptor;

    @InjectMocks
    private AuditMqLogStorage storage;

    @Test
    @DisplayName("正常日志应投递到 audit:log 主题，并以 traceId 作为消息键")
    void should_publish_to_audit_topic_with_trace_id_key() {
        when(mqTemplate.send(eq(AuditMqTopics.AUDIT_LOG), any(), any()))
            .thenReturn(SendResult.success("msg-1", AuditMqTopics.AUDIT_LOG, "1-0"));

        boolean result = storage.store(logWithTraceId("trace-abc"));

        assertThat(result).isTrue();

        ArgumentCaptor<MqSendOptions> optionsCaptor = ArgumentCaptor.forClass(MqSendOptions.class);
        verify(mqTemplate).send(eq(AuditMqTopics.AUDIT_LOG), payloadCaptor.capture(),
            optionsCaptor.capture());

        assertThat(payloadCaptor.getValue()).isInstanceOf(ApiAuditLogMainParam.class);
        ApiAuditLogMainParam param = (ApiAuditLogMainParam) payloadCaptor.getValue();
        assertThat(param.getLogType()).isEqualTo("ORG_USER_CREATE");
        assertThat(optionsCaptor.getValue().key()).isEqualTo("trace-abc");
    }

    @Test
    @DisplayName("入参为 null 时应返回 false 且不投递")
    void should_return_false_when_log_is_null() {
        assertThat(storage.store(null)).isFalse();
        verify(mqTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("投递未成功时应返回 false 而不抛异常（审计丢失可容忍，但不能影响业务）")
    void should_return_false_when_send_is_not_successful() {
        when(mqTemplate.send(any(), any(), any()))
            .thenReturn(SendResult.failure(SendStatus.REJECTED, null, AuditMqTopics.AUDIT_LOG));

        assertThatCode(() -> {
            boolean result = storage.store(logWithTraceId("trace-abc"));
            assertThat(result).isFalse();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("send 返回 null 时应返回 false 而不抛 NPE")
    void should_return_false_when_send_result_is_null() {
        when(mqTemplate.send(any(), any(), any())).thenReturn(null);

        assertThat(storage.store(logWithTraceId("trace-abc"))).isFalse();
    }

    @Test
    @DisplayName("MQ 抛异常时应吞掉并返回 false")
    void should_swallow_exception_from_mq() {
        when(mqTemplate.send(any(), any(), any()))
            .thenThrow(new IllegalStateException("redis down"));

        assertThatCode(() -> assertThat(storage.store(logWithTraceId("t"))).isFalse())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("supportBatch 必须为 false —— 消费端按单条负载消费，批量投递无法反序列化")
    void should_not_support_batch() {
        assertThat(storage.supportBatch()).isFalse();
    }

    @Test
    @DisplayName("getPriority 应高于兜底存储器，确保被 LogConfig 选中")
    void should_have_higher_priority_than_fallback_storage() {
        assertThat(storage.getPriority()).isLessThan(new LoggingLogStorage().getPriority());
    }

    @Test
    @DisplayName("storeBatch 即便被调用（违反 supportBatch 声明）也应逐条投递，不静默丢日志")
    void should_store_each_item_when_storeBatch_is_called() {
        when(mqTemplate.send(any(), any(), any()))
            .thenReturn(SendResult.success("msg", AuditMqTopics.AUDIT_LOG, "1-0"));

        boolean result = storage.storeBatch(List.of(logWithTraceId("t1"), logWithTraceId("t2")));

        assertThat(result).isTrue();
        verify(mqTemplate, times(2)).send(any(), any(), any());
    }

    @Test
    @DisplayName("storeBatch 传入空集合时应视为成功且不投递")
    void should_treat_empty_batch_as_success() {
        assertThat(storage.storeBatch(List.of())).isTrue();
        assertThat(storage.storeBatch(null)).isTrue();
        verify(mqTemplate, never()).send(any(), any(), any());
    }

    private OperationLog logWithTraceId(String traceId) {
        LocalDateTime now = LocalDateTime.now();
        OperationLog log = new OperationLog();
        log.setLogType("ORG_USER_CREATE");
        log.setOperation("新增用户");
        log.setLogStatus("SUCCESS");
        log.setPrincipalId("1001");
        log.setPrincipalType("USER");
        log.setServerTime(now);
        log.setEventTime(now);
        log.setEventDate(LocalDate.now());
        log.setTraceId(traceId);
        return log;
    }
}
