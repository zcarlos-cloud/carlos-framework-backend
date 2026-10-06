package com.carlos.audit.pojo.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 审计日志导出数据对象（精选字段，不含 old/newData 等大文本）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月17日
 */
@Data
public class AuditLogExcel implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "日志ID")
    private Long id;

    @ExcelProperty(value = "服务器时间")
    private String serverTime;

    @ExcelProperty(value = "日志大类")
    private String category;

    @ExcelProperty(value = "日志类型")
    private String logType;

    @ExcelProperty(value = "风险等级")
    private Integer riskLevel;

    @ExcelProperty(value = "状态")
    private String logStatus;

    @ExcelProperty(value = "操作描述")
    private String operation;

    @ExcelProperty(value = "操作主体ID")
    private String principalId;

    @ExcelProperty(value = "操作主体名称")
    private String principalName;

    @ExcelProperty(value = "租户ID")
    private String tenantId;

    @ExcelProperty(value = "目标对象类型")
    private String targetType;

    @ExcelProperty(value = "目标对象ID")
    private String targetId;

    @ExcelProperty(value = "目标对象名称")
    private String targetName;

    @ExcelProperty(value = "客户端IP")
    private String clientIp;

    @ExcelProperty(value = "业务渠道")
    private String bizChannel;

    @ExcelProperty(value = "耗时(ms)")
    private Integer durationMs;

    @ExcelProperty(value = "结果码")
    private String resultCode;

    @ExcelProperty(value = "结果描述")
    private String resultMessage;

    @ExcelProperty(value = "链路追踪ID")
    private String traceId;

    @ExcelProperty(value = "应用名称")
    private String appName;
}
