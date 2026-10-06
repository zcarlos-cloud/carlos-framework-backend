package com.carlos.audit.apiimpl;

import com.carlos.audit.api.ApiAuditLogArchive;
import com.carlos.audit.api.pojo.ao.AuditLogArchiveRecordAO;
import com.carlos.audit.api.pojo.param.ApiAuditLogArchiveParam;
import com.carlos.audit.convert.AuditLogArchiveRecordConvert;
import com.carlos.audit.manager.AuditLogArchiveRecordManager;
import com.carlos.audit.pojo.dto.AuditLogArchiveRecordDTO;
import com.carlos.audit.pojo.param.AuditLogArchiveRecordPageParam;
import com.carlos.audit.pojo.vo.AuditLogArchiveRecordVO;
import com.carlos.audit.service.AuditLogArchiveRecordService;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审计日志归档记录 Feign 接口实现
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit/log/archive/record")
@Tag(name = "审计日志归档记录Feign接口", description = "供其他微服务管理审计日志归档记录")
public class ApiAuditLogArchiveImpl implements ApiAuditLogArchive {

    private final AuditLogArchiveRecordService logArchiveRecordService;
    private final AuditLogArchiveRecordManager logArchiveRecordManager;

    @Override
    @PostMapping("/add")
    @Operation(summary = "新增归档记录")
    public Result<Void> addAuditLogArchiveRecord(@RequestBody @Valid ApiAuditLogArchiveParam param) {
        try {
            AuditLogArchiveRecordDTO dto = AuditLogArchiveRecordConvert.INSTANCE.toDTO(param);
            logArchiveRecordService.addAuditLogArchiveRecord(dto);
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("新增归档记录失败", e);
            return Result.error("新增归档记录失败");
        }
    }

    @Override
    @PostMapping("/delete")
    @Operation(summary = "删除归档记录")
    public Result<Void> deleteAuditLogArchiveRecord(@RequestParam("id") Long id) {
        try {
            logArchiveRecordService.deleteAuditLogArchiveRecord(java.util.Set.of(id));
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除归档记录失败", e);
            return Result.error("删除归档记录失败");
        }
    }

    @Override
    @PostMapping("/update")
    @Operation(summary = "修改归档记录")
    public Result<Void> updateAuditLogArchiveRecord(@RequestBody @Valid ApiAuditLogArchiveParam param) {
        try {
            AuditLogArchiveRecordDTO dto = AuditLogArchiveRecordConvert.INSTANCE.toDTO(param);
            logArchiveRecordService.updateAuditLogArchiveRecord(dto);
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改归档记录失败", e);
            return Result.error("修改归档记录失败");
        }
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "归档记录详情")
    public Result<AuditLogArchiveRecordAO> detail(@RequestParam("id") Long id) {
        try {
            AuditLogArchiveRecordDTO dto = logArchiveRecordManager.getDtoById(id);
            if (dto == null) {
                return Result.success(null);
            }
            return Result.success(AuditLogArchiveRecordConvert.INSTANCE.toAO(dto));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询归档记录详情失败", e);
            return Result.error("查询归档记录详情失败");
        }
    }

    @Override
    @GetMapping("/page")
    @Operation(summary = "归档记录分页列表")
    public Result<Paging<AuditLogArchiveRecordAO>> page(@RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                                                        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        try {
            AuditLogArchiveRecordPageParam param = new AuditLogArchiveRecordPageParam();
            param.setCurrent(pageNum);
            param.setSize(pageSize);
            Paging<AuditLogArchiveRecordVO> result = logArchiveRecordManager.getPage(param);
            return Result.success(copyPaging(result, AuditLogArchiveRecordConvert.INSTANCE.toAOFromVO(result.getRecords())));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("分页查询归档记录失败", e);
            return Result.error("分页查询归档记录失败");
        }
    }

    private static Paging<AuditLogArchiveRecordAO> copyPaging(Paging<AuditLogArchiveRecordVO> source,
                                                              List<AuditLogArchiveRecordAO> records) {
        Paging<AuditLogArchiveRecordAO> target = new Paging<>();
        target.setCurrent(source.getCurrent());
        target.setSize(source.getSize());
        target.setTotal(source.getTotal());
        target.setPages(source.getPages());
        target.setRecords(records);
        return target;
    }
}
