package com.carlos.monitor.db.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * SQL 控制台执行 参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Data
@Accessors(chain = true)
@Schema(description = "SQL控制台执行参数")
public class SqlExecuteParam {

    @NotBlank(message = "SQL 不能为空")
    @Size(max = 10000, message = "SQL 长度不能超过10000")
    @Schema(description = "执行 SQL（单语句）")
    private String sql;
}
