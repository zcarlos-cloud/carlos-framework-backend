package com.carlos.message.controller;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.message.convert.MessageTemplateConvert;
import com.carlos.message.enums.MessageErrorCode;
import com.carlos.message.manager.MessageTemplateManager;
import com.carlos.message.pojo.dto.MessageTemplateDTO;
import com.carlos.message.pojo.param.MessageTemplateCreateParam;
import com.carlos.message.pojo.param.MessageTemplatePageParam;
import com.carlos.message.pojo.param.MessageTemplateUpdateParam;
import com.carlos.message.pojo.vo.MessageTemplateVO;
import com.carlos.message.service.MessageTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * <p>
 * 消息模板 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("message/template")
@Tag(name = "消息模板")
@Slf4j
public class MessageTemplateController {

    public static final String BASE_NAME = "消息模板";

    private final MessageTemplateService templateService;

    private final MessageTemplateManager templateManager;


    @Log(title = "消息模板", logType = "MSG_TEMPLATE_CREATE", businessType = BusinessType.INSERT,
        operation = "'新增模板: ' + #param.templateCode", targetType = "MSG_TEMPLATE", riskLevel = 30)
    @PreAuthorize("hasPermission('message:template', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated MessageTemplateCreateParam param) {
        MessageTemplateDTO dto = MessageTemplateConvert.INSTANCE.toDTO(param);
        templateService.addMessageTemplate(dto);
    }


    @Log(title = "消息模板", logType = "MSG_TEMPLATE_DELETE", businessType = BusinessType.DELETE,
        operation = "'删除模板: ' + #param.ids", targetId = "#param.ids",
        targetType = "MSG_TEMPLATE", riskLevel = 50)
    @PreAuthorize("hasPermission('message:template', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody ParamIdSet<Long> param) {
        templateService.deleteMessageTemplate(param.getIds());
    }


    @Log(title = "消息模板", logType = "MSG_TEMPLATE_UPDATE", businessType = BusinessType.UPDATE,
        operation = "'更新模板: ' + #param.templateCode", targetId = "#param.id",
        targetType = "MSG_TEMPLATE", riskLevel = 30)
    @PreAuthorize("hasPermission('message:template', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated MessageTemplateUpdateParam param) {
        MessageTemplateDTO dto = MessageTemplateConvert.INSTANCE.toDTO(param);
        templateService.updateMessageTemplate(dto);
    }


    @PreAuthorize("hasPermission('message:template', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public MessageTemplateVO detail(Long id) {
        return MessageTemplateConvert.INSTANCE.toVO(templateManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('message:template', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<MessageTemplateVO> page(MessageTemplatePageParam param) {
        return templateManager.getPage(param);
    }

    @PreAuthorize("hasPermission('message:template', 'view')")
    @GetMapping("getByCode")
    @Operation(summary = "根据编码查询" + BASE_NAME)
    @Parameter(name = "templateCode", description = "模板编码", required = true)
    public MessageTemplateVO getByCode(@RequestParam String templateCode) {
        if (StrUtil.isBlank(templateCode)) {
            throw MessageErrorCode.MSG_PARAM_TEMPLATE_CODE_EMPTY.exception();
        }
        MessageTemplateDTO dto = templateService.getByTemplateCode(templateCode);
        return MessageTemplateConvert.INSTANCE.toVO(dto);
    }

    @Log(title = "消息模板", logType = "MSG_TEMPLATE_PUBLISH", businessType = BusinessType.UPDATE_STATUS,
        operation = "'发布模板, id: ' + #id", targetId = "#id", targetType = "MSG_TEMPLATE", riskLevel = 40)
    @PreAuthorize("hasPermission('message:template', 'publish')")
    @PostMapping("publish")
    @Operation(summary = "发布" + BASE_NAME)
    public void publish(@RequestParam Long id) {
        templateService.publishTemplate(id);
    }

    @Log(title = "消息模板", logType = "MSG_TEMPLATE_ENABLE", businessType = BusinessType.UPDATE_STATUS,
        operation = "'启用模板, id: ' + #id", targetId = "#id", targetType = "MSG_TEMPLATE", riskLevel = 40)
    @PreAuthorize("hasPermission('message:template', 'enable')")
    @PostMapping("enable")
    @Operation(summary = "启用" + BASE_NAME)
    public void enable(@RequestParam Long id) {
        templateService.enableTemplate(id);
    }

    @Log(title = "消息模板", logType = "MSG_TEMPLATE_DISABLE", businessType = BusinessType.UPDATE_STATUS,
        operation = "'禁用模板, id: ' + #id", targetId = "#id", targetType = "MSG_TEMPLATE", riskLevel = 40)
    @PreAuthorize("hasPermission('message:template', 'disable')")
    @PostMapping("disable")
    @Operation(summary = "禁用" + BASE_NAME)
    public void disable(@RequestParam Long id) {
        templateService.disableTemplate(id);
    }

    // 刻意不加 @Log：validate 是纯校验，不改变任何状态，无审计价值（见 MsgAuditAnnotationGuardTest 的 EXEMPT）
    @PreAuthorize("hasPermission('message:template', 'validate')")
    @PostMapping("validate")
    @Operation(summary = "验证模板参数")
    public void validate(@RequestBody Map<String, Object> request) {
        String templateCode = (String) request.get("templateCode");
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        if (StrUtil.isBlank(templateCode)) {
            throw MessageErrorCode.MSG_PARAM_TEMPLATE_CODE_EMPTY.exception();
        }
        templateService.validateTemplateParams(templateCode, params);
    }
}
