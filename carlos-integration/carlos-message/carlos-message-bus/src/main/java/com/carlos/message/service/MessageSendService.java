package com.carlos.message.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.carlos.core.response.Result;
import com.carlos.message.core.channel.ChannelAdapter;
import com.carlos.message.core.channel.ChannelFactory;
import com.carlos.message.core.channel.ChannelType;
import com.carlos.message.core.limiter.RateLimiter;
import com.carlos.message.core.protocol.MessageContext;
import com.carlos.message.core.protocol.SendResult;
import com.carlos.message.core.template.TemplateEngine;
import com.carlos.message.manager.*;
import com.carlos.message.metrics.MessageMetrics;
import com.carlos.message.pojo.dto.MessageChannelDTO;
import com.carlos.message.pojo.dto.MessageTemplateDTO;
import com.carlos.message.pojo.entity.MessageReceiver;
import com.carlos.message.pojo.entity.MessageRecord;
import com.carlos.message.pojo.entity.MessageSendLog;
import com.carlos.message.pojo.enums.MessageReceiverStatusEnum;
import com.carlos.message.pojo.param.MessageCreateParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p>
 * 消息发送服务
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSendService {

    private final MessageRecordManager messageRecordManager;
    private final MessageReceiverManager messageReceiverManager;
    private final MessageTemplateManager messageTemplateManager;
    private final MessageChannelManager messageChannelManager;
    private final MessageSendLogManager messageSendLogManager;
    private final MessageAsyncExecutor messageAsyncExecutor;
    private final TemplateEngine templateEngine;
    private final RateLimiter rateLimiter;
    private final org.springframework.messaging.simp.SimpMessagingTemplate simpMessagingTemplate;
    private final MessageMetrics messageMetrics;

    /**
     * 同步发送消息
     *
     * @param param 发送参数
     * @return 发送结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> send(MessageCreateParam param) {
        log.info("同步发送消息: {}", param);

        // 1. 参数校验
        if (CollUtil.isEmpty(param.getReceivers())) {
            return Result.error("接收人不能为空");
        }
        if (StrUtil.isBlank(param.getTemplateCode())) {
            return Result.error("模板编码不能为空");
        }

        // 2. 查询模板信息
        MessageTemplateDTO template = messageTemplateManager.getByTemplateCode(param.getTemplateCode());
        if (template == null) {
            return Result.error("模板不存在: " + param.getTemplateCode());
        }
        if (Boolean.FALSE.equals(template.getEnabled())) {
            return Result.error("消息模板已禁用: " + param.getTemplateCode());
        }

        // 3. 生成消息ID
        String messageId = generateMessageId();
        MDC.put("messageId", messageId);
        log.info("生成消息ID: {}", messageId);

        // 4. 渲染模板内容
        String messageContent = renderTemplate(template.getContentTemplate(), param.getTemplateParams());
        String messageTitle = StrUtil.isNotBlank(template.getTitleTemplate())
            ? renderTemplate(template.getTitleTemplate(), param.getTemplateParams()) : null;

        // 5. 保存消息记录
        MessageRecord record = new MessageRecord();
        record.setMessageId(messageId);
        record.setTemplateCode(param.getTemplateCode());
        record.setTemplateParams(param.getTemplateParams() != null ? JSONUtil.toJsonStr(param.getTemplateParams()) : null);
        record.setMessageType(template.getTypeCode());
        record.setMessageTitle(messageTitle);
        record.setMessageContent(messageContent);
        record.setSenderId(param.getSenderId());
        record.setSenderName(param.getSenderName());
        record.setSenderSystem(param.getSenderSystem());
        record.setPriority(param.getPriority() != null ? param.getPriority() : 3);
        record.setTotalCount(param.getReceivers().size());
        record.setSuccessCount(0);
        record.setFailCount(0);
        messageRecordManager.save(record);
        log.info("保存消息记录成功: messageId={}", messageId);

        // 6. 确定使用的渠道列表
        List<String> channelCodes = param.getChannelCodes();
        if (CollUtil.isEmpty(channelCodes)) {
            channelCodes = getDefaultChannelCodes(template);
        }
        if (CollUtil.isEmpty(channelCodes)) {
            messageRecordManager.updateStatistics(messageId, 0, param.getReceivers().size());
            return Result.error("未指定发送渠道");
        }

        // 7. 批量保存接收人记录
        List<MessageReceiver> receiverList = new java.util.ArrayList<>();
        for (MessageCreateParam.ReceiverParam receiverParam : param.getReceivers()) {
            for (String channelCode : channelCodes) {
                MessageReceiver receiver = new MessageReceiver();
                receiver.setMessageId(messageId);
                receiver.setReceiverId(receiverParam.getReceiverId());
                receiver.setReceiverType(receiverParam.getReceiverType());
                receiver.setReceiverNumber(receiverParam.getReceiverNumber());
                receiver.setChannelCode(channelCode);
                receiver.setStatus(MessageReceiverStatusEnum.PENDING.getCode());
                receiver.setFailCount(0);
                receiver.setCallbackUrl(param.getCallbackUrl());
                receiverList.add(receiver);
            }
        }
        messageReceiverManager.saveBatch(receiverList);

        // 8. 发送消息
        int totalSuccess = 0;
        int totalFail = 0;
        int receiverIndex = 0;
        for (MessageCreateParam.ReceiverParam receiverParam : param.getReceivers()) {
            for (String channelCode : channelCodes) {
                Long receiverId = receiverList.get(receiverIndex++).getId();

                // 调用渠道发送
                long startTime = System.currentTimeMillis();
                try {
                    messageMetrics.sendStart();
                    // 获取渠道适配器
                    ChannelAdapter adapter = ChannelFactory.getChannel(channelCode);
                    if (adapter == null) {
                        log.warn("渠道适配器不存在: {}", channelCode);
                        long costTime = System.currentTimeMillis() - startTime;
                        updateReceiverStatus(receiverId, MessageReceiverStatusEnum.FAILED, "渠道适配器不存在");
                        saveSendLog(messageId, receiverId, channelCode, null,
                            SendResult.fail("渠道适配器不存在"), costTime);
                        messageMetrics.sendEnd(channelCode, false, costTime);
                        totalFail++;
                        continue;
                    }

                    // 检查渠道是否支持该消息类型
                    if (!adapter.supports(template.getTypeCode())) {
                        log.warn("渠道不支持该消息类型, channel={}, messageType={}", channelCode, template.getTypeCode());
                        long costTime = System.currentTimeMillis() - startTime;
                        updateReceiverStatus(receiverId, MessageReceiverStatusEnum.FAILED, "渠道不支持该消息类型");
                        saveSendLog(messageId, receiverId, channelCode, null,
                            SendResult.fail("渠道不支持该消息类型"), costTime);
                        messageMetrics.sendEnd(channelCode, false, costTime);
                        totalFail++;
                        continue;
                    }

                    // 限流检查
                    if (!rateLimiter.tryAcquire(channelCode)) {
                        log.warn("渠道限流触发: channel={}, messageId={}", channelCode, messageId);
                        long costTime = System.currentTimeMillis() - startTime;
                        updateReceiverStatus(receiverId, MessageReceiverStatusEnum.FAILED, "渠道限流");
                        saveSendLog(messageId, receiverId, channelCode, null,
                            SendResult.fail("CHANNEL_RATE_LIMIT", "渠道限流"), costTime);
                        messageMetrics.sendEnd(channelCode, false, costTime);
                        totalFail++;
                        continue;
                    }

                    // 更新为发送中
                    updateReceiverStatus(receiverId, MessageReceiverStatusEnum.SENDING, null);

                    // 构建消息上下文
                    MessageContext context = buildMessageContext(messageId, channelCode,
                        receiverParam, messageTitle, messageContent,
                        template.getTypeCode(), param.getPriority(), param.getCallbackUrl());

                    // 发送消息
                    String requestParam = JSONUtil.toJsonStr(context);
                    SendResult sendResult = adapter.send(context);
                    long costTime = System.currentTimeMillis() - startTime;

                    saveSendLog(messageId, receiverId, channelCode, requestParam, sendResult, costTime);
                    messageMetrics.sendEnd(channelCode, sendResult.isSuccess(), costTime);

                    if (sendResult.isSuccess()) {
                        updateReceiverStatus(receiverId, MessageReceiverStatusEnum.SENT,
                            sendResult.getChannelMessageId());
                        totalSuccess++;
                        log.info("消息发送成功, messageId={}, channel={}, receiver={}",
                            messageId, channelCode, receiverParam.getReceiverId());
                    } else {
                        updateReceiverStatus(receiverId, MessageReceiverStatusEnum.FAILED,
                            sendResult.getErrorMessage());
                        totalFail++;
                        log.warn("消息发送失败, messageId={}, channel={}, error={}",
                            messageId, channelCode, sendResult.getErrorMessage());
                    }

                } catch (Exception e) {
                    long costTime = System.currentTimeMillis() - startTime;
                    log.error("发送消息异常, messageId={}, channel={}", messageId, channelCode, e);
                    updateReceiverStatus(receiverId, MessageReceiverStatusEnum.FAILED, e.getMessage());
                    saveSendLog(messageId, receiverId, channelCode, null,
                        SendResult.fail(e.getMessage()), costTime);
                    messageMetrics.sendEnd(channelCode, false, costTime);
                    totalFail++;
                }
            }
        }

        // 8. 更新消息记录的统计信息
        messageRecordManager.updateStatistics(messageId, totalSuccess, totalFail);

        log.info("消息发送完成, messageId={}, total={}, success={}, fail={}",
            messageId, param.getReceivers().size(), totalSuccess, totalFail);

        MDC.remove("messageId");
        return Result.success(messageId);
    }

    /**
     * 异步发送消息（立即返回，后台异步执行发送）
     *
     * @param param 发送参数
     * @return 消息ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> sendAsync(MessageCreateParam param) {
        log.info("异步发送消息: {}", param);

        if (CollUtil.isEmpty(param.getReceivers())) {
            return Result.error("接收人不能为空");
        }
        if (StrUtil.isBlank(param.getTemplateCode())) {
            return Result.error("模板编码不能为空");
        }

        // 查询模板
        MessageTemplateDTO template = messageTemplateManager.getByTemplateCode(param.getTemplateCode());
        if (template == null) {
            return Result.error("模板不存在: " + param.getTemplateCode());
        }

        // 生成消息ID
        String messageId = generateMessageId();

        // 渲染模板
        String messageContent = renderTemplate(template.getContentTemplate(), param.getTemplateParams());
        String messageTitle = StrUtil.isNotBlank(template.getTitleTemplate())
            ? renderTemplate(template.getTitleTemplate(), param.getTemplateParams()) : null;

        // 保存消息记录
        MessageRecord record = new MessageRecord();
        record.setMessageId(messageId);
        record.setTemplateCode(param.getTemplateCode());
        record.setTemplateParams(param.getTemplateParams() != null ? JSONUtil.toJsonStr(param.getTemplateParams()) : null);
        record.setMessageType(template.getTypeCode());
        record.setMessageTitle(messageTitle);
        record.setMessageContent(messageContent);
        record.setSenderId(param.getSenderId());
        record.setSenderName(param.getSenderName());
        record.setSenderSystem(param.getSenderSystem());
        record.setPriority(param.getPriority() != null ? param.getPriority() : 3);
        record.setTotalCount(param.getReceivers().size());
        record.setSuccessCount(0);
        record.setFailCount(0);
        messageRecordManager.save(record);

        // 确定渠道列表
        List<String> channelCodes = param.getChannelCodes();
        if (CollUtil.isEmpty(channelCodes)) {
            channelCodes = getDefaultChannelCodes(template);
        }

        // 批量保存接收人记录（状态为 PENDING）
        List<MessageReceiver> receiverList = new java.util.ArrayList<>();
        for (MessageCreateParam.ReceiverParam receiverParam : param.getReceivers()) {
            for (String channelCode : channelCodes) {
                MessageReceiver receiver = new MessageReceiver();
                receiver.setMessageId(messageId);
                receiver.setReceiverId(receiverParam.getReceiverId());
                receiver.setReceiverType(receiverParam.getReceiverType());
                receiver.setReceiverNumber(receiverParam.getReceiverNumber());
                receiver.setChannelCode(channelCode);
                receiver.setStatus(MessageReceiverStatusEnum.PENDING.getCode());
                receiver.setFailCount(0);
                receiver.setCallbackUrl(param.getCallbackUrl());
                receiverList.add(receiver);
            }
        }
        messageReceiverManager.saveBatch(receiverList);

        log.info("消息已加入异步队列: {}", messageId);

        // 通过独立 Bean 异步执行发送（避免 @Async 自调用代理失效）
        messageAsyncExecutor.doSendAsync(messageId, messageContent, messageTitle, template.getTypeCode(), param);

        return Result.success(messageId);
    }

    /**
     * 定时发送消息
     *
     * @param param        发送参数
     * @param scheduleTime 定时时间
     * @return 消息ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> schedule(MessageCreateParam param, LocalDateTime scheduleTime) {
        log.info("定时发送消息, 时间: {}", scheduleTime);

        if (scheduleTime == null || scheduleTime.isBefore(LocalDateTime.now())) {
            return Result.error("定时时间无效，必须是未来时间");
        }
        if (CollUtil.isEmpty(param.getReceivers())) {
            return Result.error("接收人不能为空");
        }
        if (StrUtil.isBlank(param.getTemplateCode())) {
            return Result.error("模板编码不能为空");
        }

        // 查询模板
        MessageTemplateDTO template = messageTemplateManager.getByTemplateCode(param.getTemplateCode());
        if (template == null) {
            return Result.error("模板不存在: " + param.getTemplateCode());
        }

        // 生成消息ID
        String messageId = generateMessageId();

        // 渲染模板
        String messageContent = renderTemplate(template.getContentTemplate(), param.getTemplateParams());
        String messageTitle = StrUtil.isNotBlank(template.getTitleTemplate())
            ? renderTemplate(template.getTitleTemplate(), param.getTemplateParams()) : null;

        // 保存消息记录
        MessageRecord record = new MessageRecord();
        record.setMessageId(messageId);
        record.setTemplateCode(param.getTemplateCode());
        record.setTemplateParams(param.getTemplateParams() != null ? JSONUtil.toJsonStr(param.getTemplateParams()) : null);
        record.setMessageType(template.getTypeCode());
        record.setMessageTitle(messageTitle);
        record.setMessageContent(messageContent);
        record.setSenderId(param.getSenderId());
        record.setSenderName(param.getSenderName());
        record.setSenderSystem(param.getSenderSystem());
        record.setPriority(param.getPriority() != null ? param.getPriority() : 3);
        record.setTotalCount(param.getReceivers().size());
        record.setSuccessCount(0);
        record.setFailCount(0);
        messageRecordManager.save(record);

        // 确定渠道列表
        List<String> channelCodes = param.getChannelCodes();
        if (CollUtil.isEmpty(channelCodes)) {
            channelCodes = getDefaultChannelCodes(template);
        }

        // 批量保存接收人记录（PENDING 状态，设置定时时间）
        List<MessageReceiver> receiverList = new java.util.ArrayList<>();
        for (MessageCreateParam.ReceiverParam receiverParam : param.getReceivers()) {
            for (String channelCode : channelCodes) {
                MessageReceiver receiver = new MessageReceiver();
                receiver.setMessageId(messageId);
                receiver.setReceiverId(receiverParam.getReceiverId());
                receiver.setReceiverType(receiverParam.getReceiverType());
                receiver.setReceiverNumber(receiverParam.getReceiverNumber());
                receiver.setChannelCode(channelCode);
                receiver.setStatus(MessageReceiverStatusEnum.PENDING.getCode());
                receiver.setFailCount(0);
                receiver.setScheduleTime(scheduleTime);
                receiver.setCallbackUrl(param.getCallbackUrl());
                receiverList.add(receiver);
            }
        }
        messageReceiverManager.saveBatch(receiverList);

        log.info("消息已加入定时队列: {}, 发送时间: {}", messageId, scheduleTime);
        return Result.success(messageId);
    }

    /**
     * 撤回消息
     *
     * @param messageId 消息ID
     * @return 是否成功
     */
    public Result<Boolean> revoke(String messageId) {
        log.info("撤回消息: {}", messageId);

        MessageRecord record = messageRecordManager.getByMessageId(messageId);
        if (record == null) {
            return Result.error("消息不存在: " + messageId);
        }

        List<MessageReceiver> receivers = messageReceiverManager.listByMessageId(messageId);
        int revokedCount = 0;
        for (MessageReceiver receiver : receivers) {
            Integer status = receiver.getStatus();
            if (MessageReceiverStatusEnum.PENDING.getCode().equals(status)
                || MessageReceiverStatusEnum.SENDING.getCode().equals(status)) {
                messageReceiverManager.updateStatus(receiver.getId(), MessageReceiverStatusEnum.REVOKED.getCode());
                revokedCount++;
            } else if (MessageReceiverStatusEnum.SENT.getCode().equals(status)
                || MessageReceiverStatusEnum.DELIVERED.getCode().equals(status)) {
                // 对已发送的站内信推送撤回指令
                if (ChannelType.WEBSOCKET.name().equals(receiver.getChannelCode())
                    || "WEBSOCKET".equals(receiver.getChannelCode())) {
                    pushRevokeToUser(receiver.getReceiverId(), messageId);
                }
                messageReceiverManager.updateStatus(receiver.getId(), MessageReceiverStatusEnum.REVOKED.getCode());
                revokedCount++;
            }
        }
        log.info("撤回消息完成, messageId={}, revokedCount={}", messageId, revokedCount);
        return Result.success(true);
    }

    private void pushRevokeToUser(String userId, String messageId) {
        try {
            if (simpMessagingTemplate != null) {
                cn.hutool.json.JSONObject payload = new cn.hutool.json.JSONObject();
                payload.set("type", "REVOKE");
                payload.set("messageId", messageId);
                payload.set("timestamp", System.currentTimeMillis());
                simpMessagingTemplate.convertAndSendToUser(userId, "/queue/notification", payload.toString());
                log.debug("推送撤回指令: userId={}, messageId={}", userId, messageId);
            }
        } catch (Exception e) {
            log.warn("推送撤回指令失败: userId={}, messageId={}", userId, messageId, e);
        }
    }

    /**
     * 渲染模板内容（使用 FreeMarker 引擎）
     *
     * @param templateContent 模板内容
     * @param params          参数
     * @return 渲染后的内容
     */
    private String renderTemplate(String templateContent, Map<String, Object> params) {
        if (StrUtil.isBlank(templateContent)) {
            return templateContent;
        }
        return templateEngine.renderContent(templateContent, params);
    }

    /**
     * 获取默认渠道编码列表（从模板 channelConfig 中解析）
     *
     * @param template 模板
     * @return 渠道编码列表
     */
    private List<String> getDefaultChannelCodes(MessageTemplateDTO template) {
        if (StrUtil.isBlank(template.getChannelConfig())) {
            // 如果模板未配置渠道，则获取所有可用渠道
            return messageChannelManager.getAvailableChannels()
                .stream()
                .map(MessageChannelDTO::getChannelCode)
                .collect(Collectors.toList());
        }
        try {
            JSONObject config = JSONUtil.parseObj(template.getChannelConfig());
            JSONArray channels = config.getJSONArray("defaultChannels");
            if (channels == null || channels.isEmpty()) {
                return Collections.emptyList();
            }
            return channels.toList(String.class);
        } catch (Exception e) {
            log.warn("解析模板渠道配置失败, templateCode={}, config={}",
                template.getTemplateCode(), template.getChannelConfig(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建消息上下文
     */
    private MessageContext buildMessageContext(String messageId,
                                               String channelCode,
                                               MessageCreateParam.ReceiverParam receiver,
                                               String title,
                                               String content,
                                               String messageType,
                                               Integer priority,
                                               String callbackUrl) {
        return MessageContext.builder()
            .messageId(messageId)
            .channelCode(channelCode)
            .receiverId(receiver.getReceiverId())
            .receiverType(receiver.getReceiverType())
            .receiverNumber(receiver.getReceiverNumber())
            .messageTitle(title)
            .messageContent(content)
            .messageType(messageType)
            .priority(priority)
            .callbackUrl(callbackUrl)
            .sendTime(LocalDateTime.now())
            .build();
    }

    /**
     * 更新接收人状态
     */
    private void updateReceiverStatus(Long receiverId,
                                      MessageReceiverStatusEnum status,
                                      String remark) {
        MessageReceiver update = new MessageReceiver();
        update.setId(receiverId);
        update.setStatus(status.getCode());

        if (status == MessageReceiverStatusEnum.SENT || status == MessageReceiverStatusEnum.DELIVERED) {
            update.setChannelMessageId(remark);
            update.setSendTime(LocalDateTime.now());
        } else if (status == MessageReceiverStatusEnum.FAILED) {
            messageReceiverManager.incrementFailCount(receiverId, remark);
            return;
        }
        messageReceiverManager.updateById(update);
    }

    /**
     * 保存发送日志
     */
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

    private String generateMessageId() {
        return "MSG" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
