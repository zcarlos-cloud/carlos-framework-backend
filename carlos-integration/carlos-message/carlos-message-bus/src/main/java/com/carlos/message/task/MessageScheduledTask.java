package com.carlos.message.task;

import com.carlos.message.core.channel.ChannelAdapter;
import com.carlos.message.core.channel.ChannelFactory;
import com.carlos.message.core.protocol.MessageContext;
import com.carlos.message.core.protocol.SendResult;
import com.carlos.message.manager.MessageReceiverManager;
import com.carlos.message.manager.MessageRecordManager;
import com.carlos.message.manager.MessageSendLogManager;
import com.carlos.message.pojo.entity.MessageReceiver;
import com.carlos.message.pojo.entity.MessageSendLog;
import com.carlos.message.pojo.enums.MessageReceiverStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 定时发送任务：轮询 schedule_time 已到期的 PENDING 接收人记录并触发发送
 * </p>
 *
 * @author Carlos
 * @date 2026/3/12
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class MessageScheduledTask {

    private final MessageReceiverManager receiverManager;
    private final MessageRecordManager recordManager;
    private final MessageSendLogManager sendLogManager;

    /**
     * 每分钟检查一次已到期的定时消息并发送
     */
    @Scheduled(fixedDelay = 60000)
    public void processScheduledMessages() {
        LocalDateTime now = LocalDateTime.now();
        List<MessageReceiver> pendingList = receiverManager.listScheduledPending(now);

        if (pendingList.isEmpty()) {
            return;
        }

        log.info("定时任务扫描到待发消息: count={}", pendingList.size());

        // 按 messageId 分组，逐消息处理
        Map<String, List<MessageReceiver>> grouped = pendingList.stream()
            .collect(Collectors.groupingBy(MessageReceiver::getMessageId));

        for (Map.Entry<String, List<MessageReceiver>> entry : grouped.entrySet()) {
            String messageId = entry.getKey();
            List<MessageReceiver> receivers = entry.getValue();
            int successCount = 0;
            int failCount = 0;

            for (MessageReceiver receiver : receivers) {
                long startTime = System.currentTimeMillis();
                String channelCode = receiver.getChannelCode();
                try {
                    ChannelAdapter adapter = ChannelFactory.getChannel(channelCode);
                    if (adapter == null) {
                        log.warn("定时发送：渠道适配器不存在, channel={}", channelCode);
                        markFailed(receiver, "渠道适配器不存在", messageId, channelCode, startTime);
                        failCount++;
                        continue;
                    }

                    // 更新为发送中
                    MessageReceiver sendingUpdate = new MessageReceiver();
                    sendingUpdate.setId(receiver.getId());
                    sendingUpdate.setStatus(MessageReceiverStatusEnum.SENDING.getCode());
                    receiverManager.updateById(sendingUpdate);

                    MessageContext context = MessageContext.builder()
                        .messageId(messageId)
                        .channelCode(channelCode)
                        .receiverId(receiver.getReceiverId())
                        .receiverType(receiver.getReceiverType())
                        .receiverNumber(receiver.getReceiverNumber())
                        .callbackUrl(receiver.getCallbackUrl())
                        .sendTime(LocalDateTime.now())
                        .build();

                    SendResult result = adapter.send(context);
                    long costTime = System.currentTimeMillis() - startTime;
                    saveSendLog(messageId, receiver.getId(), channelCode, null, result, costTime);

                    if (result.isSuccess()) {
                        MessageReceiver update = new MessageReceiver();
                        update.setId(receiver.getId());
                        update.setStatus(MessageReceiverStatusEnum.SENT.getCode());
                        update.setChannelMessageId(result.getChannelMessageId());
                        update.setSendTime(LocalDateTime.now());
                        receiverManager.updateById(update);
                        successCount++;
                    } else {
                        receiverManager.incrementFailCount(receiver.getId(), result.getErrorMessage());
                        failCount++;
                    }
                } catch (Exception e) {
                    long costTime = System.currentTimeMillis() - startTime;
                    log.error("定时发送异常, messageId={}, channel={}", messageId, channelCode, e);
                    markFailed(receiver, e.getMessage(), messageId, channelCode, costTime);
                    failCount++;
                }
            }

            recordManager.updateStatistics(messageId, successCount, failCount);
            log.info("定时发送完成, messageId={}, success={}, fail={}", messageId, successCount, failCount);
        }
    }

    private void markFailed(MessageReceiver receiver, String reason,
                            String messageId, String channelCode, long costTime) {
        receiverManager.incrementFailCount(receiver.getId(), reason);
        saveSendLog(messageId, receiver.getId(), channelCode, null,
            SendResult.fail(reason), costTime);
    }

    private void saveSendLog(String messageId, Long receiverId, String channelCode,
                             String requestParam, SendResult result, long costTime) {
        try {
            MessageSendLog sendLog = new MessageSendLog();
            sendLog.setMessageId(messageId);
            sendLog.setReceiverId(receiverId);
            sendLog.setChannelCode(channelCode);
            sendLog.setRequestParam(requestParam);
            sendLog.setResponseData(result.getChannelMessageId());
            sendLog.setSuccess(result.isSuccess());
            sendLog.setErrorCode(result.getErrorCode());
            sendLog.setErrorMessage(result.getErrorMessage());
            sendLog.setCostTime((int) costTime);
            sendLog.setCreateTime(LocalDateTime.now());
            sendLogManager.save(sendLog);
        } catch (Exception e) {
            log.error("保存发送日志失败, messageId={}, channelCode={}", messageId, channelCode, e);
        }
    }
}
