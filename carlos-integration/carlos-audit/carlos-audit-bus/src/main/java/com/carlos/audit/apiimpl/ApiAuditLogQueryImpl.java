package com.carlos.audit.apiimpl;

import com.carlos.audit.api.ApiAuditLogQuery;
import com.carlos.audit.api.pojo.ao.AuditLogMainAO;
import com.carlos.audit.api.pojo.ao.AuditLogStatsAO;
import com.carlos.audit.api.pojo.param.ApiAuditLogQueryParam;
import com.carlos.audit.convert.AuditLogMainConvert;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.pojo.param.AuditLogQueryPageParam;
import com.carlos.audit.pojo.vo.AuditLogStatsVO;
import com.carlos.audit.service.AuditLogQueryService;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志查询 Feign 接口实现
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit/log/query")
@Tag(name = "审计日志查询Feign接口", description = "供其他微服务查询审计日志")
public class ApiAuditLogQueryImpl implements ApiAuditLogQuery {

    private final AuditLogQueryService queryService;

    @Override
    @GetMapping("/logs/page")
    @Operation(summary = "分页查询审计日志")
    public Result<Paging<AuditLogMainAO>> pageQuery(@ModelAttribute @Valid ApiAuditLogQueryParam param) {
        try {
            AuditLogQueryPageParam queryParam = new AuditLogQueryPageParam();
            queryParam.setCurrent(param.getPageNum() == null ? 1 : param.getPageNum());
            queryParam.setSize(param.getPageSize() == null ? 20 : param.getPageSize());
            queryParam.setStartTime(param.getStartTime());
            queryParam.setEndTime(param.getEndTime());
            queryParam.setLogType(param.getLogType());
            queryParam.setLogStatus(param.getLogStatus());
            queryParam.setPrincipalId(param.getPrincipalId());
            queryParam.setPrincipalName(param.getPrincipalName());
            queryParam.setClientIp(param.getClientIp());
            queryParam.setAuthType(param.getAuthType());

            Paging<AuditLogMainDTO> result = queryService.pageQuery(param.getTenantId(), queryParam);
            return Result.success(copyPaging(result, AuditLogMainConvert.INSTANCE.toAO(result.getRecords())));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("分页查询审计日志失败", e);
            return Result.error("分页查询审计日志失败");
        }
    }

    /**
     * 复制分页结果并转换记录类型
     *
     * @param source  源分页结果
     * @param records 转换后的记录列表
     * @param <S>     源记录类型
     * @param <T>     目标记录类型
     * @return 目标分页结果
     */
    private static <S, T> Paging<T> copyPaging(Paging<S> source, List<T> records) {
        Paging<T> target = new Paging<>();
        target.setCurrent(source.getCurrent());
        target.setSize(source.getSize());
        target.setTotal(source.getTotal());
        target.setPages(source.getPages());
        target.setRecords(records);
        return target;
    }

    @Override
    @GetMapping("/logs/detail")
    @Operation(summary = "查询审计日志详情")
    public Result<AuditLogMainAO> getById(@RequestParam("id") Long id) {
        try {
            AuditLogMainDTO dto = queryService.getById(id);
            if (dto == null) {
                return Result.success(null);
            }
            return Result.success(AuditLogMainConvert.INSTANCE.toAO(dto));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询审计日志详情失败", e);
            return Result.error("查询审计日志详情失败");
        }
    }

    @Override
    @GetMapping("/stats/realtime")
    @Operation(summary = "获取实时统计")
    public Result<AuditLogStatsAO> getRealtimeStats(@RequestParam("tenantId") String tenantId,
                                                    @RequestParam("date") LocalDate date) {
        try {
            AuditLogStatsVO vo = queryService.getRealtimeStats(tenantId, date);
            return Result.success(AuditLogMainConvert.INSTANCE.toAO(vo));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取实时统计失败", e);
            return Result.error("获取实时统计失败");
        }
    }

    @Override
    @GetMapping("/trail")
    @Operation(summary = "查询用户行为轨迹")
    public Result<List<AuditLogMainAO>> getUserTrail(@RequestParam("principalId") String principalId,
                                                     @RequestParam(value = "start", required = false) LocalDateTime start,
                                                     @RequestParam(value = "end", required = false) LocalDateTime end) {
        try {
            List<AuditLogMainDTO> list = queryService.getUserTrail(principalId, start, end);
            return Result.success(AuditLogMainConvert.INSTANCE.toAO(list));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询用户行为轨迹失败", e);
            return Result.error("查询用户行为轨迹失败");
        }
    }

    @Override
    @GetMapping("/risks")
    @Operation(summary = "查询风险事件")
    public Result<List<AuditLogMainAO>> listRiskEvents(@RequestParam("tenantId") String tenantId,
                                                       @RequestParam(value = "minRiskLevel", defaultValue = "50") int minRiskLevel,
                                                       @RequestParam(value = "start", required = false) LocalDateTime start,
                                                       @RequestParam(value = "end", required = false) LocalDateTime end) {
        try {
            List<AuditLogMainDTO> list = queryService.listRiskEvents(tenantId, minRiskLevel, start, end);
            return Result.success(AuditLogMainConvert.INSTANCE.toAO(list));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询风险事件失败", e);
            return Result.error("查询风险事件失败");
        }
    }
}
