package com.carlos.audit.api;

import com.carlos.audit.ServiceNameConstant;
import com.carlos.audit.api.fallback.ApiAuditLogQueryFallbackFactory;
import com.carlos.audit.api.pojo.ao.AuditLogMainAO;
import com.carlos.audit.api.pojo.ao.AuditLogStatsAO;
import com.carlos.audit.api.pojo.param.ApiAuditLogQueryParam;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志查询 Feign 接口
 *
 * @author Carlos
 * @date 2026-06-12
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "logQuery",
    path = "/api/audit/log/query", fallbackFactory = ApiAuditLogQueryFallbackFactory.class)
@Tag(name = "审计日志查询Feign接口", description = "供其他微服务查询审计日志")
public interface ApiAuditLogQuery {

    /**
     * 分页查询审计日志
     *
     * @param param 查询参数
     * @return 分页结果
     */
    @GetMapping("/logs/page")
    @Operation(summary = "分页查询审计日志")
    Result<Paging<AuditLogMainAO>> pageQuery(@SpringQueryMap @Valid ApiAuditLogQueryParam param);

    /**
     * 查询审计日志详情
     *
     * @param id 日志ID
     * @return 日志详情
     */
    @GetMapping("/logs/detail")
    @Operation(summary = "查询审计日志详情")
    Result<AuditLogMainAO> getById(@RequestParam("id") Long id);

    /**
     * 获取实时统计
     *
     * @param tenantId 租户ID
     * @param date     统计日期
     * @return 统计结果
     */
    @GetMapping("/stats/realtime")
    @Operation(summary = "获取实时统计")
    Result<AuditLogStatsAO> getRealtimeStats(@RequestParam("tenantId") String tenantId,
                                               @RequestParam("date") LocalDate date);

    /**
     * 查询用户行为轨迹
     *
     * @param principalId 用户标识
     * @param start       开始时间
     * @param end         结束时间
     * @return 行为轨迹列表
     */
    @GetMapping("/trail")
    @Operation(summary = "查询用户行为轨迹")
    Result<List<AuditLogMainAO>> getUserTrail(@RequestParam("principalId") String principalId,
                                                @RequestParam(value = "start", required = false) LocalDateTime start,
                                                @RequestParam(value = "end", required = false) LocalDateTime end);

    /**
     * 查询风险事件
     *
     * @param tenantId     租户ID
     * @param minRiskLevel 最小风险等级
     * @param start        开始时间
     * @param end          结束时间
     * @return 风险事件列表
     */
    @GetMapping("/risks")
    @Operation(summary = "查询风险事件")
    Result<List<AuditLogMainAO>> listRiskEvents(@RequestParam("tenantId") String tenantId,
                                                  @RequestParam(value = "minRiskLevel", defaultValue = "50") int minRiskLevel,
                                                  @RequestParam(value = "start", required = false) LocalDateTime start,
                                                  @RequestParam(value = "end", required = false) LocalDateTime end);
}
