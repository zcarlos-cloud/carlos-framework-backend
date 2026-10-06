package com.carlos.audit.api.fallback;

import com.carlos.audit.api.ApiAuditLogQuery;
import com.carlos.audit.api.pojo.ao.AuditLogMainAO;
import com.carlos.audit.api.pojo.ao.AuditLogStatsAO;
import com.carlos.audit.api.pojo.param.ApiAuditLogQueryParam;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志查询 Feign 接口降级工厂
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Slf4j
public class ApiAuditLogQueryFallbackFactory implements FallbackFactory<ApiAuditLogQuery> {

    @Override
    public ApiAuditLogQuery create(Throwable throwable) {
        log.error("审计日志查询服务调用失败: {}", throwable.getMessage());

        return new ApiAuditLogQuery() {

            @Override
            public Result<Paging<AuditLogMainAO>> pageQuery(ApiAuditLogQueryParam param) {
                return Result.error("审计日志查询服务暂不可用");
            }

            @Override
            public Result<AuditLogMainAO> getById(Long id) {
                return Result.error("审计日志查询服务暂不可用");
            }

            @Override
            public Result<AuditLogStatsAO> getRealtimeStats(String tenantId, LocalDate date) {
                return Result.error("审计日志查询服务暂不可用");
            }

            @Override
            public Result<List<AuditLogMainAO>> getUserTrail(String principalId, LocalDateTime start, LocalDateTime end) {
                return Result.error("审计日志查询服务暂不可用");
            }

            @Override
            public Result<List<AuditLogMainAO>> listRiskEvents(String tenantId, int minRiskLevel,
                                                                LocalDateTime start, LocalDateTime end) {
                return Result.error("审计日志查询服务暂不可用");
            }
        };
    }
}
