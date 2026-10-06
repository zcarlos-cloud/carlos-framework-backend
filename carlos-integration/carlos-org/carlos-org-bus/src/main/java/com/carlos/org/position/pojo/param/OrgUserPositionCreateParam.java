package com.carlos.org.position.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;


/**
 * <p>
 * 用户岗位职级关联表（核心任职信息） 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户岗位职级关联表（核心任职信息）新增参数")
public class OrgUserPositionCreateParam {
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long userId;
    @NotNull(message = "岗位ID不能为空")
    @Schema(description = "岗位ID")
    private Long positionId;
    @NotNull(message = "职级ID（具体的职级）不能为空")
    @Schema(description = "职级ID（具体的职级）")
    private Long levelId;
    @NotNull(message = "任职部门ID（岗位可能跨部门，此处记录实际任职部门）不能为空")
    @Schema(description = "任职部门ID（岗位可能跨部门，此处记录实际任职部门）")
    private Long departmentId;
    @NotNull(message = "是否主岗位：1是，0否（兼职/兼岗）不能为空")
    @Schema(description = "是否主岗位：1是，0否（兼职/兼岗）")
    private Boolean main;
    @NotNull(message = "任职状态：1在职，2试用期，3待入职，4待离职，5已卸任不能为空")
    @Min(value = 1, message = "任职状态取值 1-5")
    @Max(value = 5, message = "任职状态取值 1-5")
    @Schema(description = "任职状态：1在职，2试用期，3待入职，4待离职，5已卸任")
    private Integer positionStatus;
    @NotNull(message = "任职方式：1任命，2竞聘，3委派，4借调不能为空")
    @Min(value = 1, message = "任职方式取值 1-4")
    @Max(value = 4, message = "任职方式取值 1-4")
    @Schema(description = "任职方式：1任命，2竞聘，3委派，4借调")
    private Integer appointType;
    @NotNull(message = "任职日期不能为空")
    @Schema(description = "任职日期")
    private LocalDate appointDate;
    @Schema(description = "试用期结束日期")
    private LocalDate probationEnd;
    @Schema(description = "卸任日期")
    private LocalDate dimissionDate;
    @Min(value = 0, message = "成本分摊比例不能小于0")
    @Max(value = 1, message = "成本分摊比例不能大于1")
    @Schema(description = "成本分摊比例（兼职岗使用，如0.5表示50%）")
    private BigDecimal costRatio;
    @Schema(description = "汇报对象（直接上级）user_id")
    private Long reportTo;
    @Schema(description = "虚线汇报对象（矩阵管理）user_id")
    private Long dottedReport;
    @Size(max = 100, message = "实际工作地点长度不能超过100")
    @Schema(description = "实际工作地点（可覆盖岗位默认地点）")
    private String workLocation;
    @Min(value = 1, message = "合同类型取值 1-4")
    @Max(value = 4, message = "合同类型取值 1-4")
    @Schema(description = "合同类型：1劳动合同，2劳务合同，3实习协议，4外包合同")
    private Integer contractType;
    @Size(max = 10, message = "最近绩效等级长度不能超过10")
    @Schema(description = "最近绩效等级：A/B/C/D")
    private String performanceRating;
    @Size(max = 20, message = "实际薪级长度不能超过20")
    @Schema(description = "实际薪级（可能高于岗位默认）")
    private String salaryLevel;
    @Schema(description = "是否有股权激励：0无，1有")
    private Boolean stockGrant;
    @Schema(description = "租户ID")
    private Long tenantId;
}
