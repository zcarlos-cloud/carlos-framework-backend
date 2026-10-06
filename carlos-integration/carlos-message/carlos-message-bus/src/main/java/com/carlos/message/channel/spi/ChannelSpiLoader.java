package com.carlos.message.channel.spi;

import com.carlos.message.core.channel.ChannelAdapter;
import com.carlos.message.core.channel.ChannelFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ServiceLoader;

/**
 * <p>
 * 渠道适配器 SPI 加载器
 * </p>
 *
 * <p>
 * 支持通过 Java SPI 机制（META-INF/services/com.carlos.message.core.channel.ChannelAdapter）
 * 加载第三方渠道适配器，实现插件化扩展。
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Component
public class ChannelSpiLoader {

    @PostConstruct
    public void load() {
        ServiceLoader<ChannelAdapter> loader = ServiceLoader.load(ChannelAdapter.class,
            Thread.currentThread().getContextClassLoader());
        int count = 0;
        for (ChannelAdapter adapter : loader) {
            try {
                ChannelFactory.register(adapter);
                log.info("SPI 渠道适配器加载成功: channelCode={}", adapter.getChannelCode());
                count++;
            } catch (Exception e) {
                log.error("SPI 渠道适配器加载失败: {}", adapter.getClass().getName(), e);
            }
        }
        if (count > 0) {
            log.info("共加载 {} 个 SPI 渠道适配器", count);
        }
    }
}
