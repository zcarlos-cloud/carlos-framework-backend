package com.carlos.audit.api.fallback;

import com.carlos.audit.api.ApiAuditLogConfig;
import com.carlos.audit.api.pojo.ao.AuditLogConfigAO;
import com.carlos.audit.api.pojo.param.ApiAuditLogConfigParam;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * 审计日志配置 Feign 接口降级工厂
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Slf4j
public class ApiAuditLogConfigFallbackFactory implements FallbackFactory<ApiAuditLogConfig> {

    @Override
    public ApiAuditLogConfig create(Throwable throwable) {
        log.error("审计日志配置服务调用失败: {}", throwable.getMessage());

        return new ApiAuditLogConfig() {

            @Override
            public Result<Void> addAuditLogConfig(ApiAuditLogConfigParam param) {
                return Result.error("审计日志配置服务暂不可用");
            }

            @Override
            public Result<Void> deleteAuditLogConfig(Long id) {
                return Result.error("审计日志配置服务暂不可用");
            }

            @Override
            public Result<Void> updateAuditLogConfig(ApiAuditLogConfigParam param) {
                return Result.error("审计日志配置服务暂不可用");
            }

            @Override
            public Result<AuditLogConfigAO> detail(Long id) {
                return Result.error("审计日志配置服务暂不可用");
            }

            @Override
            public Result<Paging<AuditLogConfigAO>> page(int pageNum, int pageSize) {
                return Result.error("审计日志配置服务暂不可用");
            }
        };
    }
}
