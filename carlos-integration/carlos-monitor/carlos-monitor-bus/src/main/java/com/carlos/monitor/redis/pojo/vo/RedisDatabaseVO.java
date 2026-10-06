package com.carlos.monitor.redis.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * Redis 逻辑库信息 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@Schema(description = "Redis逻辑库信息视图对象")
public class RedisDatabaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "库序号（0 起）")
    private Integer index;

    @Schema(description = "key 数量")
    private Long keyCount;

    @Schema(description = "设置了过期时间的 key 数量")
    private Long expires;

    @Schema(description = "平均 TTL（毫秒，无过期 key 时为 0）")
    private Long avgTtl;

    @Schema(description = "库总数（CONFIG GET databases，无权限回退 16）")
    private Integer totalDbs;
}
