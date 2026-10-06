package com.carlos.org.position.api.pojo.ao;


import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 岗位变更历史表 API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
public class OrgPositionHistoryAO implements Serializable {
    /** 主键 */
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 变更类型：1入职，2晋升，3降职，4转岗，5兼职，6卸任，7离职 */
    private Integer changeType;
    /** 原岗位ID */
    private Long oldPositionId;
    /** 新岗位ID */
    private Long newPositionId;
    /** 原职级ID */
    private Long oldLevelId;
    /** 新职级ID */
    private Long newLevelId;
    /** 原部门ID */
    private Long oldDeptId;
    /** 新部门ID */
    private Long newDeptId;
    /** 原薪资 */
    private BigDecimal oldSalary;
    /** 新薪资 */
    private BigDecimal newSalary;
    /** 变更原因 */
    private String changeReason;
    /** 变更生效日期 */
    private LocalDate changeDate;
    /** 审批单号 */
    private String approvalNo;
    /** 附件（JSON数组，存储文件URL） */
    private String attachments;
    /** 备注 */
    private String remark;
    /** 操作人ID */
    private Long operatorId;
    /** 操作人姓名（冗余） */
    private String operatorName;
    /** 租户ID */
    private Long tenantId;
    /** 创建时间 */
    private LocalDateTime createTime;
}
