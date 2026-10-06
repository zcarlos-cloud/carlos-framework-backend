package com.carlos.monitor.db.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * SQL 执行历史 分页查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SQL执行历史分页查询参数")
public class SqlHistoryPageParam extends ParamPage {

    @NotNull(message = "实例ID不能为空")
    @Schema(description = "实例ID")
    private Long instanceId;

    @Size(max = 20, message = "SQL 类型长度不能超过20")
    @Schema(description = "SQL 类型：SELECT/EXPLAIN/UPDATE/DELETE")
    private String sqlType;

    @Size(max = 20, message = "执行结果长度不能超过20")
    @Schema(description = "执行结果：SUCCESS/FAILED/BLOCKED")
    private String execStatus;

    @Schema(description = "执行人（用户ID）")
    private Long executeBy;

    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
