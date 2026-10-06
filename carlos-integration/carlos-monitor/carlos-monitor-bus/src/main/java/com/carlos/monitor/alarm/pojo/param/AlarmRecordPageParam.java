package com.carlos.monitor.alarm.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 告警记录 分页查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "告警记录分页查询参数")
public class AlarmRecordPageParam extends ParamPage {

    @Schema(description = "状态：FIRING/CONFIRMED/RECOVERED")
    private String alarmStatus;

    @Schema(description = "级别：INFO/WARN/CRITICAL")
    private String alarmLevel;

    @Schema(description = "实例ID")
    private Long instanceId;

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
