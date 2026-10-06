package com.carlos.message.service;

import cn.hutool.json.JSONUtil;
import com.carlos.message.core.channel.ChannelAdapter;
import com.carlos.message.core.channel.ChannelFactory;
import com.carlos.message.core.protocol.MessageContext;
import com.carlos.message.core.protocol.SendResult;
import com.carlos.message.core.retry.RetryPolicy;
import com.carlos.message.manager.MessageReceiverManager;
import com.carlos.message.manager.MessageRecordManager;
import com.carlos.message.manager.MessageSendLogManager;
import com.carlos.message.pojo.entity.MessageReceiver;
import com.carlos.message.pojo.entity.MessageSendLog;
import com.carlos.message.pojo.enums.MessageReceiverStatusEnum;
import com.carlos.message.pojo.param.MessageCreateParam;
import com.carlos.message.task.MessageRetryTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 消息异步发送执行器
 * 独立 Bean 以确保 @Async 代理生效（避免自调用问题）
 * </p>
 *
 * @author Carlos
 * @date 2026/3/12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageAsyncExecutor {

    private final MessageReceiverManager messageReceiverManager;
    private final MessageRecordManager messageRecordManager;
    private final MessageSendLogManager messageSendLogManager;
    private final MessageRetryTask messageRetryTask;
    private final RetryPolicy retryPolicy;

    /**
     * 异步执行消息发送逻辑
     *
     * @param messageId      消息ID
     * @param messageContent 消息内容
     * @param messageTitle   消息标题
     * @param messageType    消息类型编码
     * @param param          原始请求参数
     */
    @Async("messageTaskExecutor")
    public void doSendAsync(String messageId, String messageContent, String messageTitle,
                            String messageType, MessageCreateParam param) {
        log.info("异步发送任务开始: messageId={}", messageId);
        try {
            List<MessageReceiver> receivers = messageReceiverManager.listByMessageId(messageId);
            int totalSuccess = 0;
            int totalFail = 0;

            for (MessageReceiver receiver : receivers) {
                if (!MessageReceiverStatusEnum.PENDING.getCode().equals(receiver.getStatus())) {
                    continue;
                }
                String channelCode = receiver.getChannelCode();
                long startTime = System.currentTimeMillis();

                try {
                    ChannelAdapter adapter = ChannelFactory.getChannel(channelCode);
                    if (adapter == null) {
                        updateReceiverStatus(receiver.getId(), MessageReceiverStatusEnum.FAILED, "渠道适配器不存在");
                        saveSendLog(messageId, receiver.getId(), channelCode, null,
                            SendResult.fail("渠道适配器不存在"), System.currentTimeMillis() - startTime);
                        totalFail++;
                        continue;
                    }

                    // 更新为发送中
                    updateReceiverStatus(receiver.getId(), MessageReceiverStatusEnum.SENDING, null);

                    MessageCreateParam.ReceiverParam receiverParam = new MessageCreateParam.ReceiverParam();
                    receiverParam.setReceiverId(receiver.getReceiverId());
                    receiverParam.setReceiverType(receiver.getReceiverType());
                    receiverParam.setReceiverNumber(receiver.getReceiverNumber());

                    MessageContext context = MessageContext.builder()
                        .messageId(messageId)
                        .channelCode(channelCode)
                        .receiverId(receiver.getReceiverId())
                        .receiverType(receiver.getReceiverType())
                        .receiverNumber(receiver.getReceiverNumber())
                        .messageTitle(messageTitle)
                        .messageContent(messageContent)
                        .messageType(messageType)
                        .priority(param.getPriority())
                        .callbackUrl(param.getCallbackUrl())
                        .sendTime(LocalDateTime.now())
                        .build();

                    SendResult sendResult = adapter.send(context);
                    long costTime = System.currentTimeMillis() - startTime;
                    saveSendLog(messageId, receiver.getId(), channelCode, JSONUtil.toJsonStr(context), sendResult, costTime);

                    if (sendResult.isSuccess()) {
                        updateReceiverStatus(receiver.getId(), MessageReceiverStatusEnum.SENT,
                            sendResult.getChannelMessageId());
                        totalSuccess++;
                    } else {
                        handleSendFailure(receiver, sendResult, messageId, channelCode, messageTitle,
                            messageContent, messageType, param.getPriority(), param.getCallbackUrl());
                        totalFail++;
                    }
                } catch (Exception e) {
                    long costTime = System.currentTimeMillis() - startTime;
                    log.error("异步发送异常, messageId={}, channel={}", messageId, channelCode, e);
                    handleSendFailure(receiver, SendResult.fail(e.getMessage()), messageId, channelCode,
                        messageTitle, messageContent, messageType, param.getPriority(), param.getCallbackUrl());
                    saveSendLog(messageId, receiver.getId(), channelCode, null,
                        SendResult.fail(e.getMessage()), costTime);
                    totalFail++;
                }
            }

            messageRecordManager.updateStatistics(messageId, totalSuccess, totalFail);
            log.info("异步发送任务完成: messageId={}, success={}, fail={}", messageId, totalSuccess, totalFail);
        } catch (Exception e) {
            log.error("异步发送任务异常: messageId={}", messageId, e);
        }
    }

    private void handleSendFailure(MessageReceiver receiver, SendResult result, String messageId,
                                   String channelCode, String messageTitle, String messageContent,
                                   String messageType, Integer priority, String callbackUrl) {
        int failCount = receiver.getFailCount() != null ? receiver.getFailCount() : 0;
        if (retryPolicy.shouldRetry(failCount + 1, result.getErrorCode())) {
            messageReceiverManager.incrementFailCount(receiver.getId(), result.getErrorMessage());
            messageRetryTask.enqueueRetry(receiver, messageTitle, messageContent, messageType, priority, callbackUrl);
        } else {
            updateReceiverStatus(receiver.getId(), MessageReceiverStatusEnum.FAILED, result.getErrorMessage());
        }
    }

    private void updateReceiverStatus(Long receiverId, MessageReceiverStatusEnum status, String remark) {
        MessageReceiver update = new MessageReceiver();
        update.setId(receiverId);
        update.setStatus(status.getCode());
        if (status == MessageReceiverStatusEnum.SENT || status == MessageReceiverStatusEnum.DELIVERED) {
            update.setChannelMessageId(remark);
            update.setSendTime(LocalDateTime.now());
            messageReceiverManager.updateById(update);
        } else if (status == MessageReceiverStatusEnum.FAILED) {
            messageReceiverManager.incrementFailCount(receiverId, remark);
        } else {
            messageReceiverManager.updateById(update);
        }
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
            messageSendLogManager.save(sendLog);
        } catch (Exception e) {
            log.error("保存发送日志失败, messageId={}, channelCode={}", messageId, channelCode, e);
        }
    }
}
