package com.carlos.monitor.redis.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * Redis key 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@Schema(description = "Redis key新增参数")
public class KeyCreateParam {

    @NotBlank(message = "key 不能为空")
    @Size(max = 500, message = "key 长度不能超过500")
    @Schema(description = "key")
    private String key;

    @NotBlank(message = "类型不能为空")
    @Schema(description = "类型：string/hash/list/set/zset")
    private String type;

    @Size(max = 10000, message = "值长度不能超过10000")
    @Schema(description = "值（string 必填；hash 为字段值；list/set/zset 为首个元素）")
    private String value;

    @Size(max = 500, message = "字段长度不能超过500")
    @Schema(description = "hash 字段名（type=hash 必填）")
    private String field;

    @Schema(description = "zset 分数（type=zset 必填）")
    private Double score;

    @Schema(description = "TTL（秒），空或 <=0 表示不过期")
    private Long ttlSeconds;
}
