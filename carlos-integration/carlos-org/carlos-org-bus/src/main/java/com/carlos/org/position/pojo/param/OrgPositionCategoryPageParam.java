package com.carlos.org.position.pojo.param;


import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * <p>
 * 岗位类别表（职系） 列表查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "岗位类别表（职系）列表查询参数")
public class OrgPositionCategoryPageParam extends ParamPage {
    @Size(max = 50, message = "类别编码长度不能超过50")
    @Schema(description = "类别编码，如：M、T、P、S")
    private String categoryCode;
    @Size(max = 50, message = "类别名称长度不能超过50")
    @Schema(description = "类别名称，如：管理系、技术系、专业系、销售系")
    private String categoryName;
    @Schema(description = "类别类型：1管理通道，2专业通道，3双通道")
    private Integer categoryType;
    @Size(max = 200, message = "类别描述长度不能超过200")
    @Schema(description = "类别描述")
    private String description;
    @Schema(description = "排序")
    private Integer orderNum;
    @Schema(description = "状态：0禁用，1启用")
    private Integer positionCategoryStatus;
    @Schema(description = "租户ID")
    private Long tenantId;
    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
