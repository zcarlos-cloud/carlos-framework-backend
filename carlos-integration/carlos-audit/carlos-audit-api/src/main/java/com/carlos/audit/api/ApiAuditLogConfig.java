package com.carlos.audit.api;

import com.carlos.audit.ServiceNameConstant;
import com.carlos.audit.api.fallback.ApiAuditLogConfigFallbackFactory;
import com.carlos.audit.api.pojo.ao.AuditLogConfigAO;
import com.carlos.audit.api.pojo.param.ApiAuditLogConfigParam;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 审计日志配置 Feign 接口
 *
 * @author Carlos
 * @date 2026-06-12
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "logConfig",
    path = "/api/audit/log/config", fallbackFactory = ApiAuditLogConfigFallbackFactory.class)
@Tag(name = "审计日志配置Feign接口", description = "供其他微服务管理审计日志配置")
public interface ApiAuditLogConfig {

    /**
     * 新增审计日志配置
     *
     * @param param 配置参数
     * @return 新增结果
     */
    @PostMapping("/add")
    @Operation(summary = "新增审计日志配置")
    Result<Void> addAuditLogConfig(@RequestBody ApiAuditLogConfigParam param);

    /**
     * 删除审计日志配置
     *
     * @param id 配置ID
     * @return 删除结果
     */
    @PostMapping("/delete")
    @Operation(summary = "删除审计日志配置")
    Result<Void> deleteAuditLogConfig(@RequestParam("id") Long id);

    /**
     * 修改审计日志配置
     *
     * @param param 配置参数
     * @return 修改结果
     */
    @PostMapping("/update")
    @Operation(summary = "修改审计日志配置")
    Result<Void> updateAuditLogConfig(@RequestBody ApiAuditLogConfigParam param);

    /**
     * 查询审计日志配置详情
     *
     * @param id 配置ID
     * @return 配置详情
     */
    @GetMapping("/detail")
    @Operation(summary = "审计日志配置详情")
    Result<AuditLogConfigAO> detail(@RequestParam("id") Long id);

    /**
     * 分页查询审计日志配置
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "审计日志配置分页列表")
    Result<Paging<AuditLogConfigAO>> page(@RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                                           @RequestParam(value = "pageSize", defaultValue = "20") int pageSize);
}
