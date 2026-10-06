package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 用户分配角色参数
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户分配角色参数")
public class OrgUserAssignRoleParam {

    @NotNull(message = "用户id不能为空")
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 角色ID列表；整表替换语义。允许为空数组——表示清空该用户全部角色
     * （见 OrgUserService.assignRoles：先删旧关联，空列表不再插入）。
     * 前端编辑用户时取消勾选所有角色即提交空数组，故不能用 @NotEmpty
     */
    @Schema(description = "角色ID列表，整表替换；空数组表示清空全部角色")
    private List<Long> roleIds;

    @Future(message = "失效时间必须晚于当前时间")
    @Schema(description = "授权失效时间（临时授权，对本次角色列表统一生效），空表示长期有效")
    private LocalDateTime expireTime;

}
