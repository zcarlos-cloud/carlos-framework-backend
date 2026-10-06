package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 部门角色 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@Schema(description = "部门角色新增参数")
public class OrgDepartmentRoleCreateParam {
    @NotNull(message = "部门id不能为空")
    @Schema(description = "部门id")
    private Long deptId;
    @NotNull(message = "角色id不能为空")
    @Schema(description = "角色id")
    private Long roleId;
    @NotNull(message = "是否为默认角色不能为空")
    @Schema(description = "是否为默认角色")
    private Boolean defaultRole;
    @Schema(description = "租户id")
    private Long tenantId;
}
