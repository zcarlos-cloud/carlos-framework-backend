package com.carlos.monitor.db.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * SQL 执行历史 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "SQL执行历史视图对象")
public class SqlHistoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "数据库实例ID")
    private Long instanceId;

    @Schema(description = "执行人（用户ID）")
    private Long executeBy;

    @Schema(description = "执行 SQL（参数已脱敏）")
    private String sqlText;

    @Schema(description = "SQL 类型：SELECT/EXPLAIN/UPDATE/DELETE")
    private String sqlType;

    @Schema(description = "执行结果：SUCCESS/FAILED/BLOCKED")
    private String execStatus;

    @Schema(description = "被拦截原因")
    private String blockReason;

    @Schema(description = "影响行数")
    private Integer affectedRows;

    @Schema(description = "返回行数")
    private Integer resultRows;

    @Schema(description = "执行耗时（毫秒）")
    private Long durationMs;

    @Schema(description = "来源 IP")
    private String clientIp;

    @Schema(description = "执行时间")
    private LocalDateTime executeTime;
}
