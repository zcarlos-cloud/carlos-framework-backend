package com.carlos.monitor.db.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * SQL 控制台执行结果 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "SQL控制台执行结果视图对象")
public class SqlExecResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "列名列表")
    private List<String> columns;

    @Schema(description = "数据行（敏感列已按规则脱敏）")
    private List<Map<String, Object>> rows;

    @Schema(description = "影响行数（写执行）")
    private Integer affectedRows;

    @Schema(description = "是否因 max-limit 截断")
    private Boolean truncated;
}
