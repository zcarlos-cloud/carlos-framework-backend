package com.carlos.audit.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 审计日志查询分页参数（基于 ClickHouse 热数据）
 * </p>
 *
 * <p>字段与前端「审计日志查询」页面的筛选条件一一对应，租户由服务端从登录上下文解析，不作为入参。</p>
 *
 * @author Carlos
 * @date 2026年9月17日
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "审计日志查询分页参数")
public class AuditLogQueryPageParam extends ParamPage {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日志大类：SECURITY/BUSINESS/SYSTEM/AUDIT")
    @Size(max = 20, message = "日志大类长度不能超过20个字符")
    private String category;

    @Schema(description = "日志类型，如 USER_LOGIN/ORDER_PAY/DATA_EXPORT")
    @Size(max = 50, message = "日志类型长度不能超过50个字符")
    private String logType;

    @Schema(description = "日志状态：SUCCESS/FAIL/PENDING/TIMEOUT/PARTIAL_SUCCESS")
    @Size(max = 20, message = "日志状态长度不能超过20个字符")
    private String logStatus;

    @Schema(description = "操作主体ID")
    @Size(max = 64, message = "操作主体ID长度不能超过64个字符")
    private String principalId;

    @Schema(description = "操作主体名称（模糊匹配）")
    @Size(max = 100, message = "操作主体名称长度不能超过100个字符")
    private String principalName;

    @Schema(description = "操作主体类型：USER/SERVICE/SYSTEM/ANONYMOUS")
    @Size(max = 20, message = "操作主体类型长度不能超过20个字符")
    private String principalType;

    @Schema(description = "目标对象ID")
    @Size(max = 64, message = "目标对象ID长度不能超过64个字符")
    private String targetId;

    @Schema(description = "目标对象类型：ORDER/USER/CONFIG/DATA 等")
    @Size(max = 50, message = "目标对象类型长度不能超过50个字符")
    private String targetType;

    @Schema(description = "最小风险等级，0-100，含边界")
    @Min(value = 0, message = "最小风险等级不能小于0")
    @Max(value = 100, message = "最小风险等级不能大于100")
    private Integer riskLevelMin;

    @Schema(description = "最大风险等级，0-100，含边界")
    @Min(value = 0, message = "最大风险等级不能小于0")
    @Max(value = 100, message = "最大风险等级不能大于100")
    private Integer riskLevelMax;

    @Schema(description = "业务渠道：WEB/APP/MINI_PROGRAM/OPEN_API/DESKTOP/H5/BACKGROUND")
    @Size(max = 20, message = "业务渠道长度不能超过20个字符")
    private String bizChannel;

    @Schema(description = "客户端IP")
    @Size(max = 40, message = "客户端IP长度不能超过40个字符")
    private String clientIp;

    @Schema(description = "认证方式：PASSWORD/SMS/OAUTH2/LDAP/CERT 等")
    @Size(max = 20, message = "认证方式长度不能超过20个字符")
    private String authType;

    @Schema(description = "业务结果码")
    @Size(max = 50, message = "业务结果码长度不能超过50个字符")
    private String resultCode;

    @Schema(description = "开始时间，ISO-8601 格式，如 2026-09-17T00:00:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间，ISO-8601 格式，如 2026-09-17T23:59:59")
    private LocalDateTime endTime;

}
