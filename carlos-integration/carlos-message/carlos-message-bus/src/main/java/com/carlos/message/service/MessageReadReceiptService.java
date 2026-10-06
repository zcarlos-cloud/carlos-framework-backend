package com.carlos.message.service;

import cn.hutool.json.JSONObject;
import com.carlos.message.manager.MessageReceiverManager;
import com.carlos.message.manager.MessageRecordManager;
import com.carlos.message.pojo.entity.MessageReceiver;
import com.carlos.message.pojo.entity.MessageRecord;
import com.carlos.message.pojo.enums.MessageReceiverStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 消息已读回执服务
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageReadReceiptService {

    private final MessageReceiverManager messageReceiverManager;
    private final MessageRecordManager messageRecordManager;
    private final SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 标记消息已读
     *
     * @param messageId  消息ID
     * @param receiverId 接收人ID
     * @return 是否成功
     */
    public boolean markRead(String messageId, String receiverId) {
        log.info("标记消息已读: messageId={}, receiverId={}", messageId, receiverId);

        List<MessageReceiver> receivers = messageReceiverManager.listByMessageId(messageId);
        MessageReceiver target = receivers.stream()
            .filter(r -> receiverId.equals(r.getReceiverId()))
            .findFirst()
            .orElse(null);

        if (target == null) {
            log.warn("接收人记录不存在: messageId={}, receiverId={}", messageId, receiverId);
            return false;
        }

        Integer status = target.getStatus();
        if (MessageReceiverStatusEnum.READ.getCode().equals(status)) {
            return true;
        }

        if (!MessageReceiverStatusEnum.SENT.getCode().equals(status)
            && !MessageReceiverStatusEnum.DELIVERED.getCode().equals(status)) {
            log.warn("消息状态不允许标记已读: messageId={}, receiverId={}, status={}", messageId, receiverId, status);
            return false;
        }

        // 更新为已读
        MessageReceiver update = new MessageReceiver();
        update.setId(target.getId());
        update.setStatus(MessageReceiverStatusEnum.READ.getCode());
        update.setReadTime(LocalDateTime.now());
        messageReceiverManager.updateById(update);

        // 通知发送人
        notifySender(messageId, receiverId);

        // 更新消息记录的已读统计
        updateReadStatistics(messageId, receivers);

        return true;
    }

    /**
     * 批量标记已读
     *
     * @param messageIds 消息ID列表
     * @param receiverId 接收人ID
     * @return 成功数量
     */
    public int markReadBatch(List<String> messageIds, String receiverId) {
        int successCount = 0;
        for (String messageId : messageIds) {
            if (markRead(messageId, receiverId)) {
                successCount++;
            }
        }
        return successCount;
    }

    private void notifySender(String messageId, String readerId) {
        try {
            MessageRecord record = messageRecordManager.getByMessageId(messageId);
            if (record == null || simpMessagingTemplate == null) {
                return;
            }

            String senderId = record.getSenderId();
            if (senderId == null) {
                return;
            }

            JSONObject payload = new JSONObject();
            payload.set("type", "READ_RECEIPT");
            payload.set("messageId", messageId);
            payload.set("readerId", readerId);
            payload.set("timestamp", System.currentTimeMillis());
            simpMessagingTemplate.convertAndSendToUser(senderId, "/queue/notification", payload.toString());
            log.debug("已读回执通知发送人: senderId={}, messageId={}", senderId, messageId);
        } catch (Exception e) {
            log.warn("已读回执通知失败: messageId={}", messageId, e);
        }
    }

    private void updateReadStatistics(String messageId, List<MessageReceiver> receivers) {
        try {
            long readCount = receivers.stream()
                .filter(r -> MessageReceiverStatusEnum.READ.getCode().equals(r.getStatus()))
                .count();
            long totalCount = receivers.stream()
                .filter(r -> !MessageReceiverStatusEnum.PENDING.getCode().equals(r.getStatus())
                    && !MessageReceiverStatusEnum.SENDING.getCode().equals(r.getStatus()))
                .count();

            if (totalCount > 0 && readCount == totalCount) {
                MessageRecord record = messageRecordManager.getByMessageId(messageId);
                if (record != null) {
                    record.setFeedbackType("ALL_READ");
                    record.setFeedbackContent("全部已读");
                    messageRecordManager.updateById(record);
                }
            }
        } catch (Exception e) {
            log.warn("更新已读统计失败: messageId={}", messageId, e);
        }
    }
}
