package com.carlos.audit.api;

import com.carlos.audit.ServiceNameConstant;
import com.carlos.audit.api.fallback.ApiAuditLogArchiveFallbackFactory;
import com.carlos.audit.api.pojo.ao.AuditLogArchiveRecordAO;
import com.carlos.audit.api.pojo.param.ApiAuditLogArchiveParam;
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
 * 审计日志归档记录 Feign 接口
 *
 * @author Carlos
 * @date 2026-06-12
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "logArchive",
    path = "/api/audit/log/archive/record", fallbackFactory = ApiAuditLogArchiveFallbackFactory.class)
@Tag(name = "审计日志归档记录Feign接口", description = "供其他微服务管理审计日志归档记录")
public interface ApiAuditLogArchive {

    /**
     * 新增归档记录
     *
     * @param param 归档记录参数
     * @return 新增结果
     */
    @PostMapping("/add")
    @Operation(summary = "新增归档记录")
    Result<Void> addAuditLogArchiveRecord(@RequestBody ApiAuditLogArchiveParam param);

    /**
     * 删除归档记录
     *
     * @param id 归档记录ID
     * @return 删除结果
     */
    @PostMapping("/delete")
    @Operation(summary = "删除归档记录")
    Result<Void> deleteAuditLogArchiveRecord(@RequestParam("id") Long id);

    /**
     * 修改归档记录
     *
     * @param param 归档记录参数
     * @return 修改结果
     */
    @PostMapping("/update")
    @Operation(summary = "修改归档记录")
    Result<Void> updateAuditLogArchiveRecord(@RequestBody ApiAuditLogArchiveParam param);

    /**
     * 查询归档记录详情
     *
     * @param id 归档记录ID
     * @return 归档记录详情
     */
    @GetMapping("/detail")
    @Operation(summary = "归档记录详情")
    Result<AuditLogArchiveRecordAO> detail(@RequestParam("id") Long id);

    /**
     * 分页查询归档记录
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "归档记录分页列表")
    Result<Paging<AuditLogArchiveRecordAO>> page(@RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                                                 @RequestParam(value = "pageSize", defaultValue = "20") int pageSize);
}
