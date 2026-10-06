package com.carlos.monitor.dashboard.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 监控大盘汇总 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@Schema(description = "监控大盘汇总视图对象")
public class DashboardSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "实例总数")
    private Integer totalInstances;

    @Schema(description = "健康实例数")
    private Integer upCount;

    @Schema(description = "异常实例数")
    private Integer downCount;

    @Schema(description = "未知状态实例数")
    private Integer unknownCount;

    @Schema(description = "按类型分组统计")
    private List<TypeStat> typeStats;

    @Schema(description = "按级别告警统计（未恢复）")
    private List<LevelStat> alarmStats;

    @Schema(description = "最近 1 小时有成功采集的实例数")
    private Integer collectedLastHour;

    @Schema(description = "采集停滞实例数（超过 10 分钟无成功采集）")
    private Integer staleCount;

    /**
     * 类型分组统计
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "类型分组统计")
    public static class TypeStat implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "实例类型")
        private String instanceType;

        @Schema(description = "总数")
        private Integer total;

        @Schema(description = "健康数")
        private Integer up;

        @Schema(description = "异常数")
        private Integer down;
    }

    /**
     * 级别告警统计
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "级别告警统计")
    public static class LevelStat implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "告警级别")
        private String alarmLevel;

        @Schema(description = "数量")
        private Long count;
    }
}
