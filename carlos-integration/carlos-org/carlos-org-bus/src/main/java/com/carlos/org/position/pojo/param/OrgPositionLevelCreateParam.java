package com.carlos.org.position.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;


/**
 * <p>
 * 职级表 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@Schema(description = "职级表新增参数")
public class OrgPositionLevelCreateParam {
    @NotNull(message = "所属职系ID不能为空")
    @Schema(description = "所属职系ID")
    private Long categoryId;
    @NotBlank(message = "职级编码，如：T1、T2-1、M3不能为空")
    // 与前端 src/constants/reg.ts 的 REG_BIZ_CODE 保持一致：字母开头，仅字母、数字、下划线、点、中划线，长度2-64
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "职级编码须以字母开头，仅允许字母、数字、下划线、点、中划线，长度2-64")
    @Size(max = 20, message = "职级编码长度不能超过20")
    @Schema(description = "职级编码，如：T1、T2-1、M3")
    private String levelCode;
    @NotBlank(message = "职级名称，如：初级工程师、高级工程师、部门经理不能为空")
    @Size(max = 50, message = "职级名称长度不能超过50")
    @Schema(description = "职级名称，如：初级工程师、高级工程师、部门经理")
    private String levelName;
    @NotNull(message = "职级序列号，用于排序和比较，如：1、2、3不能为空")
    @Schema(description = "职级序列号，用于排序和比较，如：1、2、3")
    private Integer levelSeq;
    @Size(max = 20, message = "职级分组长度不能超过20")
    @Schema(description = "职级分组：初级、中级、高级、专家、资深专家")
    private String levelGroup;
    @Schema(description = "薪资范围下限")
    private BigDecimal minSalary;
    @Schema(description = "薪资范围上限")
    private BigDecimal maxSalary;
    @Min(value = 0, message = "股权激励等级取值 0-2")
    @Max(value = 2, message = "股权激励等级取值 0-2")
    @Schema(description = "股权激励等级：0无，1部分，2全额")
    private Integer stockLevel;
    @Size(max = 200, message = "职级描述长度不能超过200")
    @Schema(description = "职级描述")
    private String description;
    @Size(max = 65535, message = "晋升要求长度不能超过65535")
    @Schema(description = "晋升要求（JSON格式）")
    private String requirements;
    @NotNull(message = "状态：0禁用，1启用不能为空")
    @Min(value = 0, message = "状态取值 0-1")
    @Max(value = 1, message = "状态取值 0-1")
    @Schema(description = "状态：0禁用，1启用")
    private Integer positionLevelStatus;
    @Schema(description = "租户ID")
    private Long tenantId;
}
