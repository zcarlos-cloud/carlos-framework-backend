package com.carlos.auth.log.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 登录日志 详情视图对象（在列表字段基础上扩展客户端/认证/链路上下文）
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "登录日志详情")
public class LoginLogDetailVO extends LoginLogVO {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private String principalId;

    @Schema(description = "主体类型：USER-用户/SERVICE-服务/SYSTEM-系统/ANONYMOUS-匿名")
    private String principalType;

    @Schema(description = "认证源：LOCAL-本地/WECHAT-微信/DINGTALK-钉钉 等")
    private String authProvider;

    @Schema(description = "登录渠道：WEB-网页/APP-移动应用/MINI_PROGRAM-小程序 等")
    private String bizChannel;

    @Schema(description = "客户端端口")
    private Integer clientPort;

    @Schema(description = "处理服务器IP")
    private String serverIp;

    @Schema(description = "设备指纹")
    private String deviceFingerprint;

    @Schema(description = "风险等级 0-100")
    private Integer riskLevel;

    @Schema(description = "业务结果码")
    private String resultCode;

    @Schema(description = "客户端时间")
    private LocalDateTime clientTime;

    @Schema(description = "服务器时间")
    private LocalDateTime serverTime;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "全链路追踪ID")
    private String traceId;

    @Schema(description = "应用名")
    private String appName;

}
