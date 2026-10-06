package com.carlos.monitor.alarm.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 告警规则 分页查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "告警规则分页查询参数")
public class AlarmRulePageParam extends ParamPage {

    @Size(max = 100, message = "规则名称长度不能超过100")
    @Schema(description = "规则名称（模糊）")
    private String ruleName;

    @Schema(description = "指标来源：REDIS/DB/SERVICE")
    private String metricSource;

    @Schema(description = "告警级别：INFO/WARN/CRITICAL")
    private String alarmLevel;

    @Schema(description = "启用开关")
    private Boolean enabled;
}
