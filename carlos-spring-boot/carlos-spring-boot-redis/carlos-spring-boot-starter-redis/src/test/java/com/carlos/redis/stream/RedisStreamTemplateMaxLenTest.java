package com.carlos.redis.stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Stream 长度上限（M1）护栏测试
 * <p>
 * <b>这是本项目「与缓存/token 共用 Redis 实例」约束下的核心保护项</b>：
 * {@code XADD MAXLEN ~ N} 把无界积压变成固定预算。本测试确保「默认就带长度上限」
 * 这一行为不会被后续改动悄悄去掉——任何人删掉 MAXLEN 都会让本测试变红。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("RedisStreamTemplate 长度上限")
class RedisStreamTemplateMaxLenTest {

    private StringRedisTemplate redisTemplate;
    private StreamOperations<String, String, String> streamOps;
    private RedisStreamProperties properties;
    private DefaultRedisStreamTemplate template;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        streamOps = mock(StreamOperations.class);
        properties = new RedisStreamProperties();
        template = new DefaultRedisStreamTemplate(redisTemplate, properties);
        // 泛型方法用 doReturn 规避类型推断问题
        doReturn(streamOps).when(redisTemplate).opsForStream();
    }

    private RedisStreamCommands.XAddOptions captureAddOptions() {
        ArgumentCaptor<RedisStreamCommands.XAddOptions> captor =
            ArgumentCaptor.forClass(RedisStreamCommands.XAddOptions.class);
        verify(streamOps).add(eq("stream:key"), anyMap(), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("options 为 null 时应按全局默认带 MAXLEN 与近似修剪")
    void should_apply_global_maxlen_when_options_absent() {
        when(streamOps.add(anyString(), anyMap(), any())).thenReturn(RecordId.of("1-1"));

        template.add("stream:key", Map.of("data", "{}"), null);

        RedisStreamCommands.XAddOptions options = captureAddOptions();
        assertTrue(options.hasMaxlen(), "默认必须带 MAXLEN —— 这是共用实例下的核心保护项");
        assertEquals(20000L, options.getMaxlen());
        assertTrue(options.isApproximateTrimming(), "默认应为近似修剪，避免 O(N) 精确裁剪阻塞全实例");
    }

    @Test
    @DisplayName("options 指定上限时应覆盖全局默认，并尊重精确修剪选择")
    void should_honor_explicit_options() {
        when(streamOps.add(anyString(), anyMap(), any())).thenReturn(RecordId.of("1-2"));

        template.add("stream:key", Map.of("data", "{}"), new StreamAddOptions(500L, false));

        RedisStreamCommands.XAddOptions options = captureAddOptions();
        assertEquals(500L, options.getMaxlen());
        assertFalse(options.isApproximateTrimming(), "显式要求精确修剪时应予尊重");
    }

    @Test
    @DisplayName("选项层不得绕过长度上限：maxLength 为 null 时仍回落全局默认")
    void should_not_allow_bypassing_maxlen_via_options() {
        when(streamOps.add(anyString(), anyMap(), any())).thenReturn(RecordId.of("1-3"));

        // 显式传 null 只表示「用全局默认」，不表示「不限」——
        // 关闭保护只能通过全局配置（且启动期会告警），避免调用方静默失去这道保护
        template.add("stream:key", Map.of("data", "{}"), new StreamAddOptions(null, true));

        RedisStreamCommands.XAddOptions options = captureAddOptions();
        assertTrue(options.hasMaxlen(), "选项层必须无法绕过长度上限");
        assertEquals(20000L, options.getMaxlen());
    }

    @Test
    @DisplayName("全局配置为 null 时不带 MAXLEN（不阻断写入，由启动期告警提示）")
    void should_skip_maxlen_when_global_disabled() {
        properties.setMaxLength(null);
        when(streamOps.add(anyString(), anyMap(), any())).thenReturn(RecordId.of("1-4"));

        template.add("stream:key", Map.of("data", "{}"), null);

        assertFalse(captureAddOptions().hasMaxlen());
    }
}
