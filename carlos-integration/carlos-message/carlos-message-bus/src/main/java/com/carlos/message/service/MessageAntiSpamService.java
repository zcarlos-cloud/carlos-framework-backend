package com.carlos.message.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * <p>
 * 消息防刷与防重放服务
 * </p>
 *
 * <p>
 * 提供以下防护能力：
 * 1. 接收人级别发送频率限制（滑动窗口）
 * 2. 消息内容指纹去重（MD5 指纹，防止重复发送相同内容）
 * 3. 全局发送速率保护
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageAntiSpamService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String RATE_LIMIT_KEY_PREFIX = "message:rate:";
    private static final String FINGERPRINT_KEY_PREFIX = "message:fingerprint:";

    /**
     * 接收人频率限制：默认同一接收人同一渠道 60 秒内最多 5 条
     */
    private static final int RECEIVER_RATE_LIMIT = 5;
    private static final Duration RECEIVER_RATE_WINDOW = Duration.ofSeconds(60);

    /**
     * 消息指纹去重窗口：默认 5 分钟内相同内容不重复发送
     */
    private static final Duration FINGERPRINT_WINDOW = Duration.ofMinutes(5);

    /**
     * 检查是否触发频率限制
     *
     * @param receiverId  接收人ID
     * @param channelCode 渠道编码
     * @return true 表示允许发送，false 表示被限制
     */
    public boolean checkRateLimit(String receiverId, String channelCode) {
        if (StrUtil.isBlank(receiverId)) {
            return true;
        }
        String key = RATE_LIMIT_KEY_PREFIX + receiverId + ":" + (channelCode != null ? channelCode : "all");
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, RECEIVER_RATE_WINDOW);
        }
        if (count != null && count > RECEIVER_RATE_LIMIT) {
            log.warn("消息发送频率超限: receiverId={}, channelCode={}, count={}", receiverId, channelCode, count);
            return false;
        }
        return true;
    }

    /**
     * 检查消息指纹是否重复
     *
     * @param fingerprint 消息指纹（通常为 MD5(content + receiverId + channelCode)）
     * @return true 表示允许发送（不重复），false 表示重复
     */
    public boolean checkFingerprint(String fingerprint) {
        if (StrUtil.isBlank(fingerprint)) {
            return true;
        }
        String key = FINGERPRINT_KEY_PREFIX + fingerprint;
        Boolean exists = stringRedisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            log.warn("消息内容重复发送: fingerprint={}", fingerprint);
            return false;
        }
        stringRedisTemplate.opsForValue().set(key, "1", FINGERPRINT_WINDOW);
        return true;
    }

    /**
     * 生成消息指纹
     *
     * @param content     消息内容
     * @param receiverId  接收人ID
     * @param channelCode 渠道编码
     * @return MD5 指纹
     */
    public String generateFingerprint(String content, String receiverId, String channelCode) {
        String source = (content != null ? content : "") + "|" + (receiverId != null ? receiverId : "") + "|" + (channelCode != null ? channelCode : "");
        return DigestUtil.md5Hex(source);
    }

    /**
     * 综合检查（频率 + 指纹）
     *
     * @param receiverId  接收人ID
     * @param channelCode 渠道编码
     * @param content     消息内容
     * @return true 表示允许发送
     */
    public boolean check(String receiverId, String channelCode, String content) {
        if (!checkRateLimit(receiverId, channelCode)) {
            return false;
        }
        String fingerprint = generateFingerprint(content, receiverId, channelCode);
        return checkFingerprint(fingerprint);
    }
}
