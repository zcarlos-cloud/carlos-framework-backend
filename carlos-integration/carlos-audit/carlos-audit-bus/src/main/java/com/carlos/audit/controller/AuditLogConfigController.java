package com.carlos.audit.controller;

import com.carlos.audit.convert.AuditLogConfigConvert;
import com.carlos.audit.manager.AuditLogConfigManager;
import com.carlos.audit.pojo.dto.AuditLogConfigDTO;
import com.carlos.audit.pojo.param.AuditLogConfigCreateParam;
import com.carlos.audit.pojo.param.AuditLogConfigPageParam;
import com.carlos.audit.pojo.param.AuditLogConfigUpdateParam;
import com.carlos.audit.pojo.vo.AuditLogConfigVO;
import com.carlos.audit.service.AuditLogConfigService;
import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 审计日志配置（动态TTL与采样策略） rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/audit/log/config")
@Tag(name = "审计日志-配置")
public class AuditLogConfigController {

    public static final String BASE_NAME = "配置";

    private final AuditLogConfigService logConfigService;

    private final AuditLogConfigManager logConfigManager;


    @PreAuthorize("hasPermission('audit:config', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated AuditLogConfigCreateParam param) {
        AuditLogConfigDTO dto = AuditLogConfigConvert.INSTANCE.toDTO(param);
        logConfigService.addAuditLogConfig(dto);
    }


    @PreAuthorize("hasPermission('audit:config', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody ParamIdSet<Long> param) {
        logConfigService.deleteAuditLogConfig(param.getIds());
    }


    @PreAuthorize("hasPermission('audit:config', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated AuditLogConfigUpdateParam param) {
        AuditLogConfigDTO dto = AuditLogConfigConvert.INSTANCE.toDTO(param);
        logConfigService.updateAuditLogConfig(dto);
    }


    @PreAuthorize("hasPermission('audit:config', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public AuditLogConfigVO detail(Long id) {
        return AuditLogConfigConvert.INSTANCE.toVO(logConfigManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('audit:config', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<AuditLogConfigVO> page(@Parameter(description = "分页参数") AuditLogConfigPageParam param) {
        return logConfigManager.getPage(param);
    }
}
