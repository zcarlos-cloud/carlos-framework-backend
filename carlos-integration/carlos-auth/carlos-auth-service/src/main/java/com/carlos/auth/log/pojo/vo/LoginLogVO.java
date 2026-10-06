package com.carlos.auth.log.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 登录日志 视图对象
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "登录日志")
public class LoginLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "登录结果（SUCCESS-成功 FAIL-失败）")
    private String logStatus;

    @Schema(description = "登录时间")
    private LocalDateTime loginTime;

    @Schema(description = "客户端IP")
    private String ip;

    @Schema(description = "登录地点（国家/省/市拼接）")
    private String location;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "认证方式（PASSWORD/SMS/OAUTH2 等）")
    private String authType;

    @Schema(description = "结果描述")
    private String resultMessage;

    @Schema(description = "耗时（毫秒）")
    private Integer durationMs;

}
