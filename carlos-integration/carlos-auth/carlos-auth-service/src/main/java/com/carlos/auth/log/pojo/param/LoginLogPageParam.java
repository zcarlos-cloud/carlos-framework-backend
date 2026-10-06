package com.carlos.auth.log.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 登录日志 分页查询参数
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "登录日志分页查询参数")
public class LoginLogPageParam extends ParamPage {

    @Size(max = 64, message = "用户名长度不能超过64")
    @Schema(description = "用户名（模糊匹配）")
    private String username;

    @Pattern(regexp = "SUCCESS|FAIL", message = "登录结果只支持 SUCCESS/FAIL")
    @Schema(description = "登录结果（SUCCESS-成功 FAIL-失败，空为全部）")
    private String logStatus;

    @Size(max = 64, message = "客户端IP长度不能超过64")
    @Schema(description = "客户端IP（等值匹配）")
    private String ip;

    @Size(max = 32, message = "认证方式长度不能超过32")
    @Schema(description = "认证方式（PASSWORD-密码/SMS-短信/OAUTH2-OAuth2 等，空为全部）")
    private String authType;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @AssertTrue(message = "开始时间不能晚于结束时间")
    @Schema(hidden = true)
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || !startTime.isAfter(endTime);
    }

}
