package com.carlos.monitor.db.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 库表容量快照 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "库表容量快照视图对象")
public class CapacitySnapshotVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "快照日期")
    private LocalDate snapshotDate;

    @Schema(description = "模式名")
    private String schemaName;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "行数（估计值）")
    private Long tableRows;

    @Schema(description = "表大小（字节）")
    private Long tableSizeBytes;

    @Schema(description = "索引大小（字节）")
    private Long indexSizeBytes;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
