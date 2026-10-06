package com.carlos.message.manager.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.carlos.message.mapper.MessageChannelMapper;
import com.carlos.message.mapper.MessageRecordMapper;
import com.carlos.message.mapper.MessageReceiverMapper;
import com.carlos.message.mapper.MessageSendLogMapper;
import com.carlos.message.mapper.MessageTemplateMapper;
import com.carlos.message.mapper.MessageTypeMapper;
import com.carlos.message.pojo.entity.MessageChannel;
import com.carlos.message.pojo.entity.MessageRecord;
import com.carlos.message.pojo.entity.MessageReceiver;
import com.carlos.message.pojo.entity.MessageSendLog;
import com.carlos.message.pojo.entity.MessageTemplate;
import com.carlos.message.pojo.entity.MessageType;
import com.carlos.message.pojo.param.MessageChannelPageParam;
import com.carlos.message.pojo.param.MessageRecordPageParam;
import com.carlos.message.pojo.param.MessageReceiverPageParam;
import com.carlos.message.pojo.param.MessageSendLogPageParam;
import com.carlos.message.pojo.param.MessageTemplatePageParam;
import com.carlos.message.pojo.param.MessageTypePageParam;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * <p>
 * 列表查询条件构造测试
 * </p>
 *
 * <p>
 * 覆盖各 manager 的 {@code getPage} 是否真正把查询参数落到 Wrapper 上。
 * 修复前 Record / Receiver / SendLog / Template / Type 五个只调用了 {@code .select(...)}，
 * 条件与排序被静默丢弃——列表接口收下参数、返回全量且无序的数据。
 * 修复后每个条件都必须出现在生成的 SQL 片段里，且必须带稳定排序。
 * </p>
 *
 * <p>
 * 测试通过 {@link PageManagerStub} 子类注桩：MyBatis-Plus 的
 * {@code AbstractRepository.getEntityClass()} 依赖 mapper 动态代理来反推实体类型，
 * 直接注入 Mockito mock 会抛 "Unable to retrieve the mapperInterface ... from Mock"，
 * 故在子类里显式给出 mapper 与实体类型。
 * </p>
 *
 * @author Carlos
 */
@DisplayName("消息列表查询条件构造")
class MessageManagerPageQueryTest {

    /**
     * Lambda 条件要解析成真实列名，依赖 MyBatis-Plus 的 TableInfo 缓存；
     * 纯单测没有容器，需手工初始化一次。
     */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, MessageRecord.class);
        TableInfoHelper.initTableInfo(assistant, MessageReceiver.class);
        TableInfoHelper.initTableInfo(assistant, MessageSendLog.class);
        TableInfoHelper.initTableInfo(assistant, MessageTemplate.class);
        TableInfoHelper.initTableInfo(assistant, MessageType.class);
        TableInfoHelper.initTableInfo(assistant, MessageChannel.class);
    }

    // region----------------------  测试桩  ------------------------

    /**
     * 绕开「用 mapper 代理反推实体类」的逻辑，改为显式注入
     */
    private static final class RecordManagerStub extends MessageRecordManagerImpl {
        private final MessageRecordMapper mapper;

        RecordManagerStub(MessageRecordMapper mapper) {
            super(null);
            this.mapper = mapper;
        }

        @Override
        public MessageRecordMapper getBaseMapper() {
            return mapper;
        }

        @Override
        public Class<MessageRecord> getEntityClass() {
            return MessageRecord.class;
        }
    }

    private static final class ReceiverManagerStub extends MessageReceiverManagerImpl {
        private final MessageReceiverMapper mapper;

        ReceiverManagerStub(MessageReceiverMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public MessageReceiverMapper getBaseMapper() {
            return mapper;
        }

        @Override
        public Class<MessageReceiver> getEntityClass() {
            return MessageReceiver.class;
        }
    }

    private static final class SendLogManagerStub extends MessageSendLogManagerImpl {
        private final MessageSendLogMapper mapper;

        SendLogManagerStub(MessageSendLogMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public MessageSendLogMapper getBaseMapper() {
            return mapper;
        }

        @Override
        public Class<MessageSendLog> getEntityClass() {
            return MessageSendLog.class;
        }
    }

    private static final class TemplateManagerStub extends MessageTemplateManagerImpl {
        private final MessageTemplateMapper mapper;

        TemplateManagerStub(MessageTemplateMapper mapper) {
            super(null);
            this.mapper = mapper;
        }

        @Override
        public MessageTemplateMapper getBaseMapper() {
            return mapper;
        }

        @Override
        public Class<MessageTemplate> getEntityClass() {
            return MessageTemplate.class;
        }
    }

    private static final class TypeManagerStub extends MessageTypeManagerImpl {
        private final MessageTypeMapper mapper;

        TypeManagerStub(MessageTypeMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public MessageTypeMapper getBaseMapper() {
            return mapper;
        }

        @Override
        public Class<MessageType> getEntityClass() {
            return MessageType.class;
        }
    }

    private static final class ChannelManagerStub extends MessageChannelManagerImpl {
        private final MessageChannelMapper mapper;

        ChannelManagerStub(MessageChannelMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public MessageChannelMapper getBaseMapper() {
            return mapper;
        }

        @Override
        public Class<MessageChannel> getEntityClass() {
            return MessageChannel.class;
        }
    }

    // endregion----------------------  测试桩  ------------------------

    // region----------------------  测试辅助  ------------------------

    /**
     * 执行分页调用并捕获传给 mapper 的 Wrapper
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Wrapper<?> captureWrapper(BaseMapper mapper, Runnable invocation) {
        doAnswer(invocationOnMock -> invocationOnMock.getArgument(0))
            .when(mapper).selectPage(any(), any());
        invocation.run();
        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectPage(any(), captor.capture());
        return captor.getValue();
    }

    private static String sqlOf(Wrapper<?> wrapper) {
        String sql = wrapper.getSqlSegment();
        assertNotNull(sql, "Wrapper 未生成 SQL 片段");
        return sql;
    }

    private static void assertOrderedByCreateTime(String sql) {
        String lower = sql.toLowerCase();
        assertTrue(lower.contains("order by") && lower.contains("create_time"),
            "分页缺少 create_time 稳定排序（PostgreSQL 下翻页会重复/漏行）: " + sql);
    }

    private static void assertHasCondition(String sql, String column) {
        assertTrue(sql.toLowerCase().contains(column),
            "条件未落到 SQL，缺少列 " + column + " : " + sql);
    }

    private static void assertNoCondition(String sql, String column) {
        assertFalse(sql.toLowerCase().contains(column),
            "本不应参与过滤的列却出现在 SQL 中: " + column + " : " + sql);
    }

    private static void assertParamValue(Wrapper<?> wrapper, Object value) {
        assertTrue(((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs().containsValue(value),
            "参数值未绑定到 Wrapper: " + value);
    }

    // endregion----------------------  测试辅助  ------------------------

    @Nested
    @DisplayName("消息记录 MessageRecordManagerImpl")
    class RecordPageTest {

        @Test
        @DisplayName("全部查询条件都应生效，并带 create_time 倒序")
        void should_apply_all_record_filters() {
            MessageRecordMapper mapper = mock(MessageRecordMapper.class);
            RecordManagerStub manager = new RecordManagerStub(mapper);

            MessageRecordPageParam param = new MessageRecordPageParam()
                .setMessageId("M1")
                .setTemplateCode("TPL_A")
                .setMessageType("NOTICE")
                .setMessageTitle("标题")
                .setSenderId("S1")
                .setSenderName("张三")
                .setSenderSystem("CARLOS-ORG")
                .setPriority(1)
                .setStart(LocalDateTime.of(2026, 1, 1, 0, 0))
                .setEnd(LocalDateTime.of(2026, 1, 31, 23, 59));

            Wrapper<?> wrapper = captureWrapper(mapper, () -> manager.getPage(param));
            String sql = sqlOf(wrapper);

            assertAll(
                () -> assertHasCondition(sql, "message_id"),
                () -> assertHasCondition(sql, "template_code"),
                () -> assertHasCondition(sql, "message_type"),
                () -> assertHasCondition(sql, "message_title"),
                () -> assertHasCondition(sql, "sender_id"),
                () -> assertHasCondition(sql, "sender_name"),
                () -> assertHasCondition(sql, "sender_system"),
                () -> assertHasCondition(sql, "priority"),
                () -> assertHasCondition(sql, "create_time"),
                () -> assertOrderedByCreateTime(sql),
                () -> assertParamValue(wrapper, "M1"),
                () -> assertParamValue(wrapper, "TPL_A")
            );
        }

        @Test
        @DisplayName("未传条件时不应拼出任何过滤，但仍必须带稳定排序")
        void should_only_order_when_no_record_filter() {
            MessageRecordMapper mapper = mock(MessageRecordMapper.class);
            RecordManagerStub manager = new RecordManagerStub(mapper);

            Wrapper<?> wrapper = captureWrapper(mapper, () -> manager.getPage(new MessageRecordPageParam()));
            String sql = sqlOf(wrapper);

            assertAll(
                () -> assertNoCondition(sql, "message_id"),
                () -> assertNoCondition(sql, "template_code"),
                () -> assertNoCondition(sql, "priority"),
                () -> assertOrderedByCreateTime(sql)
            );
        }

        @Test
        @DisplayName("空白字符串视为未传，不生成过滤条件")
        void should_ignore_blank_record_filters() {
            MessageRecordMapper mapper = mock(MessageRecordMapper.class);
            RecordManagerStub manager = new RecordManagerStub(mapper);

            MessageRecordPageParam param = new MessageRecordPageParam()
                .setMessageId("")
                .setTemplateCode("   ");

            Wrapper<?> wrapper = captureWrapper(mapper, () -> manager.getPage(param));
            String sql = sqlOf(wrapper);

            assertAll(
                () -> assertNoCondition(sql, "message_id"),
                () -> assertNoCondition(sql, "template_code"),
                () -> assertOrderedByCreateTime(sql)
            );
        }

        @Test
        @DisplayName("JSON 载荷与正文不参与过滤")
        void should_not_filter_on_payload_columns() {
            MessageRecordMapper mapper = mock(MessageRecordMapper.class);
            RecordManagerStub manager = new RecordManagerStub(mapper);

            MessageRecordPageParam param = new MessageRecordPageParam()
                .setExtras("{\"a\":1}")
                .setTemplateParams("{\"b\":2}")
                .setMessageContent("正文关键字");

            Wrapper<?> wrapper = captureWrapper(mapper, () -> manager.getPage(param));
            String sql = sqlOf(wrapper);

            assertAll(
                () -> assertNoCondition(sql, "extras"),
                () -> assertNoCondition(sql, "template_params"),
                () -> assertNoCondition(sql, "message_content")
            );
        }
    }

    @Nested
    @DisplayName("消息接收人 MessageReceiverManagerImpl")
    class ReceiverPageTest {

        @Test
        @DisplayName("status=0（待发送）是有效取值，必须生效")
        void should_apply_status_zero() {
            MessageReceiverMapper mapper = mock(MessageReceiverMapper.class);
            ReceiverManagerStub manager = new ReceiverManagerStub(mapper);

            MessageReceiverPageParam param = new MessageReceiverPageParam()
                .setMessageId("M1")
                .setChannelCode("SMS")
                .setReceiverId("U1")
                .setStatus(0);

            Wrapper<?> wrapper = captureWrapper(mapper, () -> manager.getPage(param));
            String sql = sqlOf(wrapper);

            assertAll(
                () -> assertHasCondition(sql, "message_id"),
                () -> assertHasCondition(sql, "channel_code"),
                () -> assertHasCondition(sql, "receiver_id"),
                () -> assertHasCondition(sql, "status"),
                () -> assertOrderedByCreateTime(sql),
                () -> assertParamValue(wrapper, 0)
            );
        }

        @Test
        @DisplayName("receiverType 沿用 channelType 的约定：0 表示不筛选")
        void should_ignore_receiver_type_zero() {
            MessageReceiverMapper mapper = mock(MessageReceiverMapper.class);
            ReceiverManagerStub manager = new ReceiverManagerStub(mapper);

            Wrapper<?> wrapper = captureWrapper(mapper,
                () -> manager.getPage(new MessageReceiverPageParam().setReceiverType(0)));
            String sql = sqlOf(wrapper);

            assertAll(
                () -> assertNoCondition(sql, "receiver_type"),
                () -> assertOrderedByCreateTime(sql)
            );
        }

        @Test
        @DisplayName("receiverType 大于 0 时生效")
        void should_apply_receiver_type() {
            MessageReceiverMapper mapper = mock(MessageReceiverMapper.class);
            ReceiverManagerStub manager = new ReceiverManagerStub(mapper);

            Wrapper<?> wrapper = captureWrapper(mapper,
                () -> manager.getPage(new MessageReceiverPageParam().setReceiverType(2)));
            String sql = sqlOf(wrapper);

            assertTrue(sql.toLowerCase().contains("receiver_type"), sql);
        }
    }

    @Nested
    @DisplayName("消息发送日志 MessageSendLogManagerImpl")
    class SendLogPageTest {

        @Test
        @DisplayName("success=false 是有效取值，必须生效")
        void should_apply_success_false() {
            MessageSendLogMapper mapper = mock(MessageSendLogMapper.class);
            SendLogManagerStub manager = new SendLogManagerStub(mapper);

            MessageSendLogPageParam param = new MessageSendLogPageParam()
                .setMessageId("M1")
                .setReceiverId(100L)
                .setChannelCode("EMAIL")
                .setSuccess(false)
                .setErrorCode("E500");

            Wrapper<?> wrapper = captureWrapper(mapper, () -> manager.getPage(param));
            String sql = sqlOf(wrapper);

            assertAll(
                () -> assertHasCondition(sql, "message_id"),
                () -> assertHasCondition(sql, "receiver_id"),
                () -> assertHasCondition(sql, "channel_code"),
                () -> assertHasCondition(sql, "is_success"),
                () -> assertHasCondition(sql, "error_code"),
                () -> assertOrderedByCreateTime(sql),
                () -> assertParamValue(wrapper, false)
            );
        }

        @Test
        @DisplayName("请求/响应原始报文不参与过滤")
        void should_not_filter_on_raw_payload() {
            MessageSendLogMapper mapper = mock(MessageSendLogMapper.class);
            SendLogManagerStub manager = new SendLogManagerStub(mapper);

            MessageSendLogPageParam param = new MessageSendLogPageParam()
                .setRequestParam("{\"to\":\"a@b.c\"}")
                .setResponseData("{\"ok\":true}");

            Wrapper<?> wrapper = captureWrapper(mapper, () -> manager.getPage(param));
            String sql = sqlOf(wrapper);

            assertAll(
                () -> assertNoCondition(sql, "request_param"),
                () -> assertNoCondition(sql, "response_data"),
                () -> assertOrderedByCreateTime(sql)
            );
        }
    }

    @Nested
    @DisplayName("消息模板 MessageTemplateManagerImpl")
    class TemplatePageTest {

        @Test
        @DisplayName("typeId / 编码 / 名称 / enabled 都应生效")
        void should_apply_template_filters() {
            MessageTemplateMapper mapper = mock(MessageTemplateMapper.class);
            TemplateManagerStub manager = new TemplateManagerStub(mapper);

            MessageTemplatePageParam param = new MessageTemplatePageParam()
                .setTypeId(9L)
                .setTemplateCode("TPL")
                .setTemplateName("名称")
                .setEnabled(false);

            Wrapper<?> wrapper = captureWrapper(mapper, () -> manager.getPage(param));
            String sql = sqlOf(wrapper);

            assertAll(
                () -> assertHasCondition(sql, "type_id"),
                () -> assertHasCondition(sql, "template_code"),
                () -> assertHasCondition(sql, "template_name"),
                () -> assertHasCondition(sql, "is_enabled"),
                () -> assertOrderedByCreateTime(sql),
                () -> assertParamValue(wrapper, false)
            );
        }

        @Test
        @DisplayName("正文与 JSON 定义不参与过滤")
        void should_not_filter_on_template_payload() {
            MessageTemplateMapper mapper = mock(MessageTemplateMapper.class);
            TemplateManagerStub manager = new TemplateManagerStub(mapper);

            MessageTemplatePageParam param = new MessageTemplatePageParam()
                .setContentTemplate("正文")
                .setParamSchema("{}")
                .setChannelConfig("{}");

            Wrapper<?> wrapper = captureWrapper(mapper, () -> manager.getPage(param));
            String sql = sqlOf(wrapper);

            assertAll(
                () -> assertNoCondition(sql, "content_template"),
                () -> assertNoCondition(sql, "param_schema"),
                () -> assertNoCondition(sql, "channel_config")
            );
        }
    }

    @Nested
    @DisplayName("消息类型 MessageTypeManagerImpl")
    class TypePageTest {

        @Test
        @DisplayName("编码 / 名称 / enabled 都应生效")
        void should_apply_type_filters() {
            MessageTypeMapper mapper = mock(MessageTypeMapper.class);
            TypeManagerStub manager = new TypeManagerStub(mapper);

            MessageTypePageParam param = new MessageTypePageParam()
                .setTypeCode("NOTICE")
                .setTypeName("通知")
                .setEnabled(false);

            Wrapper<?> wrapper = captureWrapper(mapper, () -> manager.getPage(param));
            String sql = sqlOf(wrapper);

            assertAll(
                () -> assertHasCondition(sql, "type_code"),
                () -> assertHasCondition(sql, "type_name"),
                () -> assertHasCondition(sql, "is_enabled"),
                () -> assertOrderedByCreateTime(sql),
                () -> assertParamValue(wrapper, false)
            );
        }
    }

    @Nested
    @DisplayName("消息渠道 MessageChannelManagerImpl（回归保护）")
    class ChannelPageTest {

        @Test
        @DisplayName("原有的过滤与排序行为保持不变")
        void should_keep_channel_filters_working() {
            MessageChannelMapper mapper = mock(MessageChannelMapper.class);
            ChannelManagerStub manager = new ChannelManagerStub(mapper);

            MessageChannelPageParam param = new MessageChannelPageParam()
                .setChannelCode("SMS")
                .setChannelName("短信")
                .setChannelType(1)
                .setEnabled(1);

            Wrapper<?> wrapper = captureWrapper(mapper, () -> manager.getPage(param));
            String sql = sqlOf(wrapper);

            assertAll(
                () -> assertHasCondition(sql, "channel_code"),
                () -> assertHasCondition(sql, "channel_name"),
                () -> assertHasCondition(sql, "channel_type"),
                () -> assertHasCondition(sql, "is_enabled"),
                () -> assertOrderedByCreateTime(sql)
            );
        }

        @Test
        @DisplayName("channelType=0 表示不筛选")
        void should_ignore_channel_type_zero() {
            MessageChannelMapper mapper = mock(MessageChannelMapper.class);
            ChannelManagerStub manager = new ChannelManagerStub(mapper);

            Wrapper<?> wrapper = captureWrapper(mapper,
                () -> manager.getPage(new MessageChannelPageParam().setChannelType(0)));
            String sql = sqlOf(wrapper);

            assertAll(
                () -> assertNoCondition(sql, "channel_type"),
                () -> assertOrderedByCreateTime(sql)
            );
        }
    }
}
