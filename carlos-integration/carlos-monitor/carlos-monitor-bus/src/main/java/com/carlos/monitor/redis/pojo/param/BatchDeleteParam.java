package com.carlos.monitor.redis.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * Redis key 批量删除参数封装（高危，必须携带 X-Confirm-Token）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@Schema(description = "Redis key批量删除参数")
public class BatchDeleteParam {

    @Size(max = 200, message = "匹配模式长度不能超过200")
    @Schema(description = "匹配模式（与 keys 二选一，如 dept:*）")
    private String pattern;

    @Schema(description = "显式 key 列表（与 pattern 二选一）")
    private List<String> keys;
}
