package com.carlos.redis.stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Stream 序列化边界测试
 * <p>
 * 约定：key 与字段名是字符串，字段值是<b>显式 JSON 字符串</b>，原语层<b>不做任何再包装</b>。
 * 若有人把 {@code ConfigurableRedisSerializer}（默认 Jackson）挂到 Stream 上，
 * 标量会被写成带引号的字符串，字段读出会变成转义字符串——本测试把「原样透传」变成可断言的约束。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("RedisStreamTemplate 序列化边界")
class RedisStreamSerdeTest {

    private StringRedisTemplate redisTemplate;
    private StreamOperations<String, String, String> streamOps;
    private DefaultRedisStreamTemplate template;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        streamOps = mock(StreamOperations.class);
        template = new DefaultRedisStreamTemplate(redisTemplate, new RedisStreamProperties());
        doReturn(streamOps).when(redisTemplate).opsForStream();
    }

    @Test
    @DisplayName("写入时字段应原样透传，不被再序列化或加引号")
    void should_pass_fields_through_on_add() {
        when(streamOps.add(any(String.class), anyMap(), any())).thenReturn(
            org.springframework.data.redis.connection.stream.RecordId.of("1-1"));

        String payload = "{\"orderId\":\"O1\",\"amount\":10}";
        template.add("k", Map.of("data", payload), null);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(streamOps).add(eq("k"), captor.capture(), any());
        assertEquals(payload, captor.getValue().get("data"),
            "字段值必须原样透传，不得被 Jackson 再包装成带引号的字符串");
    }

    @Test
    @DisplayName("读取时字段应按原样映射为不可变 Map")
    void should_expose_fields_verbatim_on_read() {
        String payload = "{\"a\":1}";
        MapRecord<String, String, String> record = MapRecord.create("k", Map.of("data", payload));
        when(streamOps.range(eq("k"), any(Range.class), any(Limit.class))).thenReturn(List.of(record));

        List<StreamEntry> entries = template.range("k", 10);

        assertEquals(1, entries.size());
        assertEquals(payload, entries.get(0).field("data"));
        assertEquals(record.getId().getValue(), entries.get(0).recordId());
    }
}
