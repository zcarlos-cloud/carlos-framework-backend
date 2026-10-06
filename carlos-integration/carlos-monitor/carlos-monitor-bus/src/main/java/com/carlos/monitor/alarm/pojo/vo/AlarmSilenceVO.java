package com.carlos.monitor.alarm.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 告警静默 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "告警静默视图对象")
public class AlarmSilenceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "静默ID")
    private Long id;

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "实例ID（空表示规则下全部实例）")
    private Long instanceId;

    @Schema(description = "静默开始时间")
    private LocalDateTime silenceStart;

    @Schema(description = "静默结束时间")
    private LocalDateTime silenceEnd;

    @Schema(description = "静默原因")
    private String reason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
