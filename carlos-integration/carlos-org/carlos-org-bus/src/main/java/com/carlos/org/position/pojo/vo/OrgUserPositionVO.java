package com.carlos.org.position.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户岗位职级关联表（核心任职信息） 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgUserPositionVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "岗位ID")
    private Long positionId;
    @Schema(description = "岗位编码")
    private String positionCode;
    @Schema(description = "岗位名称")
    private String positionName;
    @Schema(description = "职级ID（具体的职级）")
    private Long levelId;
    @Schema(description = "职级名称")
    private String levelName;
    @Schema(description = "任职部门ID（岗位可能跨部门，此处记录实际任职部门）")
    private Long departmentId;
    @Schema(description = "部门名称")
    private String departmentName;
    @Schema(description = "是否主岗位：1是，0否（兼职/兼岗）")
    private Boolean main;
    @Schema(description = "任职状态：1在职，2试用期，3待入职，4待离职，5已卸任")
    private Integer positionStatus;
    @Schema(description = "任职方式：1任命，2竞聘，3委派，4借调")
    private Integer appointType;
    @Schema(description = "任职日期")
    private LocalDate appointDate;
    @Schema(description = "试用期结束日期")
    private LocalDate probationEnd;
    @Schema(description = "卸任日期")
    private LocalDate dimissionDate;
    @Schema(description = "成本分摊比例（兼职岗使用，如0.5表示50%）")
    private BigDecimal costRatio;
    @Schema(description = "汇报对象（直接上级）user_id")
    private Long reportTo;
    @Schema(description = "虚线汇报对象（矩阵管理）user_id")
    private Long dottedReport;
    @Schema(description = "实际工作地点（可覆盖岗位默认地点）")
    private String workLocation;
    @Schema(description = "合同类型：1劳动合同，2劳务合同，3实习协议，4外包合同")
    private Integer contractType;
    @Schema(description = "最近绩效等级：A/B/C/D")
    private String performanceRating;
    @Schema(description = "实际薪级（可能高于岗位默认）")
    private String salaryLevel;
    @Schema(description = "是否有股权激励：0无，1有")
    private Boolean stockGrant;
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
