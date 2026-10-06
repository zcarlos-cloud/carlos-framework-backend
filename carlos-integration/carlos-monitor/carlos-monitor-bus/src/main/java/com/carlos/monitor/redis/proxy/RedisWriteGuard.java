package com.carlos.monitor.redis.proxy;

import cn.hutool.core.util.StrUtil;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.util.MonitorCacheUtil;
import com.carlos.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * <p>
 * Redis 写操作前置守卫
 * 写前置链：实例 manage_enabled 校验 → 高危操作 confirm-token 校验 → （调用方做前值快照）
 * </p>
 * <p>
 * confirm-token：一次性令牌（Redis 存储，键 monitor:confirm:{token}，绑定 instanceId+操作摘要，
 * 60s 有效，消费即删），前端二次确认弹窗由后端签发
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisWriteGuard {

    /**
     * 确认令牌键前缀
     */
    private static final String CONFIRM_TOKEN_PREFIX = "monitor:confirm:";

    private final MonitorProperties monitorProperties;

    /**
     * 校验实例数据管理入口开关（写操作第一步）
     *
     * @param instance 实例 DTO
     */
    public void checkManageEnabled(MonitorInstanceDTO instance) {
        if (instance == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception();
        }
        if (Boolean.FALSE.equals(instance.getManageEnabled())) {
            throw MonitorErrorCode.MONITOR_MANAGE_DISABLED.exception();
        }
    }

    /**
     * 签发高危操作确认令牌（一次性，绑定实例与操作摘要）
     *
     * @param instanceId 实例ID
     * @param operation  操作摘要（如 batch-delete:pattern=dept:*）
     * @return 令牌
     */
    public String issueConfirmToken(Long instanceId, String operation) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String value = instanceId + ":" + operation;
        RedisUtil.setValue(CONFIRM_TOKEN_PREFIX + token, value,
            monitorProperties.getRedis().getConfirmTokenTtlSec());
        log.info("签发高危操作确认令牌, instanceId:{}, operation:{}", instanceId, operation);
        return token;
    }

    /**
     * 消费确认令牌（一次性，消费即删；绑定实例与操作摘要防篡改）
     *
     * @param instanceId  实例ID
     * @param operation   操作摘要
     * @param confirmToken 令牌（X-Confirm-Token 头传入）
     */
    public void consumeConfirmToken(Long instanceId, String operation, String confirmToken) {
        if (StrUtil.isBlank(confirmToken)) {
            throw MonitorErrorCode.MONITOR_CONFIRM_TOKEN_INVALID.exception();
        }
        String key = CONFIRM_TOKEN_PREFIX + confirmToken;
        String value = MonitorCacheUtil.getString(key);
        if (StrUtil.isBlank(value)) {
            throw MonitorErrorCode.MONITOR_CONFIRM_TOKEN_INVALID.exception("确认令牌已过期或不存在，请重新签发");
        }
        // 消费即删（一次性）
        RedisUtil.delete(key);
        String expected = instanceId + ":" + operation;
        if (!expected.equals(value)) {
            throw MonitorErrorCode.MONITOR_CONFIRM_TOKEN_INVALID.exception("确认令牌与操作不匹配，请重新签发");
        }
    }
}
