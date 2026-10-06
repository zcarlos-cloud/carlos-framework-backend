package com.carlos.message.apiimpl;

import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.message.api.MessageSendApi;
import com.carlos.message.pojo.ao.MessageStatusAO;
import com.carlos.message.pojo.ao.SendResultAO;
import com.carlos.message.pojo.param.MessageCreateParam;
import com.carlos.message.pojo.param.MessageSendParam;
import com.carlos.message.service.MessageQueryService;
import com.carlos.message.service.MessageSendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 消息发送服务 API 实现
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Slf4j
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
@Tag(name = "消息发送服务")
public class MessageSendApiImpl implements MessageSendApi {

    private final MessageSendService messageSendService;
    private final MessageQueryService messageQueryService;

    // 服务间调用：审计价值在于「哪个系统替谁发了什么消息」，故与前端入口一样纳入；
    // saveRequestParams = false 同理 —— MessageSendParam 携带 receiverNumber（手机号/邮箱）明细
    @Override
    @Log(title = "消息发送服务", logType = "MSG_API_SEND", businessType = BusinessType.INSERT,
        operation = "'服务间发送消息: 模板=' + #param.templateCode + ', 渠道=' + #param.channelCodes"
            + " + ', 发送方=' + #param.senderSystem",
        targetType = "MSG_MESSAGE", riskLevel = 40, saveRequestParams = false)
    @PostMapping("/send")
    @Operation(summary = "同步发送消息")
    public Result<SendResultAO> send(@RequestBody MessageSendParam param) {
        log.info("发送消息请求: {}", param);
        Result<String> result = messageSendService.send(toCreateParam(param));
        if (!result.isSuccess()) {
            return Result.error(result.getMsg());
        }
        SendResultAO ao = new SendResultAO();
        ao.setMessageId(result.getData());
        ao.setSuccess(true);
        return Result.success(ao);
    }

    @Override
    @Log(title = "消息发送服务", logType = "MSG_API_SEND_ASYNC", businessType = BusinessType.INSERT,
        operation = "'服务间异步发送消息: 模板=' + #param.templateCode + ', 渠道=' + #param.channelCodes"
            + " + ', 发送方=' + #param.senderSystem",
        targetType = "MSG_MESSAGE", riskLevel = 40, saveRequestParams = false)
    @PostMapping("/sendAsync")
    @Operation(summary = "异步发送消息")
    public Result<String> sendAsync(@RequestBody MessageSendParam param) {
        log.info("异步发送消息请求: {}", param);
        return messageSendService.sendAsync(toCreateParam(param));
    }

    // 刻意不加 @Log：sendBatch 是未实现的桩，永远返回「批量发送功能待实现」，无审计价值
    @Override
    @PostMapping("/sendBatch")
    @Operation(summary = "批量发送消息")
    public Result<SendResultAO> sendBatch(@RequestBody List<MessageSendParam> params) {
        log.info("批量发送消息请求, 数量: {}", params.size());
        return Result.error("批量发送功能待实现");
    }

    @Override
    @Log(title = "消息发送服务", logType = "MSG_API_SCHEDULE", businessType = BusinessType.INSERT,
        operation = "'服务间定时发送消息: 模板=' + #param.templateCode + ', 渠道=' + #param.channelCodes"
            + " + ', 发送方=' + #param.senderSystem + ', 计划时间=' + #scheduleTime",
        targetType = "MSG_MESSAGE", riskLevel = 40, saveRequestParams = false)
    @PostMapping("/schedule")
    @Operation(summary = "定时发送消息")
    public Result<String> schedule(
        @RequestBody MessageSendParam param,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduleTime) {
        log.info("定时发送消息请求, 时间: {}", scheduleTime);
        return messageSendService.schedule(toCreateParam(param), scheduleTime);
    }

    /**
     * MessageSendParam → MessageCreateParam 转换。
     * receivers 两个类是同名不同型的内嵌类（Spring 6 BeanUtils 按泛型判定不可赋值会静默跳过），
     * 必须排除后逐个显式映射，否则下游校验恒报「接收人不能为空」
     */
    private MessageCreateParam toCreateParam(MessageSendParam param) {
        MessageCreateParam createParam = new MessageCreateParam();
        BeanUtils.copyProperties(param, createParam, "receivers");
        if (param.getReceivers() != null) {
            createParam.setReceivers(param.getReceivers().stream().map(source -> {
                MessageCreateParam.ReceiverParam receiver = new MessageCreateParam.ReceiverParam();
                BeanUtils.copyProperties(source, receiver);
                return receiver;
            }).toList());
        }
        return createParam;
    }

    @Override
    @Log(title = "消息发送服务", logType = "MSG_API_REVOKE", businessType = BusinessType.UPDATE_STATUS,
        operation = "'服务间撤回消息, messageId: ' + #messageId", targetId = "#messageId",
        targetType = "MSG_MESSAGE", riskLevel = 40)
    @PostMapping("/revoke")
    @Operation(summary = "撤回消息")
    public Result<Boolean> revoke(@RequestParam("messageId") String messageId) {
        log.info("撤回消息请求, messageId: {}", messageId);
        return messageSendService.revoke(messageId);
    }

    @Override
    @GetMapping("/status")
    @Operation(summary = "查询消息状态")
    public Result<MessageStatusAO> queryStatus(@RequestParam("messageId") String messageId) {
        log.info("查询消息状态请求, messageId: {}", messageId);
        Result<Integer> statusResult = messageQueryService.queryStatus(messageId);
        if (!statusResult.isSuccess()) {
            return Result.error(statusResult.getMsg());
        }
        MessageStatusAO ao = new MessageStatusAO();
        ao.setMessageId(messageId);
        ao.setStatus(statusResult.getData());
        return Result.success(ao);
    }
}
