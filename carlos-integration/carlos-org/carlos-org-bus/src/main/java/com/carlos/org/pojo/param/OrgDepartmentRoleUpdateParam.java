package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 部门角色 更新参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@Schema(description = "部门角色修改参数")
public class OrgDepartmentRoleUpdateParam {
    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "部门id")
    private Long deptId;
    @Schema(description = "角色id")
    private Long roleId;
    @Schema(description = "是否为默认角色")
    private Boolean defaultRole;
    @Schema(description = "租户id")
    private Long tenantId;
}
