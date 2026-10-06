package com.carlos.system.menu.controller;

import com.carlos.boot.web.resource.ResourceScanService;
import com.carlos.boot.web.resource.model.ResourceApp;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * API 自动扫描接口
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnBean(ResourceScanService.class)
@RequestMapping("/sys/operation")
@Tag(name = "API自动扫描")
public class SysOperationScanController {

    private final ResourceScanService resourceScanService;

    @Log(
        title = "操作权限",
        logType = "SYS_OPERATION_SCAN",
        businessType = BusinessType.INSERT,
        operation = "'API 自动扫描：将未记录的接口写入操作权限库'",
        targetType = "SYS_OPERATION",
        riskLevel = 50)
    @PreAuthorize("hasPermission('manage:operation', 'scan')")
    @PostMapping("scan")
    @Operation(summary = "自动扫描Controller接口，将未记录的API写入sys_operation")
    public void scan() {
        ResourceApp resource = resourceScanService.scan();
        if (resource != null) {
            log.info("Operation scan completed, groups: {}", resource.getGroups() != null ? resource.getGroups().size() : 0);
        }
    }

}
