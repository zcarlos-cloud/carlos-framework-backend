package com.carlos.org.pojo.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户角色VO
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户角色VO")
public class OrgUserRoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关联ID")
    private Long id;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色生效的部门id（ADR-002 作用域），空表示全局")
    private Long deptId;

    @Schema(description = "授权失效时间，空表示长期有效")
    private LocalDateTime expireTime;

}
