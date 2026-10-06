package com.carlos.org.position.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 岗位角色关联表（岗位默认权限配置） 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@Schema(description = "岗位角色关联表（岗位默认权限配置）新增参数")
public class OrgPositionRoleCreateParam {
    @NotNull(message = "岗位ID不能为空")
    @Schema(description = "岗位ID")
    private Long positionId;
    @NotNull(message = "角色ID不能为空")
    @Schema(description = "角色ID")
    private Long roleId;
    @NotNull(message = "是否默认角色：1是（入职自动获得），0否（可选）不能为空")
    @Schema(description = "是否默认角色：1是（入职自动获得），0否（可选）")
    private Boolean defaultRole;
    @NotNull(message = "状态：0停用，1启用不能为空")
    @Schema(description = "状态：0停用，1启用")
    private Integer positionRoleStatus;
    @Schema(description = "租户ID")
    private Long tenantId;
}
