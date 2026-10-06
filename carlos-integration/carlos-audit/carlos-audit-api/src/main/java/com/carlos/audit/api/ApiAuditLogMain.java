package com.carlos.audit.api;

import com.carlos.audit.ServiceNameConstant;
import com.carlos.audit.api.fallback.ApiAuditLogMainFallbackFactory;
import com.carlos.audit.api.pojo.ao.AuditLogMainAO;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * <p>
 * 审计日志 Feign 接口
 * 提供给其他微服务通过 Feign 调用保存审计日志
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "logMain", path = "/api/audit/log/main", fallbackFactory = ApiAuditLogMainFallbackFactory.class)
@Tag(name = "审计日志Feign接口", description = "供其他微服务调用保存审计日志")
public interface ApiAuditLogMain {

    /**
     * 保存审计日志（异步）
     * <p>
     * 其他微服务通过 Feign 调用此方法保存审计日志
     * 日志将通过 Disruptor 异步写入，性能最优
     *
     * @param param 审计日志参数
     * @return 保存结果
     */
    @PostMapping("/save")
    @Operation(summary = "保存审计日志（异步）", description = "通过Disruptor异步写入，性能最优")
    Result<AuditLogMainAO> saveAuditLog(@RequestBody ApiAuditLogMainParam param);

    /**
     * 保存审计日志（同步）
     * <p>
     * 重要日志使用同步方式，确保日志写入成功
     *
     * @param param     审计日志参数
     * @param timeoutMs 超时时间（毫秒），默认5000
     * @return 保存结果
     */
    @PostMapping("/save-sync")
    @Operation(summary = "保存审计日志（同步）", description = "重要日志使用，确保写入成功")
    Result<AuditLogMainAO> saveAuditLogSync(@RequestBody ApiAuditLogMainParam param,
                                            @RequestParam(value = "timeoutMs", defaultValue = "5000") long timeoutMs);

    /**
     * 批量保存审计日志（异步）
     * <p>
     * 适用于需要批量写入日志的场景
     *
     * @param params 审计日志参数列表
     * @return 保存结果
     */
    @PostMapping("/batch-save")
    @Operation(summary = "批量保存审计日志（异步）")
    Result<Void> batchSaveAuditLog(@RequestBody List<ApiAuditLogMainParam> params);

    /**
     * 保存简单审计日志（最小参数）
     * <p>
     * 适用于快速记录简单日志场景
     *
     * @param logType    日志类型，如 USER_LOGIN、ORDER_PAY
     * @param category   日志大类：SECURITY/BUSINESS/SYSTEM/AUDIT
     * @param operation  操作描述
     * @param principalId 操作主体ID
     * @param targetId   目标对象ID（可选）
     * @param state      状态：SUCCESS/FAIL（可选，默认SUCCESS）
     * @return 保存结果
     */
    @PostMapping("/save-simple")
    @Operation(summary = "保存简单审计日志", description = "最小参数快速记录")
    Result<AuditLogMainAO> saveSimpleAuditLog(@RequestParam("logType") String logType,
                                              @RequestParam("category") AuditLogCategoryEnum category,
                                              @RequestParam("operation") String operation,
                                              @RequestParam("principalId") String principalId,
                                              @RequestParam(value = "targetId", required = false) String targetId,
                                              @RequestParam(value = "state", defaultValue = "SUCCESS") AuditLogStatusEnum state);
}
