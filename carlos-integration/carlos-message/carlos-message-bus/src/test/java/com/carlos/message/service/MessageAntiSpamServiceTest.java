package com.carlos.message.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消息防刷服务测试
 */
@ExtendWith(MockitoExtension.class)
class MessageAntiSpamServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private MessageAntiSpamService antiSpamService;

    @Test
    void should_allow_when_under_rate_limit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean result = antiSpamService.checkRateLimit("user1", "SMS");
        assertTrue(result);
    }

    @Test
    void should_block_when_over_rate_limit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(10L);

        boolean result = antiSpamService.checkRateLimit("user1", "SMS");
        assertFalse(result);
    }

    @Test
    void should_allow_unique_fingerprint() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        boolean result = antiSpamService.checkFingerprint("abc123");
        assertTrue(result);
        verify(valueOperations).set(anyString(), eq("1"), any());
    }

    @Test
    void should_block_duplicate_fingerprint() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

        boolean result = antiSpamService.checkFingerprint("abc123");
        assertFalse(result);
    }

    @Test
    void should_generate_consistent_fingerprint() {
        String fp1 = antiSpamService.generateFingerprint("hello", "user1", "SMS");
        String fp2 = antiSpamService.generateFingerprint("hello", "user1", "SMS");
        assertEquals(fp1, fp2);
        assertNotEquals(fp1, antiSpamService.generateFingerprint("hello2", "user1", "SMS"));
    }
}
