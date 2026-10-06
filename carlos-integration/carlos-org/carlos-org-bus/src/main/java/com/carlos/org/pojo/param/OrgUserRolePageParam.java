package com.carlos.org.pojo.param;


import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * <p>
 * 用户角色 列表查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户角色列表查询参数")
public class OrgUserRolePageParam extends ParamPage {
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
    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
