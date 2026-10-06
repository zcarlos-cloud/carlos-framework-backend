package com.carlos.auth.audit.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 安全告警 VO
 * </p>
 *
 * <p>显示层对象，响应给前端</p>
 *
 * @author Carlos
 * @date 2026-04-08
 */
@Data
@Accessors(chain = true)
@Schema(description = "安全告警信息")
public class SecurityAlertVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "告警ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "告警类型")
    private String alertType;

    @Schema(description = "告警类型名称")
    private String alertTypeName;

    @Schema(description = "告警级别")
    private String severity;

    @Schema(description = "告警级别名称")
    private String severityName;

    @Schema(description = "告警标题")
    private String title;

    @Schema(description = "告警内容")
    private String content;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "地理位置")
    private String location;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "是否已处理")
    private Boolean handled;

    @Schema(description = "处理时间")
    private LocalDateTime handledTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
