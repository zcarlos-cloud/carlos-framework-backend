package com.carlos.auth.audit.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 安全告警处理参数
 * </p>
 *
 * @author Carlos
 * @date 2026-04-08
 */
@Data
@Schema(description = "安全告警处理参数")
public class SecurityAlertHandleParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "告警ID不能为空")
    @Schema(description = "告警ID", required = true)
    private Long alertId;

    @Schema(description = "处理备注")
    private String remark;
}
