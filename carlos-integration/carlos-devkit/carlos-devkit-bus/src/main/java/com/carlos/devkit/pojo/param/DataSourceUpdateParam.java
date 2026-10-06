package com.carlos.devkit.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 数据源编辑参数
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "数据源编辑参数")
public class DataSourceUpdateParam extends DataSourceCreateParam {

    @NotNull(message = "数据源ID不能为空")
    @Schema(description = "数据源ID")
    private Long id;

    @Schema(description = "状态：1=启用 0=停用")
    private Integer status;
}
