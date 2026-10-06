package com.carlos.monitor.redis.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Map;

/**
 * <p>
 * Redis 当前指标快照 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@Schema(description = "Redis当前指标快照视图对象")
public class MetricCurrentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "指标名（含节点后缀）→ {v, ts} 快照值")
    private Map<String, String> metrics;
}
