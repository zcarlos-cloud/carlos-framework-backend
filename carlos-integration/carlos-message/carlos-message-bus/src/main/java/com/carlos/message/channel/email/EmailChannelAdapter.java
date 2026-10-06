package com.carlos.message.channel.email;

import cn.hutool.core.util.StrUtil;
import com.carlos.message.channel.AbstractChannelAdapter;
import com.carlos.message.core.channel.ChannelType;
import com.carlos.message.core.protocol.MessageContext;
import com.carlos.message.core.protocol.SendResult;
import com.carlos.message.manager.MessageChannelManager;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 邮件渠道适配器
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Component
public class EmailChannelAdapter extends AbstractChannelAdapter {

    private final JavaMailSender mailSender;

    public EmailChannelAdapter(MessageChannelManager messageChannelManager, JavaMailSender mailSender) {
        super(messageChannelManager);
        this.mailSender = mailSender;
    }

    @Override
    public String getChannelCode() {
        return "EMAIL";
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.EMAIL;
    }

    @Override
    public boolean supports(String messageType) {
        return true;
    }

    @Override
    public boolean healthCheck() {
        try {
            return super.isAvailable() && mailSender != null;
        } catch (Exception e) {
            log.warn("邮件渠道健康检查失败", e);
            return false;
        }
    }

    @Override
    protected SendResult doSend(MessageContext context) {
        String toAddress = context.getReceiverNumber();
        if (StrUtil.isBlank(toAddress)) {
            return SendResult.fail("收件人邮箱不能为空");
        }

        String subject = context.getMessageTitle();
        if (StrUtil.isBlank(subject)) {
            subject = "消息通知";
        }

        String content = context.getMessageContent();
        if (StrUtil.isBlank(content)) {
            return SendResult.fail("邮件内容不能为空");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(toAddress);
            helper.setSubject(subject);
            helper.setText(content, true);

            String from = getConfigValue("from");
            if (StrUtil.isNotBlank(from)) {
                helper.setFrom(from);
            }

            mailSender.send(mimeMessage);
            String messageId = "EMAIL" + System.currentTimeMillis();
            return SendResult.success(messageId);
        } catch (Exception e) {
            log.error("邮件发送异常: to={}", toAddress, e);
            return SendResult.fail("EMAIL_SEND_EXCEPTION", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String getConfigValue(String key) {
        if (channelConfig == null || channelConfig.getChannelConfig() == null) {
            return null;
        }
        try {
            cn.hutool.json.JSONObject config = cn.hutool.json.JSONUtil.parseObj(channelConfig.getChannelConfig());
            return config.getStr(key);
        } catch (Exception e) {
            log.warn("解析邮件配置失败", e);
            return null;
        }
    }
}
