package com.carlos.org.position.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 岗位变更历史表 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("org_position_history")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgPositionHistory extends Model<OrgPositionHistory> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;
    /**
     * 变更类型：1入职，2晋升，3降职，4转岗，5兼职，6卸任，7离职
     */
    @TableField(value = "change_type")
    private Integer changeType;
    /**
     * 原岗位ID
     */
    @TableField(value = "old_position_id")
    private Long oldPositionId;
    /**
     * 新岗位ID
     */
    @TableField(value = "new_position_id")
    private Long newPositionId;
    /**
     * 原职级ID
     */
    @TableField(value = "old_level_id")
    private Long oldLevelId;
    /**
     * 新职级ID
     */
    @TableField(value = "new_level_id")
    private Long newLevelId;
    /**
     * 原部门ID
     */
    @TableField(value = "old_dept_id")
    private Long oldDeptId;
    /**
     * 新部门ID
     */
    @TableField(value = "new_dept_id")
    private Long newDeptId;
    /**
     * 原薪资
     */
    @TableField(value = "old_salary")
    private BigDecimal oldSalary;
    /**
     * 新薪资
     */
    @TableField(value = "new_salary")
    private BigDecimal newSalary;
    /**
     * 变更原因
     */
    @TableField(value = "change_reason")
    private String changeReason;
    /**
     * 变更生效日期
     */
    @TableField(value = "change_date")
    private LocalDate changeDate;
    /**
     * 审批单号
     */
    @TableField(value = "approval_no")
    private String approvalNo;
    /**
     * 附件（JSON数组，存储文件URL）
     */
    @TableField(value = "attachments")
    private String attachments;
    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;
    /**
     * 操作人ID
     */
    @TableField(value = "operator_id")
    private Long operatorId;
    /**
     * 操作人姓名（冗余）
     */
    @TableField(value = "operator_name")
    private String operatorName;
    /**
     * 租户ID
     */
    @TableField(value = "tenant_id")
    private Long tenantId;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
