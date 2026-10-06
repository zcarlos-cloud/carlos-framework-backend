package com.carlos.devkit.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 数据源分页查询参数
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "数据源分页查询参数")
public class DataSourcePageParam extends ParamPage {

    @Size(max = 128, message = "数据源名称长度不能超过128")
    @Schema(description = "数据源名称（模糊）")
    private String dsName;

    @Schema(description = "数据库类型：MYSQL/POSTGRESQL")
    private String dbType;

    @Schema(description = "状态：1=启用 0=停用")
    private Integer status;
}
