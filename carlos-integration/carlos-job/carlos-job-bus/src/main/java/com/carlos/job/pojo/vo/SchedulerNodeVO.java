package com.carlos.job.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 调度器集群节点 显示对象（来自 QRTZ_SCHEDULER_STATE）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Data
@Accessors(chain = true)
@Schema(description = "调度器集群节点")
public class SchedulerNodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "调度器名称")
    private String schedulerName;

    @Schema(description = "实例ID")
    private String instanceId;

    @Schema(description = "最近心跳时间(毫秒)")
    private Long lastCheckinTime;

    @Schema(description = "心跳间隔(毫秒)")
    private Long checkinInterval;

    @Schema(description = "是否存活(心跳在 2 倍间隔内)")
    private Boolean alive;

}
