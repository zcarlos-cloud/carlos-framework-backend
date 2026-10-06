package com.carlos.auth.audit;

import com.carlos.audit.api.ApiAuditLogMain;
import com.carlos.audit.api.constant.AuditMqTopics;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.mq.core.MqSendOptions;
import com.carlos.mq.core.MqTemplate;
import com.carlos.mq.core.SendResult;
import com.carlos.mq.core.SendStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审计投递点测试
 *
 * <h2>守护的不变量：<b>审计的任何故障都不得影响登录</b></h2>
 * <p>
 * 本类覆盖的全部 8 处调用点都位于<b>登录/登出热路径</b>上。审计丢失可以接受，
 * 但因审计导致登录失败绝不可接受——所以 {@code publish()} 必须<b>永不抛出</b>，
 * 且调用方无需 try/catch。以下用例把这条不变量变成测试期可捕获的约束。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("AuditLogPublisher 投递不变量")
class AuditLogPublisherTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<MqTemplate> mqTemplateProvider = mock(ObjectProvider.class);

    private final MqTemplate mqTemplate = mock(MqTemplate.class);
    private final ApiAuditLogMain apiAuditLogMain = mock(ApiAuditLogMain.class);

    private AuditProperties properties;
    private AuditLogPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new AuditProperties();
        when(mqTemplateProvider.getIfAvailable()).thenReturn(mqTemplate);
        publisher = new AuditLogPublisher(properties, mqTemplateProvider, apiAuditLogMain);
    }

    private ApiAuditLogMainParam param() {
        ApiAuditLogMainParam param = new ApiAuditLogMainParam();
        param.setLogType("USER_LOGIN");
        param.setPrincipalName("admin");
        return param;
    }

    @Test
    @DisplayName("应投递到两端共用的主题常量（避免生产/消费主题漂移）")
    void should_publish_to_shared_topic() {
        when(mqTemplate.send(eq(AuditMqTopics.AUDIT_LOG), any(), any()))
            .thenReturn(SendResult.success("mid-1", AuditMqTopics.AUDIT_LOG, "1-1"));

        publisher.publish(param());

        ArgumentCaptor<MqSendOptions> options = ArgumentCaptor.forClass(MqSendOptions.class);
        verify(mqTemplate).send(eq(AuditMqTopics.AUDIT_LOG), any(), options.capture());
        // 业务键取 traceId（可为 null）；此处未设置，故为 null
        assertEquals(null, options.getValue().key());
    }

    @Test
    @DisplayName("MQ 抛异常时不得向上传播（登录不能被审计拖垮）")
    void should_never_throw_when_mq_fails() {
        doThrow(new IllegalStateException("Redis 不可达"))
            .when(mqTemplate).send(any(), any(), any());

        assertDoesNotThrow(() -> publisher.publish(param()));
    }

    @Test
    @DisplayName("MQ 返回失败结果时不得向上传播")
    void should_never_throw_when_mq_returns_failure() {
        when(mqTemplate.send(any(), any(), any()))
            .thenReturn(SendResult.failure(SendStatus.TRANSPORT_UNAVAILABLE, null,
                AuditMqTopics.AUDIT_LOG));

        assertDoesNotThrow(() -> publisher.publish(param()));
    }

    @Test
    @DisplayName("MQ 模板缺席时（未引入或 type=none）只落日志，不抛异常")
    void should_degrade_quietly_when_template_absent() {
        when(mqTemplateProvider.getIfAvailable()).thenReturn(null);

        assertDoesNotThrow(() -> publisher.publish(param()));
        verify(mqTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("参数为 null 时应直接忽略")
    void should_ignore_null_param() {
        assertDoesNotThrow(() -> publisher.publish(null));
        verify(mqTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("FEIGN 模式应走回退路径（用于出问题时不改代码即回退）")
    void should_fall_back_to_feign() {
        properties.setMode(AuditMode.FEIGN);

        publisher.publish(param());

        verify(apiAuditLogMain).saveAuditLog(any());
        verify(mqTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("FEIGN 模式抛异常时同样不得向上传播")
    void should_never_throw_when_feign_fails() {
        properties.setMode(AuditMode.FEIGN);
        doThrow(new IllegalStateException("审计服务不可达"))
            .when(apiAuditLogMain).saveAuditLog(any());

        assertDoesNotThrow(() -> publisher.publish(param()));
    }

    @Test
    @DisplayName("默认模式应为 MQ（打开即用新链路）")
    void should_default_to_mq_mode() {
        assertEquals(AuditMode.MQ, new AuditProperties().getMode());
    }
}
