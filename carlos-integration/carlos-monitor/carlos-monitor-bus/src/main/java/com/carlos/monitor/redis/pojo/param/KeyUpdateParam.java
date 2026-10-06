package com.carlos.monitor.redis.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * Redis key 修改参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@Schema(description = "Redis key修改参数")
public class KeyUpdateParam {

    @NotBlank(message = "key 不能为空")
    @Size(max = 500, message = "key 长度不能超过500")
    @Schema(description = "key")
    private String key;

    @NotBlank(message = "操作类型不能为空")
    @Schema(description = "操作：set-value/hash-put/add-element/remove-element/expire/persist/rename")
    private String operation;

    @Size(max = 20, message = "类型长度不能超过20")
    @Schema(description = "key 类型（remove-element 时必填：hash/list/set/zset）")
    private String type;

    @Size(max = 10000, message = "值长度不能超过10000")
    @Schema(description = "值/新元素")
    private String value;

    @Size(max = 500, message = "字段长度不能超过500")
    @Schema(description = "hash 字段名")
    private String field;

    @Size(max = 500, message = "元素长度不能超过500")
    @Schema(description = "待删除元素（remove-element）")
    private String element;

    @Schema(description = "zset 分数")
    private Double score;

    @Schema(description = "过期时间（秒，expire 操作）")
    private Long ttlSeconds;

    @Size(max = 500, message = "新 key 长度不能超过500")
    @Schema(description = "新 key 名（rename 操作）")
    private String newKey;
}
