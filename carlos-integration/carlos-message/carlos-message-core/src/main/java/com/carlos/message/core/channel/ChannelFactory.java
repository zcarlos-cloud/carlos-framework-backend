package com.carlos.message.core.channel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 渠道工厂
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
public class ChannelFactory {

    private static final Map<String, ChannelAdapter> CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * 注册渠道适配器
     *
     * @param adapter 适配器
     */
    public static void register(ChannelAdapter adapter) {
        CHANNEL_MAP.put(adapter.getChannelCode(), adapter);
    }

    /**
     * 获取渠道适配器
     *
     * @param channelCode 渠道编码
     * @return 适配器
     */
    public static ChannelAdapter getChannel(String channelCode) {
        return CHANNEL_MAP.get(channelCode);
    }

    /**
     * 获取所有渠道适配器
     *
     * @return 适配器列表
     */
    public static List<ChannelAdapter> getAllChannels() {
        return List.copyOf(CHANNEL_MAP.values());
    }

    /**
     * 获取指定类型的渠道适配器
     *
     * @param channelType 渠道类型
     * @return 适配器列表
     */
    public static List<ChannelAdapter> getChannelsByType(ChannelType channelType) {
        return CHANNEL_MAP.values().stream()
                .filter(channel -> channel.getChannelType() == channelType)
                .toList();
    }

    /**
     * 判断渠道是否存在
     *
     * @param channelCode 渠道编码
     * @return 是否存在
     */
    public static boolean hasChannel(String channelCode) {
        return CHANNEL_MAP.containsKey(channelCode);
    }

    /**
     * 移除渠道适配器
     *
     * @param channelCode 渠道编码
     */
    public static void remove(String channelCode) {
        CHANNEL_MAP.remove(channelCode);
    }
}
