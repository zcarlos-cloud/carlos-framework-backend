package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;


/**
 * <p>
 * 角色设置数据权限参数
 * </p>
 * <p>配置数据权限</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
public class OrgRoleSetDataScopeParam {

    /**
     * 角色id
     */
    @NotNull(message = "角色id不能为空")
    @Schema(description = "角色id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Serializable roleId;

    /**
     * 数据范围，1：全部, 2: 本部及子部门, 3:仅本部门, 4:仅本人, 5:自定义规则
     */
    @NotNull(message = "数据范围不能为空")
    @Min(value = 1, message = "数据范围取值 1-5")
    @Max(value = 5, message = "数据范围取值 1-5")
    @Schema(description = "数据范围，1：全部, 2: 本部及子部门, 3:仅本部门, 4:仅本人, 5:自定义规则", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer dataScope;

}
