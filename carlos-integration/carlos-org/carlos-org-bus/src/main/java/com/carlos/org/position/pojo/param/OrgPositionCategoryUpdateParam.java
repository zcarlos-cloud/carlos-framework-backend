package com.carlos.org.position.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 岗位类别表（职系） 更新参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@Schema(description = "岗位类别表（职系）修改参数")
public class OrgPositionCategoryUpdateParam {
    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;
    @Size(max = 50, message = "类别编码长度不能超过50")
    @Schema(description = "类别编码，如：M、T、P、S")
    private String categoryCode;
    @Size(max = 50, message = "类别名称长度不能超过50")
    @Schema(description = "类别名称，如：管理系、技术系、专业系、销售系")
    private String categoryName;
    @Min(value = 1, message = "类别类型取值 1-3")
    @Max(value = 3, message = "类别类型取值 1-3")
    @Schema(description = "类别类型：1管理通道，2专业通道，3双通道")
    private Integer categoryType;
    @Size(max = 200, message = "类别描述长度不能超过200")
    @Schema(description = "类别描述")
    private String description;
    @Min(value = 0, message = "排序不能小于0")
    @Schema(description = "排序")
    private Integer orderNum;
    @Min(value = 0, message = "状态取值 0-1")
    @Max(value = 1, message = "状态取值 0-1")
    @Schema(description = "状态：0禁用，1启用")
    private Integer positionCategoryStatus;
    @Schema(description = "租户ID")
    private Long tenantId;
}
