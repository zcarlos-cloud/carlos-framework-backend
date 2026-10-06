package com.carlos.org.position.pojo.param;


import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * <p>
 * 职级表 列表查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "职级表列表查询参数")
public class OrgPositionLevelPageParam extends ParamPage {
    @Schema(description = "所属职系ID")
    private Long categoryId;
    @Size(max = 20, message = "职级编码长度不能超过20")
    @Schema(description = "职级编码，如：T1、T2-1、M3")
    private String levelCode;
    @Size(max = 50, message = "职级名称长度不能超过50")
    @Schema(description = "职级名称，如：初级工程师、高级工程师、部门经理")
    private String levelName;
    @Schema(description = "职级序列号，用于排序和比较，如：1、2、3")
    private Integer levelSeq;
    @Size(max = 20, message = "职级分组长度不能超过20")
    @Schema(description = "职级分组：初级、中级、高级、专家、资深专家")
    private String levelGroup;
    @Schema(description = "薪资范围下限")
    private BigDecimal minSalary;
    @Schema(description = "薪资范围上限")
    private BigDecimal maxSalary;
    @Schema(description = "股权激励等级：0无，1部分，2全额")
    private Integer stockLevel;
    @Size(max = 200, message = "职级描述长度不能超过200")
    @Schema(description = "职级描述")
    private String description;
    @Size(max = 65535, message = "晋升要求长度不能超过65535")
    @Schema(description = "晋升要求（JSON格式）")
    private String requirements;
    @Schema(description = "状态：0禁用，1启用")
    private Integer positionLevelStatus;
    @Schema(description = "租户ID")
    private Long tenantId;
    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
