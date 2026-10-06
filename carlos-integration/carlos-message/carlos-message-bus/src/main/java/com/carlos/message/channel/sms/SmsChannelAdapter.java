package com.carlos.message.channel.sms;

import cn.hutool.core.util.StrUtil;
import com.carlos.message.channel.AbstractChannelAdapter;
import com.carlos.message.core.channel.ChannelType;
import com.carlos.message.core.protocol.MessageContext;
import com.carlos.message.core.protocol.SendResult;
import com.carlos.message.manager.MessageChannelManager;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 短信渠道适配器（基于 sms4j）
 * </p>
 *
 * @author Carlos
 * @date 2026/3/12
 */
@Slf4j
@Component
public class SmsChannelAdapter extends AbstractChannelAdapter {

    public SmsChannelAdapter(MessageChannelManager messageChannelManager) {
        super(messageChannelManager);
    }

    @Override
    public String getChannelCode() {
        return "SMS";
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.SMS;
    }

    @Override
    public boolean supports(String messageType) {
        return "VERIFY_CODE".equals(messageType)
            || "SYSTEM_NOTIFY".equals(messageType)
            || "TASK_NOTIFY".equals(messageType)
            || "APPROVAL_REMIND".equals(messageType);
    }

    @Override
    public boolean healthCheck() {
        try {
            return super.isAvailable() && !SmsFactory.getAll().isEmpty();
        } catch (Exception e) {
            log.warn("短信渠道健康检查失败", e);
            return false;
        }
    }

    @Override
    protected SendResult doSend(MessageContext context) {
        String phoneNumber = context.getReceiverNumber();
        if (StrUtil.isBlank(phoneNumber)) {
            return SendResult.fail("接收号码不能为空");
        }

        String content = context.getMessageContent();
        if (StrUtil.isBlank(content)) {
            return SendResult.fail("短信内容不能为空");
        }

        try {
            SmsBlend smsBlend = SmsFactory.getSmsBlend();
            if (smsBlend == null) {
                return SendResult.fail("未找到可用的短信配置");
            }

            SmsResponse response = smsBlend.sendMessage(phoneNumber, content);
            if (response != null && Boolean.TRUE.equals(response.isSuccess())) {
                String channelMessageId = response.getConfigId() != null
                    ? response.getConfigId() + "_" + System.currentTimeMillis()
                    : "SMS" + System.currentTimeMillis();
                return SendResult.success(channelMessageId);
            } else {
                String errorMsg = response != null ? response.getData().toString() : "短信发送失败";
                return SendResult.fail("SMS_SEND_FAILED", errorMsg);
            }
        } catch (Exception e) {
            log.error("短信发送异常: phone={}", phoneNumber, e);
            return SendResult.fail("SMS_SEND_EXCEPTION", e.getMessage());
        }
    }
}
