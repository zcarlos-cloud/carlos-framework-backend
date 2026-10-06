package com.carlos.org.pojo.param;


import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * <p>
 * 角色分页查询参数
 * </p>
 * <p>角色列表</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrgRolePageParam extends ParamPage {

    /**
     * 角色名称
     */
    @Size(max = 40, message = "角色名称长度不能超过40")
    @Schema(description = "角色名称")
    private String roleName;

    /**
     * 角色编码
     */
    @Size(max = 100, message = "角色编码长度不能超过100")
    @Schema(description = "角色编码")
    private String roleCode;

    /**
     * 角色类型，1：系统角色, 2: 自定义角色
     */
    @Schema(description = "角色类型，1：系统角色, 2: 自定义角色")
    private Integer roleType;

    /**
     * 角色状态，0：禁用, 1: 启用
     */
    @Schema(description = "角色状态，0：禁用, 1: 启用")
    private Integer roleStatus;

}
