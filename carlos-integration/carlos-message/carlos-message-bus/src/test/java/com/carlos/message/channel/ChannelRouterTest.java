package com.carlos.message.channel;

import com.carlos.message.core.channel.ChannelAdapter;
import com.carlos.message.core.channel.ChannelFactory;
import com.carlos.message.core.channel.ChannelType;
import com.carlos.message.core.protocol.MessageContext;
import com.carlos.message.core.protocol.SendResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 渠道路由器测试
 */
class ChannelRouterTest {

    @BeforeEach
    void setUp() {
        ChannelFactory.register(new MockChannelAdapter("SMS_ALIYUN", ChannelType.SMS, 10));
        ChannelFactory.register(new MockChannelAdapter("SMS_TENCENT", ChannelType.SMS, 5));
        ChannelFactory.register(new MockChannelAdapter("EMAIL", ChannelType.EMAIL, 1));
    }

    @AfterEach
    void tearDown() {
        ChannelFactory.remove("SMS_ALIYUN");
        ChannelFactory.remove("SMS_TENCENT");
        ChannelFactory.remove("EMAIL");
    }

    @Test
    void should_route_by_channel_code() {
        ChannelRouter router = new ChannelRouter(null);
        ChannelAdapter adapter = router.route("EMAIL");
        assertNotNull(adapter);
        assertEquals("EMAIL", adapter.getChannelCode());
    }

    @Test
    void should_return_null_when_channel_not_found() {
        ChannelRouter router = new ChannelRouter(null);
        assertNull(router.route("NOT_EXIST"));
    }

    @Test
    void should_route_by_type() {
        ChannelRouter router = new ChannelRouter(null);
        ChannelAdapter adapter = router.routeByType(ChannelType.EMAIL, "TEST");
        assertNotNull(adapter);
        assertEquals(ChannelType.EMAIL, adapter.getChannelType());
    }

    @Test
    void should_support_failover() {
        ChannelRouter router = new ChannelRouter(null);
        MessageContext context = MessageContext.builder()
            .messageType("TEST")
            .messageContent("test")
            .build();
        SendResult result = router.sendWithFallback(context, ChannelType.EMAIL);
        assertNotNull(result);
    }

    static class MockChannelAdapter implements ChannelAdapter {
        private final String code;
        private final ChannelType type;
        private final int weight;

        MockChannelAdapter(String code, ChannelType type, int weight) {
            this.code = code;
            this.type = type;
            this.weight = weight;
        }

        @Override
        public String getChannelCode() {
            return code;
        }

        @Override
        public ChannelType getChannelType() {
            return type;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public SendResult send(MessageContext context) {
            return SendResult.success("mock");
        }

        @Override
        public boolean supports(String messageType) {
            return true;
        }
    }
}
