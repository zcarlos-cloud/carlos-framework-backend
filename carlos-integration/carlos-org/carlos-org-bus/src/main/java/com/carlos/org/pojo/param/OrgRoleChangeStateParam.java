package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;


/**
 * <p>
 * 角色状态变更参数
 * </p>
 * <p>启用/禁用角色</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
public class OrgRoleChangeStateParam {

    /**
     * 角色id
     * <p>声明为 Serializable：前端把雪花id作为字符串下发，Jackson 落成 String；
     * 由 OrgRoleService.changeState 统一 Convert.toLong，避免下游 selectById 强转失败</p>
     */
    @NotNull(message = "角色id不能为空")
    @Schema(description = "角色id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Serializable id;

    /**
     * 状态，0：禁用, 1: 启用
     */
    @NotNull(message = "状态不能为空")
    @Schema(description = "状态，0：禁用, 1: 启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer roleStatus;

}
