package com.carlos.message.task;

import com.carlos.message.core.channel.ChannelAdapter;
import com.carlos.message.core.channel.ChannelFactory;
import com.carlos.message.core.protocol.MessageContext;
import com.carlos.message.core.protocol.SendResult;
import com.carlos.message.core.retry.RetryPolicy;
import com.carlos.message.manager.MessageReceiverManager;
import com.carlos.message.manager.MessageRecordManager;
import com.carlos.message.manager.MessageSendLogManager;
import com.carlos.message.pojo.entity.MessageReceiver;
import com.carlos.message.pojo.enums.MessageReceiverStatusEnum;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * <p>
 * 消息重试定时任务
 * </p>
 *
 * <p>
 * 基于 Redis ZSet 实现重试队列，每秒扫描一次到期的重试消息，
 * 更新状态为 SENDING 后重新调用渠道适配器发送。
 * 超过最大重试次数的消息保持 FAILED 状态，不再重试。
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRetryTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final MessageReceiverManager receiverManager;
    private final MessageRecordManager recordManager;
    private final MessageSendLogManager sendLogManager;
    private final RetryPolicy retryPolicy;

    private static final String RETRY_ZSET_KEY = "message:retry:zset";

    /**
     * 每秒扫描一次重试队列
     */
    @Scheduled(fixedDelay = 1000)
    public void processRetryMessages() {
        double now = System.currentTimeMillis();
        Set<ZSetOperations.TypedTuple<String>> tuples =
            stringRedisTemplate.opsForZSet().rangeByScoreWithScores(RETRY_ZSET_KEY, 0D, now);

        if (tuples == null || tuples.isEmpty()) {
            return;
        }

        for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<String> tuple : tuples) {
            String json = tuple.getValue();
            if (json == null) {
                continue;
            }
            try {
                RetryMessage retryMessage = objectMapper.readValue(json, RetryMessage.class);
                processRetry(retryMessage);
                stringRedisTemplate.opsForZSet().remove(RETRY_ZSET_KEY, json);
            } catch (Exception e) {
                log.error("处理重试消息失败: json={}", json, e);
                stringRedisTemplate.opsForZSet().remove(RETRY_ZSET_KEY, json);
            }
        }
    }

    private void processRetry(RetryMessage retryMessage) {
        MessageReceiver receiver = receiverManager.getById(retryMessage.getReceiverId());
        if (receiver == null) {
            log.warn("重试消息接收人不存在: receiverId={}", retryMessage.getReceiverId());
            return;
        }

        Integer status = receiver.getStatus();
        if (!MessageReceiverStatusEnum.FAILED.getCode().equals(status)
            && !MessageReceiverStatusEnum.PENDING.getCode().equals(status)) {
            log.debug("接收人状态已变更，跳过重试: receiverId={}, status={}", receiver.getId(), status);
            return;
        }

        if (receiver.getFailCount() != null && receiver.getFailCount() >= retryPolicy.getMaxRetries()) {
            log.warn("消息重试次数已达上限: receiverId={}, failCount={}", receiver.getId(), receiver.getFailCount());
            return;
        }

        String channelCode = retryMessage.getChannelCode();
        String messageId = retryMessage.getMessageId();
        long startTime = System.currentTimeMillis();

        try {
            MessageReceiver update = new MessageReceiver();
            update.setId(receiver.getId());
            update.setStatus(MessageReceiverStatusEnum.SENDING.getCode());
            receiverManager.updateById(update);

            ChannelAdapter adapter = ChannelFactory.getChannel(channelCode);
            if (adapter == null) {
                markFailed(receiver, "渠道适配器不存在", messageId, channelCode, startTime);
                return;
            }

            MessageContext context = buildContext(retryMessage, receiver);
            SendResult result = adapter.send(context);
            long costTime = System.currentTimeMillis() - startTime;

            saveSendLog(messageId, receiver.getId(), channelCode, toJson(retryMessage), result, costTime);

            if (result.isSuccess()) {
                MessageReceiver successUpdate = new MessageReceiver();
                successUpdate.setId(receiver.getId());
                successUpdate.setStatus(MessageReceiverStatusEnum.SENT.getCode());
                successUpdate.setChannelMessageId(result.getChannelMessageId());
                successUpdate.setSendTime(LocalDateTime.now());
                receiverManager.updateById(successUpdate);
                recordManager.updateStatistics(messageId, 1, 0);
                log.info("消息重试发送成功: messageId={}, receiverId={}", messageId, receiver.getId());
            } else {
                markFailed(receiver, result.getErrorMessage(), messageId, channelCode, startTime);
            }
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("消息重试发送异常: messageId={}, receiverId={}", messageId, receiver.getId(), e);
            markFailed(receiver, e.getMessage(), messageId, channelCode, costTime);
        }
    }

    private void markFailed(MessageReceiver receiver, String reason,
                            String messageId, String channelCode, long startTime) {
        receiverManager.incrementFailCount(receiver.getId(), reason);
        saveSendLog(messageId, receiver.getId(), channelCode, null,
            SendResult.fail(reason), System.currentTimeMillis() - startTime);
    }

    private void saveSendLog(String messageId, Long receiverId, String channelCode,
                             String requestParam, SendResult result, long costTime) {
        try {
            com.carlos.message.pojo.entity.MessageSendLog sendLog = new com.carlos.message.pojo.entity.MessageSendLog();
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

    private MessageContext buildContext(RetryMessage retry, MessageReceiver receiver) {
        return MessageContext.builder()
            .messageId(retry.getMessageId())
            .channelCode(retry.getChannelCode())
            .receiverId(receiver.getReceiverId())
            .receiverType(receiver.getReceiverType())
            .receiverNumber(receiver.getReceiverNumber())
            .messageTitle(retry.getMessageTitle())
            .messageContent(retry.getMessageContent())
            .messageType(retry.getMessageType())
            .priority(retry.getPriority())
            .callbackUrl(retry.getCallbackUrl())
            .sendTime(LocalDateTime.now())
            .build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将消息加入重试队列
     */
    public void enqueueRetry(MessageReceiver receiver, String messageTitle, String messageContent,
                             String messageType, Integer priority, String callbackUrl) {
        try {
            RetryMessage retryMessage = new RetryMessage();
            retryMessage.setMessageId(receiver.getMessageId());
            retryMessage.setReceiverId(receiver.getId());
            retryMessage.setChannelCode(receiver.getChannelCode());
            retryMessage.setMessageTitle(messageTitle);
            retryMessage.setMessageContent(messageContent);
            retryMessage.setMessageType(messageType);
            retryMessage.setPriority(priority);
            retryMessage.setCallbackUrl(callbackUrl);

            int retryCount = receiver.getFailCount() != null ? receiver.getFailCount() : 0;
            long delay = retryPolicy.getRetryInterval(retryCount + 1);
            double score = System.currentTimeMillis() + delay;

            String json = objectMapper.writeValueAsString(retryMessage);
            stringRedisTemplate.opsForZSet().add(RETRY_ZSET_KEY, json, score);
            log.info("消息加入重试队列: messageId={}, receiverId={}, delayMs={}",
                receiver.getMessageId(), receiver.getId(), delay);
        } catch (Exception e) {
            log.error("加入重试队列失败: receiverId={}", receiver.getId(), e);
        }
    }

    /**
     * 重试消息内部传输对象
     */
    @Data
    public static class RetryMessage {
        private String messageId;
        private Long receiverId;
        private String channelCode;
        private String messageTitle;
        private String messageContent;
        private String messageType;
        private Integer priority;
        private String callbackUrl;
    }
}
