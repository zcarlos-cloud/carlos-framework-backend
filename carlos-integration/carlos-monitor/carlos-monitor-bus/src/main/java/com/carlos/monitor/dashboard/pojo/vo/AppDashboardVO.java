package com.carlos.monitor.dashboard.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * <p>
 * 应用视角大盘 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@Schema(description = "应用视角大盘视图对象")
public class AppDashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "应用名")
    private String appName;

    @Schema(description = "关联实例（含健康与关键当前值）")
    private java.util.List<AppInstanceVO> instances;

    /**
     * 应用关联实例
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "应用关联实例")
    public static class AppInstanceVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "实例ID")
        private Long instanceId;

        @Schema(description = "实例名称")
        private String instanceName;

        @Schema(description = "实例类型")
        private String instanceType;

        @Schema(description = "健康状态：UNKNOWN/UP/DOWN")
        private String healthStatus;

        @Schema(description = "最近成功采集时间")
        private LocalDateTime lastCollectTime;

        @Schema(description = "关键当前指标（指标名 → {v,ts}）")
        private Map<String, String> metrics;
    }
}
