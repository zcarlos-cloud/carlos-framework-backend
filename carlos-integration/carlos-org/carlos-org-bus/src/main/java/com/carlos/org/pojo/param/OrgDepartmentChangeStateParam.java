package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * <p>
 * 部门状态变更参数
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
public class OrgDepartmentChangeStateParam {

    /**
     * 部门id
     */
    @NotNull(message = "部门id不能为空")
    @Schema(description = "部门id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    /**
     * 状态，0：禁用，1：启用
     */
    @NotNull(message = "状态不能为空")
    @Schema(description = "状态，0：禁用，1：启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer deptStatus;

}
