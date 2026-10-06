package com.carlos.job.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 调度运行报表 显示对象（Dashboard 聚合）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Data
@Accessors(chain = true)
@Schema(description = "调度运行报表")
public class JobDashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "任务总数")
    private Long jobCount;

    @Schema(description = "在线调度器节点数")
    private Long onlineNodeCount;

    @Schema(description = "今日触发次数")
    private Long todayTriggerCount;

    @Schema(description = "今日成功次数")
    private Long todaySuccessCount;

    @Schema(description = "今日失败次数")
    private Long todayFailCount;

    @Schema(description = "今日成功率(0-100)")
    private Double todaySuccessRate;

    @Schema(description = "当前正在执行的任务数")
    private Integer runningJobCount;

    @Schema(description = "日期分布(近 N 天每日成功/失败)")
    private List<DateDistribution> dateDistribution;

    @Schema(description = "执行状态分布(饼图)")
    private List<StatusDistribution> statusDistribution;

    /**
     * 日期分布点
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "日期分布点")
    public static class DateDistribution implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "日期(yyyy-MM-dd)")
        private String date;

        @Schema(description = "成功次数")
        private Long successCount;

        @Schema(description = "失败次数")
        private Long failCount;

    }

    /**
     * 执行状态分布点
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "执行状态分布点")
    public static class StatusDistribution implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "执行状态: 1-成功 2-失败 3-执行中")
        private Integer status;

        @Schema(description = "状态名")
        private String statusName;

        @Schema(description = "次数")
        private Long count;

    }

}
