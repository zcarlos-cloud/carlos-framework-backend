package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户角色 更新参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户角色修改参数")
public class OrgUserRoleUpdateParam {
    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "用户id")
    private Long userId;
    @Schema(description = "角色id")
    private Long roleId;
    @Schema(description = "角色生效的部门id")
    private Long deptId;
    @Schema(description = "失效时间")
    private LocalDateTime expireTime;
    @Schema(description = "租户id")
    private Long tenantId;
}
