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
 * 岗位变更历史表 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgPositionHistoryVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "用户ID")
    private Long userId;
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
    @Schema(description = "变更原因")
    private String changeReason;
    @Schema(description = "变更生效日期")
    private LocalDate changeDate;
    @Schema(description = "审批单号")
    private String approvalNo;
    @Schema(description = "附件（JSON数组，存储文件URL）")
    private String attachments;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "操作人ID")
    private Long operatorId;
    @Schema(description = "操作人姓名（冗余）")
    private String operatorName;
    @Schema(description = "租户ID")
    private Long tenantId;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
