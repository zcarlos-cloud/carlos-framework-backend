package com.carlos.monitor.redis.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 指标时序点 显示层对象（原始点只有 ts/value；聚合点带 avg/max/min/p95/count）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "指标时序点视图对象")
public class HistoryPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "时间点（原始点 ts / 聚合点 hour）")
    private String ts;

    @Schema(description = "原始值")
    private Double value;

    @Schema(description = "平均值（聚合）")
    private Double avgValue;

    @Schema(description = "最大值（聚合）")
    private Double maxValue;

    @Schema(description = "最小值（聚合）")
    private Double minValue;

    @Schema(description = "P95 值（聚合）")
    private Double p95Value;

    @Schema(description = "样本点数（聚合）")
    private Long pointCount;
}
