package com.carlos.message.channel;

import com.carlos.message.core.channel.ChannelAdapter;
import com.carlos.message.core.channel.ChannelFactory;
import com.carlos.message.core.protocol.MessageContext;
import com.carlos.message.core.protocol.SendResult;
import com.carlos.message.manager.MessageChannelManager;
import com.carlos.message.pojo.dto.MessageChannelDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;

/**
 * <p>
 * 抽象渠道适配器
 * </p>
 *
 * @author Carlos
 * @date 2026/3/12
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractChannelAdapter implements ChannelAdapter, InitializingBean {

    protected final MessageChannelManager messageChannelManager;

    /**
     * 渠道配置缓存
     */
    protected MessageChannelDTO channelConfig;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 注册到渠道工厂
        ChannelFactory.register(this);
        log.info("渠道适配器注册成功: {}", getChannelCode());
    }

    @Override
    public boolean isAvailable() {
        if (channelConfig == null) {
            loadChannelConfig();
        }
        return channelConfig != null &&
            Objects.equals(channelConfig.getEnabled(), 1);
    }

    /**
     * 加载渠道配置
     */
    protected void loadChannelConfig() {
        try {
            MessageChannelDTO dto = messageChannelManager.getByChannelCode(getChannelCode());
            if (dto != null) {
                this.channelConfig = dto;
            }
        } catch (Exception e) {
            log.error("加载渠道配置失败: {}", getChannelCode(), e);
        }
    }

    /**
     * 刷新渠道配置（外部调用以热更新）
     */
    public void refreshConfig() {
        loadChannelConfig();
        log.info("渠道配置已刷新: {}", getChannelCode());
    }

    @Override
    public SendResult send(MessageContext context) {
        if (!isAvailable()) {
            return SendResult.fail("渠道不可用: " + getChannelCode());
        }

        if (!supports(context.getMessageType())) {
            return SendResult.fail("渠道不支持该消息类型: " + context.getMessageType());
        }

        try {
            log.info("开始发送消息, channel={}, messageId={}", getChannelCode(), context.getMessageId());
            SendResult result = doSend(context);
            log.info("消息发送完成, channel={}, messageId={}, success={}",
                getChannelCode(), context.getMessageId(), result.isSuccess());
            return result;
        } catch (Exception e) {
            log.error("消息发送异常, channel={}, messageId={}", getChannelCode(), context.getMessageId(), e);
            return SendResult.fail("发送异常: " + e.getMessage());
        }
    }

    /**
     * 具体渠道发送实现
     *
     * @param context 消息上下文
     * @return 发送结果
     */
    protected abstract SendResult doSend(MessageContext context);

    /**
     * 获取渠道配置
     *
     * @param <T> 配置类型
     * @param configClass 配置类
     * @return 渠道配置
     */
    @SuppressWarnings("unchecked")
    protected <T> T getChannelConfig(Class<T> configClass) {
        if (channelConfig == null || channelConfig.getChannelConfig() == null) {
            return null;
        }
        // 这里可以实现 JSON 到对象的转换
        // 简化处理，实际应该使用 JSON 解析
        return (T) channelConfig.getChannelConfig();
    }
}
