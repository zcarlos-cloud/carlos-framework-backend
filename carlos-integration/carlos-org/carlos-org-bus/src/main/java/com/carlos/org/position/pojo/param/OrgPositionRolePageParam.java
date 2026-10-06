package com.carlos.org.position.pojo.param;


import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * <p>
 * 岗位角色关联表（岗位默认权限配置） 列表查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "岗位角色关联表（岗位默认权限配置）列表查询参数")
public class OrgPositionRolePageParam extends ParamPage {
    @Schema(description = "岗位ID")
    private Long positionId;
    @Schema(description = "角色ID")
    private Long roleId;
    @Schema(description = "是否默认角色：1是（入职自动获得），0否（可选）")
    private Boolean defaultRole;
    @Schema(description = "状态：0停用，1启用")
    private Integer positionRoleStatus;
    @Schema(description = "租户ID")
    private Long tenantId;
    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
