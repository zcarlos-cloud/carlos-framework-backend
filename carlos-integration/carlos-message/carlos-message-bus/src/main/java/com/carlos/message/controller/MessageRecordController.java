package com.carlos.message.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.message.convert.MessageRecordConvert;
import com.carlos.message.manager.MessageRecordManager;
import com.carlos.message.pojo.dto.MessageRecordDTO;
import com.carlos.message.pojo.param.MessageRecordCreateParam;
import com.carlos.message.pojo.param.MessageRecordPageParam;
import com.carlos.message.pojo.param.MessageRecordUpdateParam;
import com.carlos.message.pojo.vo.MessageRecordVO;
import com.carlos.message.service.MessageRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 消息记录表 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("message/record")
@Tag(name = "消息记录表")
public class MessageRecordController {

    public static final String BASE_NAME = "消息记录表";

    private final MessageRecordService recordService;

    private final MessageRecordManager recordManager;


    @PreAuthorize("hasPermission('message:record', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated MessageRecordCreateParam param) {
        MessageRecordDTO dto = MessageRecordConvert.INSTANCE.toDTO(param);
        recordService.addMessageRecord(dto);
    }


    @PreAuthorize("hasPermission('message:record', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody ParamIdSet<Long> param) {
        recordService.deleteMessageRecord(param.getIds());
    }


    @PreAuthorize("hasPermission('message:record', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated MessageRecordUpdateParam param) {
        MessageRecordDTO dto = MessageRecordConvert.INSTANCE.toDTO(param);
        recordService.updateMessageRecord(dto);
    }


    @PreAuthorize("hasPermission('message:record', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public MessageRecordVO detail(Long id) {
        return MessageRecordConvert.INSTANCE.toVO(recordManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('message:record', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<MessageRecordVO> page(MessageRecordPageParam param) {
        return recordManager.getPage(param);
    }
}
