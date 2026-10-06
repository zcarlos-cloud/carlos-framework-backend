package com.carlos.monitor.alarm.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 告警静默 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@Schema(description = "告警静默新增参数")
public class AlarmSilenceCreateParam {

    @NotNull(message = "规则ID不能为空")
    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "实例ID（空表示该规则下全部实例静默）")
    private Long instanceId;

    @NotNull(message = "静默开始时间不能为空")
    @Schema(description = "静默开始时间")
    private LocalDateTime silenceStart;

    @NotNull(message = "静默结束时间不能为空")
    @Schema(description = "静默结束时间")
    private LocalDateTime silenceEnd;

    @Size(max = 500, message = "静默原因长度不能超过500")
    @Schema(description = "静默原因")
    private String reason;
}
