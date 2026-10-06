package com.carlos.audit.apiimpl;

import com.carlos.audit.api.ApiAuditLogConfig;
import com.carlos.audit.api.pojo.ao.AuditLogConfigAO;
import com.carlos.audit.api.pojo.param.ApiAuditLogConfigParam;
import com.carlos.audit.convert.AuditLogConfigConvert;
import com.carlos.audit.manager.AuditLogConfigManager;
import com.carlos.audit.pojo.dto.AuditLogConfigDTO;
import com.carlos.audit.pojo.param.AuditLogConfigPageParam;
import com.carlos.audit.pojo.vo.AuditLogConfigVO;
import com.carlos.audit.service.AuditLogConfigService;
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
 * 审计日志配置 Feign 接口实现
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit/log/config")
@Tag(name = "审计日志配置Feign接口", description = "供其他微服务管理审计日志配置")
public class ApiAuditLogConfigImpl implements ApiAuditLogConfig {

    private final AuditLogConfigService logConfigService;
    private final AuditLogConfigManager logConfigManager;

    @Override
    @PostMapping("/add")
    @Operation(summary = "新增审计日志配置")
    public Result<Void> addAuditLogConfig(@RequestBody @Valid ApiAuditLogConfigParam param) {
        try {
            AuditLogConfigDTO dto = AuditLogConfigConvert.INSTANCE.toDTO(param);
            logConfigService.addAuditLogConfig(dto);
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("新增审计日志配置失败", e);
            return Result.error("新增审计日志配置失败");
        }
    }

    @Override
    @PostMapping("/delete")
    @Operation(summary = "删除审计日志配置")
    public Result<Void> deleteAuditLogConfig(@RequestParam("id") Long id) {
        try {
            logConfigService.deleteAuditLogConfig(java.util.Set.of(id));
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除审计日志配置失败", e);
            return Result.error("删除审计日志配置失败");
        }
    }

    @Override
    @PostMapping("/update")
    @Operation(summary = "修改审计日志配置")
    public Result<Void> updateAuditLogConfig(@RequestBody @Valid ApiAuditLogConfigParam param) {
        try {
            AuditLogConfigDTO dto = AuditLogConfigConvert.INSTANCE.toDTO(param);
            logConfigService.updateAuditLogConfig(dto);
            return Result.success();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改审计日志配置失败", e);
            return Result.error("修改审计日志配置失败");
        }
    }

    @Override
    @GetMapping("/detail")
    @Operation(summary = "审计日志配置详情")
    public Result<AuditLogConfigAO> detail(@RequestParam("id") Long id) {
        try {
            AuditLogConfigDTO dto = logConfigManager.getDtoById(id);
            if (dto == null) {
                return Result.success(null);
            }
            return Result.success(AuditLogConfigConvert.INSTANCE.toAO(dto));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询审计日志配置详情失败", e);
            return Result.error("查询审计日志配置详情失败");
        }
    }

    @Override
    @GetMapping("/page")
    @Operation(summary = "审计日志配置分页列表")
    public Result<Paging<AuditLogConfigAO>> page(@RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                                                 @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        try {
            AuditLogConfigPageParam param = new AuditLogConfigPageParam();
            param.setCurrent(pageNum);
            param.setSize(pageSize);
            Paging<AuditLogConfigVO> result = logConfigManager.getPage(param);
            return Result.success(copyPaging(result, AuditLogConfigConvert.INSTANCE.toAOFromVO(result.getRecords())));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("分页查询审计日志配置失败", e);
            return Result.error("分页查询审计日志配置失败");
        }
    }

    private static Paging<AuditLogConfigAO> copyPaging(Paging<AuditLogConfigVO> source, List<AuditLogConfigAO> records) {
        Paging<AuditLogConfigAO> target = new Paging<>();
        target.setCurrent(source.getCurrent());
        target.setSize(source.getSize());
        target.setTotal(source.getTotal());
        target.setPages(source.getPages());
        target.setRecords(records);
        return target;
    }
}
