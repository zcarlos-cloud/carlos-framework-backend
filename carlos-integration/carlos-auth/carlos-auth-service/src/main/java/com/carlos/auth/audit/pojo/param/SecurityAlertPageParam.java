package com.carlos.auth.audit.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 安全告警分页查询参数
 * </p>
 *
 * @author Carlos
 * @date 2026-04-08
 */
@Data
@Schema(description = "安全告警分页查询参数")
public class SecurityAlertPageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "告警类型")
    private String alertType;

    @Schema(description = "告警级别")
    private String severity;

    @Schema(description = "是否已处理")
    private Boolean handled;
}
