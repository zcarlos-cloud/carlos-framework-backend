package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 角色 新增参数封装
 * </p>
 * <p>新增角色</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@Schema(description = "角色新增参数")
public class OrgRoleCreateParam {

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 40, message = "角色名称长度不能超过40")
    @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleName;

    /**
     * 角色编码
     */
    // 与前端 src/constants/reg.ts 的 REG_BIZ_CODE 保持一致：字母开头，仅字母、数字、下划线、点、中划线，长度2-64
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "角色编码须以字母开头，仅允许字母、数字、下划线、点、中划线，长度2-64")
    @Size(max = 100, message = "角色编码长度不能超过100")
    @Schema(description = "角色编码")
    private String roleCode;

    /**
     * 数据范围，1：全部, 2: 本部及子部门, 3:仅本部门, 4:仅本人, 5:自定义规则
     * <p>为空时由 Service 层默认设置为「仅本人」（见 OrgRoleService.addOrgRole）</p>
     */
    @Min(value = 1, message = "数据范围取值 1-5")
    @Max(value = 5, message = "数据范围取值 1-5")
    @Schema(description = "数据范围，1：全部, 2: 本部及子部门, 3:仅本部门, 4:仅本人, 5:自定义规则")
    private Integer dataScope;

    /**
     * 备注
     */
    @Size(max = 200, message = "备注长度不能超过200")
    @Schema(description = "备注")
    private String description;

}
