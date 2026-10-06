package com.carlos.mq.spi;

import com.carlos.mq.core.MqMessageListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 消费订阅描述校验测试
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("MqSubscription 校验")
class MqSubscriptionTest {

    private static MqMessageListener noopHandler() {
        return message -> null;
    }

    @Test
    @DisplayName("topic/group/handler 缺失时应快速失败")
    void should_reject_missing_required_fields() {
        assertThrows(IllegalArgumentException.class,
            () -> new MqSubscription(null, "g", null, 1, 1, 3, noopHandler()));
        assertThrows(IllegalArgumentException.class,
            () -> new MqSubscription("  ", "g", null, 1, 1, 3, noopHandler()));
        assertThrows(IllegalArgumentException.class,
            () -> new MqSubscription("t", "", null, 1, 1, 3, noopHandler()));
        assertThrows(IllegalArgumentException.class,
            () -> new MqSubscription("t", "g", null, 1, 1, 3, null));
    }

    @Test
    @DisplayName("非法的并发/批量/投递上限应回落为 1，而不是放行非法值")
    void should_normalize_non_positive_numbers() {
        MqSubscription subscription = new MqSubscription("t", "g", null, 0, 0, -5, noopHandler());

        assertEquals(1, subscription.concurrency());
        assertEquals(1, subscription.batchSize());
        assertEquals(1, subscription.maxDeliveryCount());
    }

    @Test
    @DisplayName("consumerId 可缺省（由传输层按实例派生唯一名）")
    void should_allow_absent_consumer_id() {
        MqSubscription subscription = new MqSubscription("t", "g", null, 2, 10, 3, noopHandler());

        assertNull(subscription.consumerId());
        assertEquals(2, subscription.concurrency());
        assertEquals("t@g", subscription.display());
    }

    @Test
    @DisplayName("并发度应被保留（容器据此注册多个消费者名以分摊消息）")
    void should_keep_concurrency() {
        MqSubscription subscription = new MqSubscription("t", "g", null, 4, 1, 3, noopHandler());

        assertEquals(4, subscription.concurrency());
    }
}
