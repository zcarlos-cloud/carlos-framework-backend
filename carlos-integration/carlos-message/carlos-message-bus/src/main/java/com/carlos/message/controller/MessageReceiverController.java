package com.carlos.message.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.message.convert.MessageReceiverConvert;
import com.carlos.message.manager.MessageReceiverManager;
import com.carlos.message.pojo.dto.MessageReceiverDTO;
import com.carlos.message.pojo.param.MessageReceiverCreateParam;
import com.carlos.message.pojo.param.MessageReceiverPageParam;
import com.carlos.message.pojo.param.MessageReceiverUpdateParam;
import com.carlos.message.pojo.vo.MessageReceiverVO;
import com.carlos.message.service.MessageReceiverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 消息接收人表 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("message/receiver")
@Tag(name = "消息接收人表")
public class MessageReceiverController {

    public static final String BASE_NAME = "消息接收人表";

    private final MessageReceiverService receiverService;

    private final MessageReceiverManager receiverManager;


    @PreAuthorize("hasPermission('message:receiver', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated MessageReceiverCreateParam param) {
        MessageReceiverDTO dto = MessageReceiverConvert.INSTANCE.toDTO(param);
        receiverService.addMessageReceiver(dto);
    }


    @PreAuthorize("hasPermission('message:receiver', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody ParamIdSet<Long> param) {
        receiverService.deleteMessageReceiver(param.getIds());
    }


    @PreAuthorize("hasPermission('message:receiver', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated MessageReceiverUpdateParam param) {
        MessageReceiverDTO dto = MessageReceiverConvert.INSTANCE.toDTO(param);
        receiverService.updateMessageReceiver(dto);
    }


    @PreAuthorize("hasPermission('message:receiver', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public MessageReceiverVO detail(Long id) {
        return MessageReceiverConvert.INSTANCE.toVO(receiverManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('message:receiver', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<MessageReceiverVO> page(MessageReceiverPageParam param) {
        return receiverManager.getPage(param);
    }
}
