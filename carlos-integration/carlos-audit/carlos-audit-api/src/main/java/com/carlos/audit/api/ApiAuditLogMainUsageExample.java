package com.carlos.audit.api;

import com.carlos.audit.api.pojo.ao.AuditLogMainAO;
import com.carlos.audit.api.pojo.enums.*;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.core.response.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 审计日志 Feign 接口使用示例
 * </p>
 * <p>
 * 本类展示了如何在其他微服务中使用 ApiAuditLogMain Feign 接口保存审计日志
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiAuditLogMainUsageExample {

    private final ApiAuditLogMain auditLogClient;

    /**
     * 示例1：异步保存完整审计日志
     * <p>
     * 适用于需要记录详细信息的场景
     */
    public void example1AsyncSaveFullLog() {
        // 构建审计日志参数
        ApiAuditLogMainParam param = new ApiAuditLogMainParam();
        param.setLogType("ORDER_PAY");
        param.setCategory(AuditLogCategoryEnum.BUSINESS);
        param.setOperation("订单支付：订单号ORDER_202403060001");
        param.setPrincipalId("10001");
        param.setPrincipalType(AuditLogPrincipalTypeEnum.USER);
        param.setPrincipalName("张三");
        param.setTenantId("T001");
        param.setTargetType("ORDER");
        param.setTargetId("ORDER_202403060001");
        param.setTargetName("订单-商品A*2");
        param.setLogStatus(AuditLogStatusEnum.SUCCESS);
        param.setRiskLevel(70);
        param.setMonetaryAmount(new BigDecimal("199.99"));
        param.setBizOrderNo("ORDER_202403060001");
        param.setBizScene("订单支付-支付宝");
        param.setClientIp("192.168.1.100");
        param.setUserAgent("Mozilla/5.0...");
        param.setBizChannel(AuditLogBizChannelEnum.WEB);
        param.setServerTime(LocalDateTime.now());
        param.setEventTime(LocalDateTime.now());

        // 调用 Feign 接口保存（异步）
        Result<AuditLogMainAO> result = auditLogClient.saveAuditLog(param);

        if (result.isSuccess()) {
            log.info("审计日志保存成功，ID: {}", result.getData().getId());
        } else {
            log.warn("审计日志保存失败: {}", result.getMsg());
            // 注意：异步保存失败不影响主业务流程
        }
    }

    /**
     * 示例2：同步保存审计日志（重要操作）
     * <p>
     * 适用于重要操作，确保日志写入成功
     */
    public void example2SyncSaveImportantLog() {
        ApiAuditLogMainParam param = new ApiAuditLogMainParam();
        param.setLogType("CONFIG_CHANGE");
        param.setCategory(AuditLogCategoryEnum.SYSTEM);
        param.setOperation("修改系统配置：数据库连接池大小");
        param.setPrincipalId("ADMIN_001");
        param.setPrincipalType(AuditLogPrincipalTypeEnum.USER);
        param.setTargetType("CONFIG");
        param.setTargetId("DB_POOL_CONFIG");
        param.setLogStatus(AuditLogStatusEnum.SUCCESS);
        param.setRiskLevel(90);
        param.setOldData("{\"maxConnections\": 100}");
        param.setNewData("{\"maxConnections\": 200}");
        param.setHasDataChange(true);
        param.setChangeSummary("数据库连接池最大连接数从100修改为200");
        param.setServerTime(LocalDateTime.now());
        param.setEventTime(LocalDateTime.now());

        // 调用 Feign 接口保存（同步，超时5秒）
        Result<AuditLogMainAO> result = auditLogClient.saveAuditLogSync(param, 5000);

        if (result.isSuccess()) {
            log.info("重要审计日志同步保存成功，ID: {}", result.getData().getId());
        } else {
            log.error("重要审计日志同步保存失败: {}", result.getMsg());
            // 重要日志保存失败，可能需要回滚业务操作或告警
            throw AuditErrorCode.AUDIT_LOG_SAVE_FAILED.exception();
        }
    }

    /**
     * 示例3：批量保存审计日志
     * <p>
     * 适用于批量操作的场景
     */
    public void example3BatchSaveLogs() {
        // 构建批量日志参数
        ApiAuditLogMainParam log1 = new ApiAuditLogMainParam();
        log1.setLogType("DATA_EXPORT");
        log1.setCategory(AuditLogCategoryEnum.SECURITY);
        log1.setOperation("导出订单数据");
        log1.setPrincipalId("10001");
        log1.setLogStatus(AuditLogStatusEnum.SUCCESS);
        log1.setServerTime(LocalDateTime.now());
        log1.setEventTime(LocalDateTime.now());

        ApiAuditLogMainParam log2 = new ApiAuditLogMainParam();
        log2.setLogType("DATA_EXPORT");
        log2.setCategory(AuditLogCategoryEnum.SECURITY);
        log2.setOperation("导出用户数据");
        log2.setPrincipalId("10001");
        log2.setLogStatus(AuditLogStatusEnum.SUCCESS);
        log2.setServerTime(LocalDateTime.now());
        log2.setEventTime(LocalDateTime.now());

        List<ApiAuditLogMainParam> params = Arrays.asList(log1, log2);

        // 批量保存（异步）
        Result<Void> result = auditLogClient.batchSaveAuditLog(params);

        if (result.isSuccess()) {
            log.info("批量审计日志保存成功，数量: {}", params.size());
        } else {
            log.warn("批量审计日志保存失败: {}", result.getMsg());
        }
    }

    /**
     * 示例4：快速保存简单审计日志
     * <p>
     * 适用于快速记录简单日志，参数最少
     */
    public void example4QuickSaveSimpleLog() {
        // 快速保存简单日志（异步）
        Result<AuditLogMainAO> result = auditLogClient.saveSimpleAuditLog(
            "USER_LOGIN",           // logType
            AuditLogCategoryEnum.SECURITY,             // category
            "用户登录成功",          // operation
            "10001",                // principalId
            null,                   // targetId（可选）
                AuditLogStatusEnum.SUCCESS// logStatus（可选，默认SUCCESS）
        );

        if (result.isSuccess()) {
            log.info("简单审计日志保存成功，ID: {}", result.getData().getId());
        } else {
            log.warn("简单审计日志保存失败: {}", result.getMsg());
        }
    }

    /**
     * 示例5：在业务方法中集成审计日志
     * <p>
     * 展示如何在实际业务方法中使用
     */
    public String example5BusinessMethodWithAudit(String orderNo, BigDecimal amount, String userId) {
        boolean success = false;
        String resultMessage = "";

        try {
            // 执行业务逻辑...
            // orderService.payOrder(orderNo, amount);

            success = true;
            resultMessage = "支付成功";

            // 记录成功日志
            ApiAuditLogMainParam param = new ApiAuditLogMainParam();
            param.setLogType("ORDER_PAY");
            param.setCategory(AuditLogCategoryEnum.BUSINESS);
            param.setOperation("订单支付：订单号" + orderNo + "，金额：" + amount);
            param.setPrincipalId(userId);
            param.setTargetId(orderNo);
            param.setTargetType("ORDER");
            param.setMonetaryAmount(amount);
            param.setLogStatus(AuditLogStatusEnum.SUCCESS);
            param.setBizOrderNo(orderNo);
            param.setServerTime(LocalDateTime.now());
            param.setEventTime(LocalDateTime.now());

            auditLogClient.saveAuditLog(param);

        } catch (Exception e) {
            success = false;
            resultMessage = "支付失败：" + e.getMessage();

            // 记录失败日志
            ApiAuditLogMainParam param = new ApiAuditLogMainParam();
            param.setLogType("ORDER_PAY");
            param.setCategory(AuditLogCategoryEnum.BUSINESS);
            param.setOperation("订单支付失败：订单号" + orderNo + "，错误：" + e.getMessage());
            param.setPrincipalId(userId);
            param.setTargetId(orderNo);
            param.setTargetType("ORDER");
            param.setMonetaryAmount(amount);
            param.setLogStatus(AuditLogStatusEnum.FAIL);
            param.setResultCode(e.getClass().getSimpleName());
            param.setResultMessage(e.getMessage());
            param.setBizOrderNo(orderNo);
            param.setServerTime(LocalDateTime.now());
            param.setEventTime(LocalDateTime.now());

            auditLogClient.saveAuditLog(param);
        }

        return resultMessage;
    }

    /**
     * 业务系统日志类型常量定义示例
     */
    public static final class OrderLogType {
        public static final String ORDER_CREATE = "ORDER_CREATE";
        public static final String ORDER_PAY = "ORDER_PAY";
        public static final String ORDER_CANCEL = "ORDER_CANCEL";
        public static final String ORDER_SHIP = "ORDER_SHIP";
        public static final String ORDER_COMPLETE = "ORDER_COMPLETE";
        public static final String ORDER_REFUND = "ORDER_REFUND";

        private OrderLogType() {
        }
    }

    public static final class UserLogType {
        public static final String USER_REGISTER = "USER_REGISTER";
        public static final String USER_LOGIN = "USER_LOGIN";
        public static final String USER_LOGOUT = "USER_LOGOUT";
        public static final String USER_PASSWORD_CHANGE = "USER_PASSWORD_CHANGE";
        public static final String USER_INFO_UPDATE = "USER_INFO_UPDATE";

        private UserLogType() {
        }
    }
}
