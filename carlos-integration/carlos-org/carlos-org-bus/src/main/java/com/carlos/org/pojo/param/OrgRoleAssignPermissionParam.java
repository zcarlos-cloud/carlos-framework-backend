package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;


/**
 * <p>
 * 角色分配操作权限参数
 * </p>
 * <p>配置角色操作权限（sys_operation）</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
public class OrgRoleAssignPermissionParam {

    /**
     * 角色id
     */
    @NotNull(message = "角色id不能为空")
    @Schema(description = "角色id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long roleId;

    /**
     * 操作id列表
     */
    @NotEmpty(message = "操作id列表不能为空")
    @Schema(description = "操作id列表（sys_operation.id）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> operationIds;

}
