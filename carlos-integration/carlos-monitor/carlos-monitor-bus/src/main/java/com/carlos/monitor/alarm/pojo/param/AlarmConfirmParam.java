package com.carlos.monitor.alarm.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 告警确认 参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@Schema(description = "告警确认参数")
public class AlarmConfirmParam {

    @Size(max = 1000, message = "处理备注长度不能超过1000")
    @Schema(description = "处理备注")
    private String handleRemark;
}
