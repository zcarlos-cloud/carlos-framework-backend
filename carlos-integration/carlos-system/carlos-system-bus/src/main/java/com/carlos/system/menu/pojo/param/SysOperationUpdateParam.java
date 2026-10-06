package com.carlos.system.menu.pojo.param;


import com.carlos.system.enums.OperationStatus;
import com.carlos.system.enums.OperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 统一操作表 更新参数封装
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Data
@Accessors(chain = true)
@Schema(description = "统一操作修改参数")
public class SysOperationUpdateParam {

    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "操作类型：1=按钮, 2=API")
    private OperationType opType;

    @Schema(description = "所属菜单ID")
    private Long menuId;

    @Size(max = 100, message = "操作名称长度不能超过100")
    @Schema(description = "操作名称")
    private String opName;

    @Size(max = 100, message = "权限码长度不能超过100")
    @Schema(description = "权限码，如 user:create")
    private String permCode;

    @Size(max = 100, message = "图标长度不能超过100")
    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer orderNum;

    @Size(max = 500, message = "API路径长度不能超过500")
    @Schema(description = "API路径（仅API）")
    private String apiPath;

    @Size(max = 10, message = "HTTP方法长度不能超过10")
    @Schema(description = "HTTP方法（仅API）")
    private String apiMethod;

    @Schema(description = "状态：0禁用 1启用")
    private OperationStatus opStatus;

}
