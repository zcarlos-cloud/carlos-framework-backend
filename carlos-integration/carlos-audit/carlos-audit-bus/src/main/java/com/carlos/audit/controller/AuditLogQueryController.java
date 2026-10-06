package com.carlos.audit.controller;

import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.pojo.param.AuditLogQueryPageParam;
import com.carlos.audit.pojo.vo.AuditLogDailyStatsVO;
import com.carlos.audit.pojo.vo.AuditLogStatsVO;
import com.carlos.audit.service.AuditLogQueryService;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.security.auth.UserContext;
import com.carlos.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 审计日志查询控制器
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@Slf4j
@RestController
@RequestMapping("/audit/log/query")
@RequiredArgsConstructor
@Tag(name = "审计日志查询", description = "审计日志查询接口")
public class AuditLogQueryController {

    private final AuditLogQueryService queryService;

    /**
     * 分页查询审计日志
     */
    @PreAuthorize("hasPermission('audit:log', 'view')")
    @PostMapping("/logs/page")
    @Operation(summary = "分页查询审计日志")
    public Result<Paging<AuditLogMainDTO>> page(@Validated @RequestBody AuditLogQueryPageParam param) {
        String tenantId = getCurrentTenantId();
        Paging<AuditLogMainDTO> result = queryService.pageQuery(tenantId, param);
        return Result.success(result);
    }

    /**
     * 查询单条日志详情
     */
    @PreAuthorize("hasPermission('audit:log', 'view')")
    @GetMapping("/logs/detail")
    @Operation(summary = "查询审计日志详情")
    public Result<AuditLogMainDTO> getById(@RequestParam Long id) {
        AuditLogMainDTO dto = queryService.getById(id);
        return Result.success(dto);
    }

    /**
     * 获取实时统计
     */
    @PreAuthorize("hasPermission('audit:log', 'view')")
    @GetMapping("/stats/realtime")
    @Operation(summary = "获取实时统计")
    public Result<AuditLogStatsVO> getRealtimeStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String tenantId = getCurrentTenantId();
        AuditLogStatsVO stats = queryService.getRealtimeStats(tenantId, date);
        return Result.success(stats);
    }

    /**
     * 获取按天聚合统计（趋势图）
     */
    @PreAuthorize("hasPermission('audit:log', 'view')")
    @GetMapping("/stats/daily")
    @Operation(summary = "获取按天聚合统计")
    public Result<List<AuditLogDailyStatsVO>> getDailyStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String tenantId = getCurrentTenantId();
        List<AuditLogDailyStatsVO> stats = queryService.getDailyStats(tenantId, startDate, endDate);
        return Result.success(stats);
    }

    /**
     * 查询用户行为轨迹
     */
    @PreAuthorize("hasPermission('audit:log', 'view')")
    @GetMapping("/trail")
    @Operation(summary = "查询用户行为轨迹")
    public Result<List<AuditLogMainDTO>> getUserTrail(
        @RequestParam String principalId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<AuditLogMainDTO> trail = queryService.getUserTrail(principalId, start, end);
        return Result.success(trail);
    }

    /**
     * 查询风险事件
     */
    @PreAuthorize("hasPermission('audit:log', 'view')")
    @GetMapping("/risks")
    @Operation(summary = "查询风险事件")
    public Result<List<AuditLogMainDTO>> listRiskEvents(
        @RequestParam(defaultValue = "50") int minRiskLevel,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        String tenantId = getCurrentTenantId();
        List<AuditLogMainDTO> risks = queryService.listRiskEvents(tenantId, minRiskLevel, start, end);
        return Result.success(risks);
    }

    /**
     * 按当前筛选条件导出审计日志 Excel
     */
    @PreAuthorize("hasPermission('audit:log', 'export')")
    @PostMapping("/export")
    @Operation(summary = "导出审计日志")
    public void export(@Validated @RequestBody AuditLogQueryPageParam param, HttpServletResponse response) {
        String tenantId = getCurrentTenantId();
        queryService.exportLogs(tenantId, param, response);
    }

    /**
     * 获取当前租户ID
     */
    private String getCurrentTenantId() {
        try {
            UserContext user = SecurityUtils.getCurrentUser();
            if (user == null || user.getTenantId() == null) {
                return "0";
            }
            return String.valueOf(user.getTenantId());
        } catch (Exception e) {
            log.warn("获取当前租户ID失败，使用默认租户0", e);
            return "0";
        }
    }
}
