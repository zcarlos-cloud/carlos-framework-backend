package com.carlos.message.controller;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.message.convert.MessageTypeConvert;
import com.carlos.message.enums.MessageErrorCode;
import com.carlos.message.manager.MessageTypeManager;
import com.carlos.message.pojo.dto.MessageTypeDTO;
import com.carlos.message.pojo.param.MessageTypeCreateParam;
import com.carlos.message.pojo.param.MessageTypePageParam;
import com.carlos.message.pojo.param.MessageTypeUpdateParam;
import com.carlos.message.pojo.vo.MessageTypeVO;
import com.carlos.message.service.MessageTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 消息类型 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("message/type")
@Tag(name = "消息类型")
public class MessageTypeController {

    public static final String BASE_NAME = "消息类型";

    private final MessageTypeService typeService;

    private final MessageTypeManager typeManager;


    @Log(title = "消息类型", logType = "MSG_TYPE_CREATE", businessType = BusinessType.INSERT,
        operation = "'新增消息类型: ' + #param.typeCode", targetType = "MSG_TYPE", riskLevel = 30)
    @PreAuthorize("hasPermission('message:type', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated MessageTypeCreateParam param) {
        MessageTypeDTO dto = MessageTypeConvert.INSTANCE.toDTO(param);
        typeService.addMessageType(dto);
    }


    @Log(title = "消息类型", logType = "MSG_TYPE_DELETE", businessType = BusinessType.DELETE,
        operation = "'删除消息类型: ' + #param.ids", targetId = "#param.ids",
        targetType = "MSG_TYPE", riskLevel = 50)
    @PreAuthorize("hasPermission('message:type', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody ParamIdSet<Long> param) {
        typeService.deleteMessageType(param.getIds());
    }


    @Log(title = "消息类型", logType = "MSG_TYPE_UPDATE", businessType = BusinessType.UPDATE,
        operation = "'更新消息类型: ' + #param.typeCode", targetId = "#param.id",
        targetType = "MSG_TYPE", riskLevel = 30)
    @PreAuthorize("hasPermission('message:type', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated MessageTypeUpdateParam param) {
        MessageTypeDTO dto = MessageTypeConvert.INSTANCE.toDTO(param);
        typeService.updateMessageType(dto);
    }


    @PreAuthorize("hasPermission('message:type', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public MessageTypeVO detail(Long id) {
        return MessageTypeConvert.INSTANCE.toVO(typeManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('message:type', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<MessageTypeVO> page(MessageTypePageParam param) {
        return typeManager.getPage(param);
    }

    @PreAuthorize("hasPermission('message:type', 'view')")
    @GetMapping("getByCode")
    @Operation(summary = "根据编码查询" + BASE_NAME)
    @Parameter(name = "typeCode", description = "类型编码", required = true)
    public MessageTypeVO getByCode(@RequestParam String typeCode) {
        if (StrUtil.isBlank(typeCode)) {
            throw MessageErrorCode.MSG_PARAM_TEMPLATE_CODE_EMPTY.exception();
        }
        MessageTypeDTO dto = typeService.getByTypeCode(typeCode);
        return MessageTypeConvert.INSTANCE.toVO(dto);
    }

    @Log(title = "消息类型", logType = "MSG_TYPE_ENABLE", businessType = BusinessType.UPDATE_STATUS,
        operation = "'启用消息类型, id: ' + #id", targetId = "#id", targetType = "MSG_TYPE", riskLevel = 40)
    @PreAuthorize("hasPermission('message:type', 'enable')")
    @PostMapping("enable")
    @Operation(summary = "启用" + BASE_NAME)
    public void enable(@RequestParam Long id) {
        typeService.enableType(id);
    }

    @Log(title = "消息类型", logType = "MSG_TYPE_DISABLE", businessType = BusinessType.UPDATE_STATUS,
        operation = "'禁用消息类型, id: ' + #id", targetId = "#id", targetType = "MSG_TYPE", riskLevel = 40)
    @PreAuthorize("hasPermission('message:type', 'disable')")
    @PostMapping("disable")
    @Operation(summary = "禁用" + BASE_NAME)
    public void disable(@RequestParam Long id) {
        typeService.disableType(id);
    }
}
