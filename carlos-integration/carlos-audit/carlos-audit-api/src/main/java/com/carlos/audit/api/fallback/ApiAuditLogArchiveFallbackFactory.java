package com.carlos.audit.api.fallback;

import com.carlos.audit.api.ApiAuditLogArchive;
import com.carlos.audit.api.pojo.ao.AuditLogArchiveRecordAO;
import com.carlos.audit.api.pojo.param.ApiAuditLogArchiveParam;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * 审计日志归档记录 Feign 接口降级工厂
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Slf4j
public class ApiAuditLogArchiveFallbackFactory implements FallbackFactory<ApiAuditLogArchive> {

    @Override
    public ApiAuditLogArchive create(Throwable throwable) {
        log.error("审计日志归档服务调用失败: {}", throwable.getMessage());

        return new ApiAuditLogArchive() {

            @Override
            public Result<Void> addAuditLogArchiveRecord(ApiAuditLogArchiveParam param) {
                return Result.error("审计日志归档服务暂不可用");
            }

            @Override
            public Result<Void> deleteAuditLogArchiveRecord(Long id) {
                return Result.error("审计日志归档服务暂不可用");
            }

            @Override
            public Result<Void> updateAuditLogArchiveRecord(ApiAuditLogArchiveParam param) {
                return Result.error("审计日志归档服务暂不可用");
            }

            @Override
            public Result<AuditLogArchiveRecordAO> detail(Long id) {
                return Result.error("审计日志归档服务暂不可用");
            }

            @Override
            public Result<Paging<AuditLogArchiveRecordAO>> page(int pageNum, int pageSize) {
                return Result.error("审计日志归档服务暂不可用");
            }
        };
    }
}
