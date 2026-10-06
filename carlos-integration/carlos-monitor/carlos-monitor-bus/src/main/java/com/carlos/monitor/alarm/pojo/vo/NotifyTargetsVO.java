package com.carlos.monitor.alarm.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 告警通知目标 显示层对象（monitor_alarm_rule.notify_targets JSON 转对象）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@Schema(description = "告警通知目标视图对象")
public class NotifyTargetsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "通知用户ID列表")
    private List<Long> userIds;

    @Schema(description = "通知角色ID列表")
    private List<Long> roleIds;
}
