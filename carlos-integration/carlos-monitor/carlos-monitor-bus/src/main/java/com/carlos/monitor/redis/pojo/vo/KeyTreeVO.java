package com.carlos.monitor.redis.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * key 树形分级浏览结果 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@Schema(description = "key树形分级浏览结果视图对象")
public class KeyTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前层节点（folder 在前、leaf 在后，按名称排序）")
    private List<KeyTreeNodeVO> nodes;

    @Schema(description = "下一游标（空串表示扫描结束）")
    private String nextCursor;

    @Schema(description = "本次扫描 key 数量（过滤前）")
    private Long scannedCount;

    @Schema(description = "是否扫描结束")
    private Boolean finished;

    @Schema(description = "folder 计数是否为近似值（扫描未完整时为 true）")
    private Boolean approximate;
}
