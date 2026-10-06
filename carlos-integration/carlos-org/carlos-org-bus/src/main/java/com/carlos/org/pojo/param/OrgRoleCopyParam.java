package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;


/**
 * <p>
 * 角色复制参数
 * </p>
 * <p>复制角色</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
public class OrgRoleCopyParam {

    /**
     * 源角色id
     */
    @NotNull(message = "源角色id不能为空")
    @Schema(description = "源角色id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Serializable sourceRoleId;

    /**
     * 新角色名称
     */
    @NotBlank(message = "新角色名称不能为空")
    @Size(max = 40, message = "新角色名称长度不能超过40")
    @Schema(description = "新角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newRoleName;

    /**
     * 新角色编码
     */
    // 与前端 src/constants/reg.ts 的 REG_BIZ_CODE 保持一致：字母开头，仅字母、数字、下划线、点、中划线，长度2-64
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "新角色编码须以字母开头，仅允许字母、数字、下划线、点、中划线，长度2-64")
    @Size(max = 100, message = "新角色编码长度不能超过100")
    @Schema(description = "新角色编码")
    private String newRoleCode;

}
