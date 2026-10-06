package com.carlos.system.menu.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 功能模块 分页查询参数
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "功能模块分页查询参数")
public class SysModulePageParam extends ParamPage {

    @Size(max = 100, message = "模块名称长度不能超过100")
    @Schema(description = "模块名称")
    private String moduleName;

    @Size(max = 100, message = "模块编码长度不能超过100")
    @Schema(description = "模块编码")
    private String moduleCode;

    @Schema(description = "状态：true启用 false禁用")
    private Boolean moduleStatus;

}
