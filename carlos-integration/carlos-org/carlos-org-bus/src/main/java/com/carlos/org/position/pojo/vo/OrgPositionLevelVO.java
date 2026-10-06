package com.carlos.org.position.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 职级表 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgPositionLevelVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "所属职系ID")
    private Long categoryId;
    @Schema(description = "职级编码，如：T1、T2-1、M3")
    private String levelCode;
    @Schema(description = "职级名称，如：初级工程师、高级工程师、部门经理")
    private String levelName;
    @Schema(description = "职级序列号，用于排序和比较，如：1、2、3")
    private Integer levelSeq;
    @Schema(description = "职级分组：初级、中级、高级、专家、资深专家")
    private String levelGroup;
    @Schema(description = "薪资范围下限")
    private BigDecimal minSalary;
    @Schema(description = "薪资范围上限")
    private BigDecimal maxSalary;
    @Schema(description = "股权激励等级：0无，1部分，2全额")
    private Integer stockLevel;
    @Schema(description = "职级描述")
    private String description;
    @Schema(description = "晋升要求（JSON格式）")
    private String requirements;
    @Schema(description = "状态：0禁用，1启用")
    private Integer positionLevelStatus;
    @Schema(description = "租户ID")
    private Long tenantId;
    @Schema(description = "创建者")
    private Long createBy;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "修改者")
    private Long updateBy;
    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

}
