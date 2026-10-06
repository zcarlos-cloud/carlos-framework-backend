package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 部门 更新参数封装
 * </p>
 * <p>编辑部门</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@Schema(description = "部门修改参数")
public class OrgDepartmentUpdateParam {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空")
    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    /**
     * 父id
     */
    @Schema(description = "父id")
    private Long parentId;

    /**
     * 部门名称
     */
    @Size(max = 64, message = "部门名称长度不能超过64")
    @Schema(description = "部门名称")
    private String deptName;

    /**
     * 部门编号
     */
    // 与前端 src/constants/reg.ts 的 REG_BIZ_CODE 保持一致：字母开头，仅字母、数字、下划线、点、中划线，长度2-64
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "部门编号须以字母开头，仅允许字母、数字、下划线、点、中划线，长度2-64")
    @Size(max = 64, message = "部门编号长度不能超过64")
    @Schema(description = "部门编号")
    private String deptCode;

    /**
     * 负责人id
     */
    @Schema(description = "负责人id")
    private Long leaderId;

    /**
     * 排序
     */
    @Min(value = 0, message = "排序不能小于0")
    @Schema(description = "排序")
    private Integer orderNum;

    /**
     * 备注
     */
    @Size(max = 200, message = "备注长度不能超过200")
    @Schema(description = "备注")
    private String description;

}
