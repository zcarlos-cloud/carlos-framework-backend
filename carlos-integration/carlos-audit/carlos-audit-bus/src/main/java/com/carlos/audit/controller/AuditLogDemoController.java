package com.carlos.audit.controller;

import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * <p>
 * 审计日志注解使用示例控制器
 * 演示 @Log 注解的各种使用方式（存储到 ClickHouse）
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */

@RestController
@RequestMapping("/api/audit/demo")
@Tag(name = "审计日志使用示例", description = "演示 @Log 注解的各种使用方式（存储到 ClickHouse）")
public class AuditLogDemoController {

    /**
     * 示例1: 用户登录（基础用法）
     */
    @Log(
        title = "用户管理",
        logType = "USER_LOGIN",
        businessType = BusinessType.LOGIN,
        operation = "'用户登录: ' + #param.username",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('audit:demo', 'login')")
    @PostMapping("/login")
    @Operation(summary = "用户登录示例")
    public Result<LoginVO> login(@RequestBody LoginParam param) {
        // 模拟登录逻辑
        LoginVO vo = new LoginVO();
        vo.setUserId("10001");
        vo.setToken("mock-jwt-token");
        return Result.success(vo);
    }

    /**
     * 示例2: 订单支付（完整用法，带目标对象）
     */
    @Log(
        title = "订单管理",
        logType = "ORDER_PAY",
        businessType = BusinessType.UPDATE,
        operation = "'订单支付: ' + #param.orderNo + ', 金额: ' + #param.amount",
        targetId = "#param.orderNo",
        targetType = "ORDER",
        riskLevel = 70,
        saveRequestParams = true,
        saveResponseData = true
    )
    @PreAuthorize("hasPermission('audit:demo', 'pay')")
    @PostMapping("/order/pay")
    @Operation(summary = "订单支付示例")
    public Result<OrderVO> payOrder(@RequestBody OrderPayParam param) {
        // 模拟支付逻辑
        OrderVO vo = new OrderVO();
        vo.setOrderNo(param.getOrderNo());
        vo.setStatus("PAID");
        return Result.success(vo);
    }

    /**
     * 示例3: 数据导出（高风险操作）
     */
    @Log(
        title = "数据导出",
        logType = "DATA_EXPORT",
        businessType = BusinessType.EXPORT,
        operation = "'数据导出: ' + #tableName + ', 条件: ' + #condition",
        targetType = "CONFIG",
        riskLevel = 80
    )
    @PreAuthorize("hasPermission('audit:demo', 'export')")
    @GetMapping("/export")
    @Operation(summary = "数据导出示例")
    public Result<String> exportData(
        @RequestParam String tableName,
        @RequestParam(required = false) String condition) {
        // 模拟导出逻辑
        return Result.success("export-file-id-12345");
    }

    /**
     * 示例4: 同步记录审计日志（重要操作）
     */
    @Log(
        title = "配置管理",
        logType = "CONFIG_CHANGE",
        businessType = BusinessType.UPDATE,
        operation = "'配置变更: ' + #param.configKey",
        targetType = "CONFIG",
        riskLevel = 90,
        async = false  // 同步记录，确保日志写入成功
    )
    @PreAuthorize("hasPermission('audit:demo', 'update')")
    @PostMapping("/config/update")
    @Operation(summary = "配置变更示例（同步审计）")
    public Result<Void> updateConfig(@RequestBody ConfigParam param) {
        // 模拟配置更新
        return Result.success();
    }

    /**
     * 示例5: 带业务渠道的审计日志
     */
    @Log(
        title = "文件管理",
        logType = "FILE_UPLOAD",
        businessType = BusinessType.INSERT,
        operation = "'文件上传: ' + #fileName",
        targetType = "FILE",
        riskLevel = 20,
        bizChannel = "#channel"
    )
    @PreAuthorize("hasPermission('audit:demo', 'upload')")
    @PostMapping("/file/upload")
    @Operation(summary = "文件上传示例（带业务渠道）")
    public Result<FileVO> uploadFile(
        @RequestParam String fileName,
        @RequestParam(defaultValue = "WEB") String channel) {
        // 模拟上传逻辑
        FileVO vo = new FileVO();
        vo.setFileId("file-123");
        vo.setFileName(fileName);
        return Result.success(vo);
    }

    // ==================== 参数/返回对象定义 ====================

    @Data
    static class LoginParam {
        private String username;
        private String password;
    }

    @Data
    static class LoginVO {
        private String userId;
        private String token;
    }

    @Data
    static class OrderPayParam {
        private String orderNo;
        private BigDecimal amount;
        private String payMethod;
    }

    @Data
    static class OrderVO {
        private String orderNo;
        private String status;
    }

    @Data
    static class ConfigParam {
        private String configKey;
        private String configValue;
    }

    @Data
    static class FileVO {
        private String fileId;
        private String fileName;
    }
}
