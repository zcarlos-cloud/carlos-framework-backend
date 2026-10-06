package com.carlos.message.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.message.convert.MessageConvert;
import com.carlos.message.pojo.dto.MessageRecordDTO;
import com.carlos.message.pojo.param.MessageCreateParam;
import com.carlos.message.pojo.param.MessagePageParam;
import com.carlos.message.pojo.vo.MessageRecordVO;
import com.carlos.message.service.MessageQueryService;
import com.carlos.message.service.MessageSendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * <p>
 * 消息管理控制器
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Slf4j
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
@Tag(name = "消息管理")
public class MessageController {

    private final MessageSendService messageSendService;
    private final MessageQueryService messageQueryService;
    private final MessageConvert messageConvert;

    // saveRequestParams = false：入参含完整消息体与接收人明细（receiverNumber 为手机号/邮箱），
    // 审计只留「谁、什么模板、什么渠道」，收件人名单不属于审计所需
    @Log(title = "消息管理", logType = "MSG_SEND", businessType = BusinessType.INSERT,
        operation = "'发送消息: 模板=' + #param.templateCode + ', 渠道=' + #param.channelCodes",
        targetType = "MSG_MESSAGE", riskLevel = 40, saveRequestParams = false)
    @PreAuthorize("hasPermission('message:info', 'send')")
    @PostMapping("/send")
    @Operation(summary = "同步发送消息")
    public Result<String> send(@RequestBody @Valid MessageCreateParam param) {
        return messageSendService.send(param);
    }

    @Log(title = "消息管理", logType = "MSG_SEND_ASYNC", businessType = BusinessType.INSERT,
        operation = "'异步发送消息: 模板=' + #param.templateCode + ', 渠道=' + #param.channelCodes",
        targetType = "MSG_MESSAGE", riskLevel = 40, saveRequestParams = false)
    @PreAuthorize("hasPermission('message:info', 'send')")
    @PostMapping("/sendAsync")
    @Operation(summary = "异步发送消息")
    public Result<String> sendAsync(@RequestBody @Valid MessageCreateParam param) {
        return messageSendService.sendAsync(param);
    }

    @Log(title = "消息管理", logType = "MSG_SCHEDULE", businessType = BusinessType.INSERT,
        operation = "'定时发送消息: 模板=' + #param.templateCode + ', 渠道=' + #param.channelCodes"
            + " + ', 计划时间=' + #scheduleTime",
        targetType = "MSG_MESSAGE", riskLevel = 40, saveRequestParams = false)
    @PreAuthorize("hasPermission('message:info', 'schedule')")
    @PostMapping("/schedule")
    @Operation(summary = "定时发送消息")
    public Result<String> schedule(
        @RequestBody @Valid MessageCreateParam param,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduleTime) {
        return messageSendService.schedule(param, scheduleTime);
    }

    @Log(title = "消息管理", logType = "MSG_REVOKE", businessType = BusinessType.UPDATE_STATUS,
        operation = "'撤回消息, messageId: ' + #messageId", targetId = "#messageId",
        targetType = "MSG_MESSAGE", riskLevel = 40)
    @PreAuthorize("hasPermission('message:info', 'revoke')")
    @PostMapping("/revoke")
    @Operation(summary = "撤回消息")
    public Result<Boolean> revoke(@RequestParam("messageId") String messageId) {
        return messageSendService.revoke(messageId);
    }

    @PreAuthorize("hasPermission('message:info', 'view')")
    @GetMapping("/detail")
    @Operation(summary = "查询消息详情")
    public Result<MessageRecordVO> getById(@RequestParam("messageId") String messageId) {
        Result<MessageRecordDTO> result = messageQueryService.getById(messageId);
        if (!result.isSuccess()) {
            return Result.error(result.getMsg());
        }
        return Result.success(messageConvert.toVO(result.getData()));
    }

    @PreAuthorize("hasPermission('message:info', 'view')")
    @GetMapping("/page")
    @Operation(summary = "分页查询消息记录")
    public Result<Paging<MessageRecordVO>> page(
        @RequestParam MessagePageParam param) {

        Result<Paging<MessageRecordDTO>> result = messageQueryService.page(param);
        if (!result.isSuccess()) {
            return Result.error(result.getMsg());
        }
        Paging<MessageRecordVO> voPaging = new Paging<>();
        voPaging.setRecords(messageConvert.toVOList(result.getData().getRecords()));
        voPaging.setTotal(result.getData().getTotal());
        return Result.success(voPaging);
    }
}
