package com.carlos.monitor.redis.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 大key/热key 报告明细 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "大key/热key报告明细视图对象")
public class BigkeyReportItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "报告ID")
    private Long reportId;

    @Schema(description = "节点地址（cluster 模式 key 所在分片）")
    private String nodeAddress;

    @Schema(description = "key 名")
    private String redisKey;

    @Schema(description = "类型：string/hash/list/set/zset/stream")
    private String keyType;

    @Schema(description = "内存占用（字节）")
    private Long memoryBytes;

    @Schema(description = "元素数量")
    private Long elementCount;

    @Schema(description = "TTL（秒），-1 永不过期")
    private Long ttlSeconds;

    @Schema(description = "访问频次（热key报告）")
    private Long accessFreq;

    @Schema(description = "内存为估算值（Redis < 4.0 无 MEMORY USAGE 降级）")
    private Boolean estimated;
}
