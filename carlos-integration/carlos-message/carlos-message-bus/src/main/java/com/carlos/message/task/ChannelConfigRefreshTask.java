package com.carlos.message.task;

import com.carlos.message.channel.AbstractChannelAdapter;
import com.carlos.message.core.channel.ChannelAdapter;
import com.carlos.message.core.channel.ChannelFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * 渠道配置热刷新定时任务
 * </p>
 *
 * <p>
 * 定期从数据库重新加载渠道配置，实现配置变更无需重启服务。
 * 刷新间隔默认 30 秒。
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Component
public class ChannelConfigRefreshTask {

    /**
     * 定时刷新渠道配置（每30秒）
     */
    @Scheduled(fixedRate = 30000)
    public void refreshAllChannels() {
        List<ChannelAdapter> channels = ChannelFactory.getAllChannels();
        if (channels.isEmpty()) {
            return;
        }

        log.debug("开始刷新渠道配置, 数量: {}", channels.size());
        for (ChannelAdapter adapter : channels) {
            if (adapter instanceof AbstractChannelAdapter abstractAdapter) {
                try {
                    abstractAdapter.refreshConfig();
                } catch (Exception e) {
                    log.error("刷新渠道配置失败: {}", adapter.getChannelCode(), e);
                }
            }
        }
    }
}
