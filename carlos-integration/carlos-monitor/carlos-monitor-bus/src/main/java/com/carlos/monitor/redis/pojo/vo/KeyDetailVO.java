package com.carlos.monitor.redis.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * key 详情 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "key详情视图对象")
public class KeyDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "key（展示编码）")
    private String key;

    @Schema(description = "类型：string/hash/list/set/zset/stream")
    private String type;

    @Schema(description = "TTL（秒），-1 永不过期")
    private Long ttlSeconds;

    @Schema(description = "内存占用（字节，Redis < 4.0 为空）")
    private Long memoryBytes;

    @Schema(description = "元素数量")
    private Long elementCount;

    @Schema(description = "值（按类型分页返回，敏感字段已按规则脱敏）")
    private Object value;

    @Schema(description = "值子游标（空串表示结束；list 为偏移量）")
    private String subCursor;

    @Schema(description = "值是否读取完毕")
    private Boolean subFinished;
}
