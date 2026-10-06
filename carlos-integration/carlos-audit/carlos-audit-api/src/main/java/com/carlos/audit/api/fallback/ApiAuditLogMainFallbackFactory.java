package com.carlos.audit.api.fallback;

import com.carlos.audit.api.ApiAuditLogMain;
import com.carlos.audit.api.pojo.ao.AuditLogMainAO;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.core.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;

/**
 * <p>
 * 审计日志 Feign 接口降级工厂
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Slf4j
public class ApiAuditLogMainFallbackFactory implements FallbackFactory<ApiAuditLogMain> {

    @Override
    public ApiAuditLogMain create(Throwable throwable) {
        log.error("审计日志服务调用失败", throwable);

        return new ApiAuditLogMain() {

            @Override
            public Result<AuditLogMainAO> saveAuditLog(ApiAuditLogMainParam param) {
                log.warn("保存审计日志降级处理，logType: {}", param != null ? param.getLogType() : "null");
                // 降级时返回失败，但不影响主业务流程
                return Result.error("审计日志服务暂不可用，日志已丢弃");
            }

            @Override
            public Result<AuditLogMainAO> saveAuditLogSync(ApiAuditLogMainParam param, long timeoutMs) {
                log.warn("同步保存审计日志降级处理，logType: {}", param != null ? param.getLogType() : "null");
                return Result.error("审计日志服务暂不可用，日志已丢弃");
            }

            @Override
            public Result<Void> batchSaveAuditLog(List<ApiAuditLogMainParam> params) {
                log.warn("批量保存审计日志降级处理，日志数量: {}", params != null ? params.size() : 0);
                return Result.error("审计日志服务暂不可用，日志已丢弃");
            }

            @Override
            public Result<AuditLogMainAO> saveSimpleAuditLog(String logType, AuditLogCategoryEnum category,
                                                             String operation, String principalId,
                                                             String targetId, AuditLogStatusEnum state) {
                log.warn("保存简单审计日志降级处理，logType: {}", logType);
                return Result.error("审计日志服务暂不可用，日志已丢弃");
            }
        };
    }
}
