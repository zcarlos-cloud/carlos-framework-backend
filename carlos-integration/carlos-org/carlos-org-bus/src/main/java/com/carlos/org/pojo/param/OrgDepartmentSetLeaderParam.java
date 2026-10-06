package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * <p>
 * 设置部门负责人参数
 * </p>
 * <p>设置部门负责人</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
public class OrgDepartmentSetLeaderParam {

    /**
     * 部门id
     */
    @NotNull(message = "部门id不能为空")
    @Schema(description = "部门id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long deptId;

    /**
     * 负责人id
     */
    @NotNull(message = "负责人id不能为空")
    @Schema(description = "负责人id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long leaderId;

}
