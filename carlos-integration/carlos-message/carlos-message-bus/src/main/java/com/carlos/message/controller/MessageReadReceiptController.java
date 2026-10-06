package com.carlos.message.controller;

import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.message.pojo.param.MessageReadParam;
import com.carlos.message.service.MessageReadReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 消息已读回执接口
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@RestController
@RequestMapping("/message/read")
@RequiredArgsConstructor
@Tag(name = "消息已读回执")
public class MessageReadReceiptController {

    private final MessageReadReceiptService messageReadReceiptService;

    @Log(title = "消息已读", logType = "MSG_READ", businessType = BusinessType.UPDATE_STATUS,
        operation = "'标记消息已读, 接收人: ' + #param.receiverId", targetType = "MSG_MESSAGE", riskLevel = 20)
    @PreAuthorize("hasPermission('message:read-receipt', 'markRead')")
    @PostMapping
    @Operation(summary = "标记消息已读")
    public Result<Boolean> markRead(@Valid @RequestBody MessageReadParam param) {
        if (param.getMessageId() != null) {
            boolean success = messageReadReceiptService.markRead(param.getMessageId(), param.getReceiverId());
            return success ? Result.success(true) : Result.error("标记已读失败");
        }
        if (param.getMessageIds() != null && !param.getMessageIds().isEmpty()) {
            int count = messageReadReceiptService.markReadBatch(param.getMessageIds(), param.getReceiverId());
            return Result.success(count > 0);
        }
        return Result.error("消息ID不能为空");
    }
}
