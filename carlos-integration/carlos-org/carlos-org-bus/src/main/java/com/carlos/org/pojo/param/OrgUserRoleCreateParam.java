package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * <p>
 * 用户角色 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户角色新增参数")
public class OrgUserRoleCreateParam {
    @NotNull(message = "用户id不能为空")
    @Schema(description = "用户id")
    private Long userId;
    @NotNull(message = "角色id不能为空")
    @Schema(description = "角色id")
    private Long roleId;
    @Schema(description = "角色生效的部门id")
    private Long deptId;
    @Schema(description = "失效时间")
    private LocalDateTime expireTime;
    @Schema(description = "租户id")
    private Long tenantId;
}
