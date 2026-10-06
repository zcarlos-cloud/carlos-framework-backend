package com.carlos.audit.controller;

import com.carlos.audit.convert.AuditLogArchiveRecordConvert;
import com.carlos.audit.manager.AuditLogArchiveRecordManager;
import com.carlos.audit.pojo.dto.AuditLogArchiveRecordDTO;
import com.carlos.audit.pojo.param.AuditLogArchiveRecordCreateParam;
import com.carlos.audit.pojo.param.AuditLogArchiveRecordPageParam;
import com.carlos.audit.pojo.param.AuditLogArchiveRecordUpdateParam;
import com.carlos.audit.pojo.vo.AuditLogArchiveRecordVO;
import com.carlos.audit.service.AuditLogArchiveRecordService;
import com.carlos.audit.service.AuditLogArchiveService;
import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 审计日志归档记录（管理冷数据归档） rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/audit/log/archive/record")
@Tag(name = "审计日志-归档记录")
public class AuditLogArchiveRecordController {

    public static final String BASE_NAME = "归档记录";

    private final AuditLogArchiveRecordService logArchiveRecordService;

    private final AuditLogArchiveRecordManager logArchiveRecordManager;

    private final AuditLogArchiveService logArchiveService;


    @PreAuthorize("hasPermission('audit:archive', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated AuditLogArchiveRecordCreateParam param) {
        AuditLogArchiveRecordDTO dto = AuditLogArchiveRecordConvert.INSTANCE.toDTO(param);
        logArchiveRecordService.addAuditLogArchiveRecord(dto);
    }


    @PreAuthorize("hasPermission('audit:archive', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody ParamIdSet<Long> param) {
        logArchiveRecordService.deleteAuditLogArchiveRecord(param.getIds());
    }


    @PreAuthorize("hasPermission('audit:archive', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated AuditLogArchiveRecordUpdateParam param) {
        AuditLogArchiveRecordDTO dto = AuditLogArchiveRecordConvert.INSTANCE.toDTO(param);
        logArchiveRecordService.updateAuditLogArchiveRecord(dto);
    }


    @PreAuthorize("hasPermission('audit:archive', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public AuditLogArchiveRecordVO detail(@RequestParam Long id) {
        return AuditLogArchiveRecordConvert.INSTANCE.toVO(logArchiveRecordManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('audit:archive', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<AuditLogArchiveRecordVO> page(AuditLogArchiveRecordPageParam param) {
        return logArchiveRecordManager.getPage(param);
    }

    @PreAuthorize("hasPermission('audit:archive', 'restore')")
    @PostMapping("restore")
    @Operation(summary = "恢复归档数据至 ClickHouse")
    public void restore(@RequestParam String archiveId) {
        logArchiveService.restoreArchive(archiveId);
    }
}
