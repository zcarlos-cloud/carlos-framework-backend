package com.carlos.org.pojo.param;


import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * <p>
 * 部门角色 列表查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "部门角色列表查询参数")
public class OrgDepartmentRolePageParam extends ParamPage {
    @Schema(description = "部门id")
    private Long deptId;
    @Schema(description = "角色id")
    private Long roleId;
    @Schema(description = "是否为默认角色")
    private Boolean defaultRole;
    @Schema(description = "租户id")
    private Long tenantId;
    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
