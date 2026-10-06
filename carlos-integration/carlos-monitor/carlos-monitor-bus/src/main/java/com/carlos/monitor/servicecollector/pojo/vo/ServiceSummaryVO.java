package com.carlos.monitor.servicecollector.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 微服务清单项 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "微服务清单项视图对象")
public class ServiceSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "服务名")
    private String serviceName;

    @Schema(description = "monitor_instance ID")
    private Long instanceId;

    @Schema(description = "实例数（Nacos 实时）")
    private Integer instanceCount;

    @Schema(description = "健康实例数（actuator UP）")
    private Integer healthyCount;

    @Schema(description = "采集健康状态：UNKNOWN/UP/DOWN")
    private String healthStatus;

    @Schema(description = "最近成功采集时间")
    private LocalDateTime lastCollectTime;

    @Schema(description = "是否已停用（从 Nacos 消失）")
    private Boolean disabled;
}
