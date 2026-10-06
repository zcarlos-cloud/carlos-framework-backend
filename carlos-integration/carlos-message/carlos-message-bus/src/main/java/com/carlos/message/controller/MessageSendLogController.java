package com.carlos.message.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.message.convert.MessageSendLogConvert;
import com.carlos.message.manager.MessageSendLogManager;
import com.carlos.message.pojo.dto.MessageSendLogDTO;
import com.carlos.message.pojo.param.MessageSendLogCreateParam;
import com.carlos.message.pojo.param.MessageSendLogPageParam;
import com.carlos.message.pojo.param.MessageSendLogUpdateParam;
import com.carlos.message.pojo.vo.MessageSendLogVO;
import com.carlos.message.service.MessageSendLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 消息发送日志 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("message/send/log")
@Tag(name = "消息发送日志")
public class MessageSendLogController {

    public static final String BASE_NAME = "消息发送日志";

    private final MessageSendLogService sendLogService;

    private final MessageSendLogManager sendLogManager;


    @PreAuthorize("hasPermission('message:send-log', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated MessageSendLogCreateParam param) {
        MessageSendLogDTO dto = MessageSendLogConvert.INSTANCE.toDTO(param);
        sendLogService.addMessageSendLog(dto);
    }


    @PreAuthorize("hasPermission('message:send-log', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody ParamIdSet<Long> param) {
        sendLogService.deleteMessageSendLog(param.getIds());
    }


    @PreAuthorize("hasPermission('message:send-log', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated MessageSendLogUpdateParam param) {
        MessageSendLogDTO dto = MessageSendLogConvert.INSTANCE.toDTO(param);
        sendLogService.updateMessageSendLog(dto);
    }


    @PreAuthorize("hasPermission('message:send-log', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public MessageSendLogVO detail(Long id) {
        return MessageSendLogConvert.INSTANCE.toVO(sendLogManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('message:send-log', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<MessageSendLogVO> page(MessageSendLogPageParam param) {
        return sendLogManager.getPage(param);
    }
}
