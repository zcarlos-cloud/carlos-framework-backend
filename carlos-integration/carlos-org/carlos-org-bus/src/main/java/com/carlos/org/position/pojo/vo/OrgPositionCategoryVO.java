package com.carlos.org.position.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 岗位类别表（职系） 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgPositionCategoryVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "类别编码，如：M、T、P、S")
    private String categoryCode;
    @Schema(description = "类别名称，如：管理系、技术系、专业系、销售系")
    private String categoryName;
    @Schema(description = "类别类型：1管理通道，2专业通道，3双通道")
    private Integer categoryType;
    @Schema(description = "类别描述")
    private String description;
    @Schema(description = "排序")
    private Integer orderNum;
    @Schema(description = "状态：0禁用，1启用")
    private Integer positionCategoryStatus;
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
