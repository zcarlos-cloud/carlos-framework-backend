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
 * 岗位变更历史表 更新参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@Schema(description = "岗位变更历史表修改参数")
public class OrgPositionHistoryUpdateParam {
    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
    @Min(value = 1, message = "变更类型取值 1-7")
    @Max(value = 7, message = "变更类型取值 1-7")
    @Schema(description = "变更类型：1入职，2晋升，3降职，4转岗，5兼职，6卸任，7离职")
    private Integer changeType;
    @Schema(description = "原岗位ID")
    private Long oldPositionId;
    @Schema(description = "新岗位ID")
    private Long newPositionId;
    @Schema(description = "原职级ID")
    private Long oldLevelId;
    @Schema(description = "新职级ID")
    private Long newLevelId;
    @Schema(description = "原部门ID")
    private Long oldDeptId;
    @Schema(description = "新部门ID")
    private Long newDeptId;
    @Schema(description = "原薪资")
    private BigDecimal oldSalary;
    @Schema(description = "新薪资")
    private BigDecimal newSalary;
    @Size(max = 200, message = "变更原因长度不能超过200")
    @Schema(description = "变更原因")
    private String changeReason;
    @Schema(description = "变更生效日期")
    private LocalDate changeDate;
    @Size(max = 50, message = "审批单号长度不能超过50")
    @Schema(description = "审批单号")
    private String approvalNo;
    @Size(max = 65535, message = "附件长度不能超过65535")
    @Schema(description = "附件（JSON数组，存储文件URL）")
    private String attachments;
    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "操作人ID")
    private Long operatorId;
    @Size(max = 20, message = "操作人姓名长度不能超过20")
    @Schema(description = "操作人姓名（冗余）")
    private String operatorName;
    @Schema(description = "租户ID")
    private Long tenantId;
}
