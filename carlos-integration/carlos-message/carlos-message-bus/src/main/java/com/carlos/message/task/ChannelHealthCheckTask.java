package com.carlos.message.task;

import com.carlos.message.core.channel.ChannelAdapter;
import com.carlos.message.core.channel.ChannelFactory;
import com.carlos.message.manager.MessageChannelManager;
import com.carlos.message.metrics.MessageMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 渠道健康检查定时任务
 * </p>
 *
 * <p>
 * 定期扫描所有已注册的渠道适配器，执行健康检查并根据结果更新渠道状态。
 * 状态变化时同步更新数据库，避免故障渠道持续接收流量。
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelHealthCheckTask {

    private final MessageChannelManager messageChannelManager;
    private final MessageMetrics messageMetrics;
    private final SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 渠道健康状态内存缓存 (channelCode -> isHealthy)
     */
    private static final java.util.Map<String, Boolean> HEALTH_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 连续失败次数计数器 (channelCode -> failCount)
     */
    private static final java.util.Map<String, Integer> FAIL_COUNT_MAP = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 触发故障状态所需连续失败次数
     */
    private static final int FAILURE_THRESHOLD = 3;

    /**
     * 定时执行健康检查 (每30秒)
     */
    @Scheduled(fixedRate = 30000)
    public void checkAllChannels() {
        var channels = ChannelFactory.getAllChannels();
        if (channels.isEmpty()) {
            return;
        }

        log.debug("开始渠道健康检查, 渠道数量: {}", channels.size());
        for (ChannelAdapter adapter : channels) {
            checkChannel(adapter);
        }
    }

    private void checkChannel(ChannelAdapter adapter) {
        String channelCode = adapter.getChannelCode();
        boolean previousHealthy = HEALTH_CACHE.getOrDefault(channelCode, true);
        boolean currentHealthy;

        try {
            currentHealthy = adapter.healthCheck();
        } catch (Exception e) {
            log.warn("渠道健康检查异常: channelCode={}", channelCode, e);
            currentHealthy = false;
        }

        if (currentHealthy) {
            // 健康：清除失败计数
            FAIL_COUNT_MAP.remove(channelCode);
            if (!previousHealthy) {
                // 从故障恢复
                recoverChannel(channelCode);
            }
            HEALTH_CACHE.put(channelCode, true);
        } else {
            // 不健康：累加失败计数
            int failCount = FAIL_COUNT_MAP.getOrDefault(channelCode, 0) + 1;
            FAIL_COUNT_MAP.put(channelCode, failCount);

            if (previousHealthy && failCount >= FAILURE_THRESHOLD) {
                // 达到故障阈值，标记为故障
                faultChannel(channelCode);
                HEALTH_CACHE.put(channelCode, false);
            }
        }
    }

    private void faultChannel(String channelCode) {
        try {
            var dto = messageChannelManager.getByChannelCode(channelCode);
            if (dto != null && dto.getEnabled() != null && dto.getEnabled() == 1) {
                messageChannelManager.updateStatus(dto.getId(), 2);
                log.error("【告警】渠道标记为故障状态: channelCode={}, channelName={}", channelCode, dto.getChannelName());
                messageMetrics.channelHealth(channelCode, false);
                sendAlert(channelCode, dto.getChannelName(), "故障");
            }
        } catch (Exception e) {
            log.error("更新渠道故障状态失败: channelCode={}", channelCode, e);
        }
    }

    private void recoverChannel(String channelCode) {
        try {
            var dto = messageChannelManager.getByChannelCode(channelCode);
            if (dto != null && dto.getEnabled() != null && dto.getEnabled() == 2) {
                messageChannelManager.updateStatus(dto.getId(), 1);
                log.info("渠道从故障恢复为启用状态: channelCode={}", channelCode);
                messageMetrics.channelHealth(channelCode, true);
            }
        } catch (Exception e) {
            log.error("更新渠道恢复状态失败: channelCode={}", channelCode, e);
        }
    }

    private void sendAlert(String channelCode, String channelName, String status) {
        try {
            if (simpMessagingTemplate != null) {
                cn.hutool.json.JSONObject payload = new cn.hutool.json.JSONObject();
                payload.set("type", "CHANNEL_ALERT");
                payload.set("channelCode", channelCode);
                payload.set("channelName", channelName);
                payload.set("status", status);
                payload.set("timestamp", System.currentTimeMillis());
                simpMessagingTemplate.convertAndSend("/topic/alerts", payload.toString());
            }
        } catch (Exception e) {
            log.warn("发送渠道告警通知失败: channelCode={}", channelCode, e);
        }
    }

    /**
     * 获取渠道当前健康状态
     *
     * @param channelCode 渠道编码
     * @return 是否健康
     */
    public static boolean isHealthy(String channelCode) {
        return HEALTH_CACHE.getOrDefault(channelCode, true);
    }

    /**
     * 手动重置渠道健康状态
     *
     * @param channelCode 渠道编码
     */
    public static void reset(String channelCode) {
        HEALTH_CACHE.remove(channelCode);
        FAIL_COUNT_MAP.remove(channelCode);
    }
}
