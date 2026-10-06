package com.carlos.monitor.redis.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * key 浏览结果 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@Schema(description = "key浏览结果视图对象")
public class KeyScanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "key 列表（展示编码）")
    private List<String> keys;

    @Schema(description = "下一游标（空串表示扫描结束）")
    private String nextCursor;

    @Schema(description = "本次扫描 key 数量（过滤前）")
    private Long scannedCount;

    @Schema(description = "是否扫描结束")
    private Boolean finished;
}
