package com.carlos.message.limiter;

import com.carlos.message.config.MessageProperties;
import com.carlos.message.core.limiter.RateLimiter;
import com.carlos.message.manager.MessageChannelManager;
import com.carlos.message.pojo.dto.MessageChannelDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 基于 Redis ZSet 滑动窗口的分布式限流器
 * </p>
 *
 * <p>
 * 使用 Lua 脚本保证窗口清理、计数判断和成员添加的原子性。
 * 窗口固定为 1 秒，支持按渠道编码独立限流。
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSlidingWindowRateLimiter implements RateLimiter {

    private final StringRedisTemplate stringRedisTemplate;
    private final MessageProperties messageProperties;
    private final MessageChannelManager messageChannelManager;

    private static final String RATE_LIMIT_KEY_PREFIX = "message:ratelimit:";
    private static final long WINDOW_SIZE_MS = 1000;

    private static final String SLIDING_WINDOW_LUA =
        "local key = KEYS[1]; " +
            "local now = tonumber(ARGV[1]); " +
            "local window = tonumber(ARGV[2]); " +
            "local limit = tonumber(ARGV[3]); " +
            "local member = ARGV[4]; " +
            "local minScore = now - window; " +
            "redis.call('zremrangeByScore', key, 0, minScore); " +
            "local current = redis.call('zcard', key); " +
            "if current < limit then " +
            "    redis.call('zadd', key, now, member); " +
            "    redis.call('pexpire', key, window); " +
            "    return 1; " +
            "else " +
            "    return 0; " +
            "end";

    private DefaultRedisScript<Long> redisScript;
    private final Map<String, Integer> qpsCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        redisScript = new DefaultRedisScript<>(SLIDING_WINDOW_LUA, Long.class);
    }

    @Override
    public boolean tryAcquire(String channelCode, int permits) {
        if (!messageProperties.getRateLimiter().isEnabled()) {
            return true;
        }

        int qps = getChannelQps(channelCode);
        if (permits > qps) {
            log.warn("请求许可数超过渠道 QPS 上限: channelCode={}, permits={}, qps={}", channelCode, permits, qps);
            return false;
        }

        String key = RATE_LIMIT_KEY_PREFIX + channelCode;
        long now = System.currentTimeMillis();
        String member = now + ":" + Thread.currentThread().getId();

        for (int i = 0; i < permits; i++) {
            Long result = stringRedisTemplate.execute(redisScript,
                Collections.singletonList(key),
                String.valueOf(now),
                String.valueOf(WINDOW_SIZE_MS),
                String.valueOf(qps),
                member + ":" + i);
            if (result == null || result == 0L) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void updateConfig(String channelCode, int qps, int burst) {
        qpsCache.put(channelCode, qps);
        log.info("更新渠道限流配置: channelCode={}, qps={}, burst={}", channelCode, qps, burst);
    }

    private int getChannelQps(String channelCode) {
        Integer cached = qpsCache.get(channelCode);
        if (cached != null) {
            return cached;
        }

        Map<String, Integer> channels = messageProperties.getRateLimiter().getChannels();
        if (channels != null && channels.containsKey(channelCode)) {
            int qps = channels.get(channelCode);
            qpsCache.put(channelCode, qps);
            return qps;
        }

        MessageChannelDTO dto = messageChannelManager.getByChannelCode(channelCode);
        if (dto != null && dto.getRateLimitQps() != null && dto.getRateLimitQps() > 0) {
            qpsCache.put(channelCode, dto.getRateLimitQps());
            return dto.getRateLimitQps();
        }

        int defaultQps = messageProperties.getRateLimiter().getDefaultQps();
        qpsCache.put(channelCode, defaultQps);
        return defaultQps;
    }
}
