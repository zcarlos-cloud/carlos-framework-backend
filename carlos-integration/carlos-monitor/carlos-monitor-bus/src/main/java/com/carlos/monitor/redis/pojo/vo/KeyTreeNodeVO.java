package com.carlos.monitor.redis.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * key 树形节点 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@Schema(description = "key树形节点视图对象")
public class KeyTreeNodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "节点名（folder 为分隔段，leaf 为 key 末段；展示编码）")
    private String name;

    @Schema(description = "完整 key（仅 leaf；展示编码）")
    private String fullKey;

    @Schema(description = "Redis 类型（仅 leaf：string/hash/list/set/zset/stream）")
    private String type;

    @Schema(description = "是否叶子 key")
    private Boolean leaf;

    @Schema(description = "folder 下匹配 key 数（扫描未完整时为近似值）")
    private Long keyCount;
}
