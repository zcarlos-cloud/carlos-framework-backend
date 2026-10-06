package com.carlos.monitor.dashboard.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 大盘趋势 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@Schema(description = "大盘趋势视图对象")
public class DashboardTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "指标名")
    private String metricName;

    @Schema(description = "范围")
    private String range;

    @Schema(description = "Top N 实例序列")
    private List<InstanceSeries> series;

    /**
     * 实例趋势序列
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "实例趋势序列")
    public static class InstanceSeries implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "实例ID")
        private Long instanceId;

        @Schema(description = "实例名称")
        private String instanceName;

        @Schema(description = "趋势点（小时聚合均值）")
        private List<Point> points;
    }

    /**
     * 趋势点
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "趋势点")
    public static class Point implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "时间")
        private String ts;

        @Schema(description = "均值")
        private Double value;
    }
}
