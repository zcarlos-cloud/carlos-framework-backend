package com.carlos.message.channel;

import com.carlos.message.core.channel.ChannelAdapter;
import com.carlos.message.core.channel.ChannelFactory;
import com.carlos.message.core.channel.ChannelType;
import com.carlos.message.core.protocol.MessageContext;
import com.carlos.message.core.protocol.SendResult;
import com.carlos.message.manager.MessageChannelManager;
import com.carlos.message.pojo.dto.MessageChannelDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * <p>
 * 渠道路由器
 * </p>
 *
 * <p>
 * 支持按渠道类型路由，根据权重选择具体服务商，并具备故障转移能力。
 * 当同一渠道类型配置了多个服务商时（如阿里云短信 + 腾讯云短信），
 * 按权重随机选择；发送失败时自动切换到同类型其他可用渠道。
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelRouter {

    private final MessageChannelManager messageChannelManager;

    /**
     * 按渠道编码获取适配器（直接路由）
     *
     * @param channelCode 渠道编码
     * @return 适配器
     */
    public ChannelAdapter route(String channelCode) {
        return ChannelFactory.getChannel(channelCode);
    }

    /**
     * 按渠道类型路由（权重随机选择）
     *
     * @param channelType 渠道类型
     * @param messageType 消息类型
     * @return 适配器
     */
    public ChannelAdapter routeByType(ChannelType channelType, String messageType) {
        List<ChannelAdapter> candidates = ChannelFactory.getChannelsByType(channelType)
            .stream()
            .filter(a -> a.isAvailable() && a.supports(messageType))
            .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            log.warn("无可用渠道适配器: channelType={}, messageType={}", channelType, messageType);
            return null;
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        // 多个候选时，按权重选择
        return selectByWeight(candidates);
    }

    /**
     * 发送消息并支持故障转移
     *
     * @param context     消息上下文
     * @param channelType 渠道类型
     * @return 发送结果
     */
    public SendResult sendWithFallback(MessageContext context, ChannelType channelType) {
        List<ChannelAdapter> candidates = ChannelFactory.getChannelsByType(channelType)
            .stream()
            .filter(a -> a.isAvailable() && a.supports(context.getMessageType()))
            .sorted((a1, a2) -> {
                // 按权重降序
                int w1 = getWeight(a1.getChannelCode());
                int w2 = getWeight(a2.getChannelCode());
                return Integer.compare(w2, w1);
            })
            .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return SendResult.fail("NO_AVAILABLE_CHANNEL", "无可用渠道: " + channelType.getDesc());
        }

        SendResult lastResult = null;
        for (ChannelAdapter adapter : candidates) {
            try {
                lastResult = adapter.send(context);
                if (lastResult.isSuccess()) {
                    // 记录实际使用的渠道
                    context.setChannelCode(adapter.getChannelCode());
                    return lastResult;
                }
                log.warn("渠道发送失败, 尝试下一个, channelCode={}, error={}",
                    adapter.getChannelCode(), lastResult.getErrorMessage());
            } catch (Exception e) {
                log.error("渠道发送异常, channelCode={}", adapter.getChannelCode(), e);
                lastResult = SendResult.fail("CHANNEL_EXCEPTION", e.getMessage());
            }
        }

        return lastResult != null ? lastResult
            : SendResult.fail("ALL_CHANNELS_FAILED", "所有渠道均发送失败");
    }

    /**
     * 批量获取渠道适配器（按类型分组后各选一个）
     *
     * @param channelTypes 渠道类型列表
     * @param messageType  消息类型
     * @return 渠道编码 -> 适配器
     */
    public Map<String, ChannelAdapter> routeBatch(List<ChannelType> channelTypes, String messageType) {
        Map<String, ChannelAdapter> result = new LinkedHashMap<>();
        for (ChannelType type : channelTypes) {
            ChannelAdapter adapter = routeByType(type, messageType);
            if (adapter != null) {
                result.put(adapter.getChannelCode(), adapter);
            }
        }
        return result;
    }

    /**
     * 按权重随机选择适配器
     */
    private ChannelAdapter selectByWeight(List<ChannelAdapter> candidates) {
        int totalWeight = 0;
        Map<ChannelAdapter, Integer> weightMap = new LinkedHashMap<>();
        for (ChannelAdapter adapter : candidates) {
            int weight = getWeight(adapter.getChannelCode());
            weight = Math.max(weight, 1); // 最小权重为1
            weightMap.put(adapter, weight);
            totalWeight += weight;
        }

        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int current = 0;
        for (Map.Entry<ChannelAdapter, Integer> entry : weightMap.entrySet()) {
            current += entry.getValue();
            if (random < current) {
                return entry.getKey();
            }
        }

        // 兜底返回最后一个
        return candidates.get(candidates.size() - 1);
    }

    private int getWeight(String channelCode) {
        try {
            MessageChannelDTO dto = messageChannelManager.getByChannelCode(channelCode);
            if (dto != null && dto.getWeight() != null && dto.getWeight() > 0) {
                return dto.getWeight();
            }
        } catch (Exception e) {
            log.warn("获取渠道权重失败: channelCode={}", channelCode);
        }
        return 1;
    }
}
