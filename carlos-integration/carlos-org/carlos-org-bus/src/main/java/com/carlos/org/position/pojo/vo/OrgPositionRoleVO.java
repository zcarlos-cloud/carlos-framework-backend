package com.carlos.org.position.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 岗位角色关联表（岗位默认权限配置） 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgPositionRoleVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "岗位ID")
    private Long positionId;
    @Schema(description = "角色ID")
    private Long roleId;
    @Schema(description = "是否默认角色：1是（入职自动获得），0否（可选）")
    private Boolean defaultRole;
    @Schema(description = "状态：0停用，1启用")
    private Integer positionRoleStatus;
    @Schema(description = "创建者")
    private Long createBy;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "修改者")
    private Long updateBy;
    @Schema(description = "修改时间")
    private LocalDateTime updateTime;
    @Schema(description = "租户ID")
    private Long tenantId;

}
