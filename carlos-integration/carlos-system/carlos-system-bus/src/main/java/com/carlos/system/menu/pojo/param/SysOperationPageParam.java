package com.carlos.system.menu.pojo.param;


import com.carlos.core.param.ParamPage;
import com.carlos.system.enums.OperationStatus;
import com.carlos.system.enums.OperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 统一操作表 分页列表查询参数
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "统一操作分页列表查询参数")
public class SysOperationPageParam extends ParamPage {

    @Schema(description = "操作类型：1=按钮, 2=API")
    private OperationType opType;

    @Schema(description = "所属菜单ID")
    private Long menuId;

    @Size(max = 100, message = "操作名称长度不能超过100")
    @Schema(description = "操作名称")
    private String opName;

    @Size(max = 100, message = "权限码长度不能超过100")
    @Schema(description = "权限码")
    private String permCode;

    @Size(max = 500, message = "API路径长度不能超过500")
    @Schema(description = "API路径")
    private String apiPath;

    @Schema(description = "状态：0禁用 1启用")
    private OperationStatus opStatus;

}
