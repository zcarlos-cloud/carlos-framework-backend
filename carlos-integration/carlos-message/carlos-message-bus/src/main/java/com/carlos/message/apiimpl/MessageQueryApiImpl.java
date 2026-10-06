package com.carlos.message.apiimpl;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.message.api.MessageQueryApi;
import com.carlos.message.pojo.ao.MessageReceiverAO;
import com.carlos.message.pojo.ao.MessageRecordAO;
import com.carlos.message.pojo.dto.MessageReceiverDTO;
import com.carlos.message.pojo.dto.MessageRecordDTO;
import com.carlos.message.pojo.param.MessagePageParam;
import com.carlos.message.service.MessageQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 消息查询服务 API 实现
 * </p>
 *
 * @author Carlos
 * @date 2026/3/12
 */
@Slf4j
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
@Tag(name = "消息查询服务")
public class MessageQueryApiImpl implements MessageQueryApi {

    private final MessageQueryService messageQueryService;

    @Override
    @GetMapping("/detail")
    @Operation(summary = "查询消息记录详情")
    public Result<MessageRecordAO> getById(@RequestParam("messageId") String messageId) {
        log.info("查询消息详情: {}", messageId);
        Result<MessageRecordDTO> result = messageQueryService.getById(messageId);
        if (!result.isSuccess()) {
            return Result.error(result.getMsg());
        }
        return Result.success(toAO(result.getData()));
    }

    @Override
    @PostMapping("/page")
    @Operation(summary = "分页查询消息记录")
    public Result<Paging<MessageRecordAO>> page(@RequestBody MessagePageParam param) {
        log.info("分页查询消息记录: {}", param);
        Result<Paging<MessageRecordDTO>> result = messageQueryService.page(param);
        if (!result.isSuccess()) {
            return Result.error(result.getMsg());
        }
        Paging<MessageRecordDTO> dtoPage = result.getData();
        Paging<MessageRecordAO> aoPage = new Paging<>();
        aoPage.setTotal(dtoPage.getTotal());
        aoPage.setCurrent(dtoPage.getCurrent());
        aoPage.setSize(dtoPage.getSize());
        aoPage.setPages(dtoPage.getPages());
        List<MessageRecordAO> aoList = dtoPage.getRecords() == null ? new ArrayList<>()
            : dtoPage.getRecords().stream().map(this::toAO).collect(Collectors.toList());
        aoPage.setRecords(aoList);
        return Result.success(aoPage);
    }

    @Override
    @GetMapping("/receivers")
    @Operation(summary = "查询消息接收人列表")
    public Result<List<MessageReceiverAO>> getReceivers(@RequestParam("messageId") String messageId) {
        log.info("查询消息接收人列表: {}", messageId);
        Result<List<MessageReceiverDTO>> result = messageQueryService.getReceivers(messageId);
        if (!result.isSuccess()) {
            return Result.error(result.getMsg());
        }
        List<MessageReceiverAO> aoList = result.getData() == null ? new ArrayList<>()
            : result.getData().stream().map(this::toReceiverAO).collect(Collectors.toList());
        return Result.success(aoList);
    }

    @Override
    @GetMapping("/unread")
    @Operation(summary = "查询用户未读消息")
    public Result<List<MessageRecordAO>> getUnread(@RequestParam("userId") String userId) {
        log.info("查询用户未读消息: {}", userId);
        Result<List<MessageRecordDTO>> result = messageQueryService.getUnread(userId);
        if (!result.isSuccess()) {
            return Result.error(result.getMsg());
        }
        List<MessageRecordAO> aoList = result.getData() == null ? new ArrayList<>()
            : result.getData().stream().map(this::toAO).collect(Collectors.toList());
        return Result.success(aoList);
    }

    /**
     * MessageRecordDTO → MessageRecordAO
     */
    private MessageRecordAO toAO(MessageRecordDTO dto) {
        if (dto == null) {
            return null;
        }
        MessageRecordAO ao = new MessageRecordAO();
        ao.setMessageId(dto.getMessageId());
        ao.setTemplateCode(dto.getTemplateCode());
        ao.setMessageType(dto.getMessageType());
        ao.setMessageTitle(dto.getMessageTitle());
        ao.setMessageContent(dto.getMessageContent());
        ao.setSenderId(dto.getSenderId());
        ao.setSenderName(dto.getSenderName());
        ao.setSenderSystem(dto.getSenderSystem());
        ao.setPriority(dto.getPriority());
        ao.setTotalCount(dto.getTotalCount());
        ao.setSuccessCount(dto.getSuccessCount());
        ao.setFailCount(dto.getFailCount());
        ao.setCreateTime(dto.getCreateTime());
        return ao;
    }

    /**
     * MessageReceiverDTO → MessageReceiverAO
     */
    private MessageReceiverAO toReceiverAO(MessageReceiverDTO dto) {
        if (dto == null) {
            return null;
        }
        MessageReceiverAO ao = new MessageReceiverAO();
        ao.setMessageId(dto.getMessageId());
        ao.setChannelCode(dto.getChannelCode());
        ao.setReceiverId(dto.getReceiverId());
        ao.setReceiverType(dto.getReceiverType());
        ao.setReceiverNumber(dto.getReceiverNumber());
        ao.setStatus(dto.getStatus());
        ao.setFailCount(dto.getFailCount());
        ao.setFailReason(dto.getFailReason());
        ao.setSendTime(dto.getSendTime());
        ao.setDeliverTime(dto.getDeliverTime());
        ao.setReadTime(dto.getReadTime());
        ao.setCreateTime(dto.getCreateTime());
        return ao;
    }
}
