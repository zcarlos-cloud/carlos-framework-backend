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
 * 用户岗位职级关联表（核心任职信息） 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("org_user_position")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgUserPosition extends Model<OrgUserPosition> implements Serializable {
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
     * 岗位ID
     */
    @TableField(value = "position_id")
    private Long positionId;
    /**
     * 职级ID（具体的职级）
     */
    @TableField(value = "level_id")
    private Long levelId;
    /**
     * 任职部门ID（岗位可能跨部门，此处记录实际任职部门）
     */
    @TableField(value = "department_id")
    private Long departmentId;
    /**
     * 是否主岗位：1是，0否（兼职/兼岗）
     */
    @TableField(value = "is_main")
    private Boolean main;
    /**
     * 任职状态：1在职，2试用期，3待入职，4待离职，5已卸任
     */
    @TableField(value = "position_status")
    private Integer positionStatus;
    /**
     * 任职方式：1任命，2竞聘，3委派，4借调
     */
    @TableField(value = "appoint_type")
    private Integer appointType;
    /**
     * 任职日期
     */
    @TableField(value = "appoint_date")
    private LocalDate appointDate;
    /**
     * 试用期结束日期
     */
    @TableField(value = "probation_end")
    private LocalDate probationEnd;
    /**
     * 卸任日期
     */
    @TableField(value = "dimission_date")
    private LocalDate dimissionDate;
    /**
     * 成本分摊比例（兼职岗使用，如0.5表示50%）
     */
    @TableField(value = "cost_ratio")
    private BigDecimal costRatio;
    /**
     * 汇报对象（直接上级）user_id
     */
    @TableField(value = "report_to")
    private Long reportTo;
    /**
     * 虚线汇报对象（矩阵管理）user_id
     */
    @TableField(value = "dotted_report")
    private Long dottedReport;
    /**
     * 实际工作地点（可覆盖岗位默认地点）
     */
    @TableField(value = "work_location")
    private String workLocation;
    /**
     * 合同类型：1劳动合同，2劳务合同，3实习协议，4外包合同
     */
    @TableField(value = "contract_type")
    private Integer contractType;
    /**
     * 最近绩效等级：A/B/C/D
     */
    @TableField(value = "performance_rating")
    private String performanceRating;
    /**
     * 实际薪级（可能高于岗位默认）
     */
    @TableField(value = "salary_level")
    private String salaryLevel;
    /**
     * 是否有股权激励：0无，1有
     */
    @TableField(value = "stock_grant")
    private Boolean stockGrant;
    /**
     * 乐观锁版本号
     */
    @Version
    @TableField(value = "version")
    private Integer version;
    /**
     * 逻辑删除：0正常，1删除
     */
    @TableLogic
    @TableField(value = "is_deleted")
    private Boolean deleted;
    /**
     * 租户ID
     */
    @TableField(value = "tenant_id")
    private Long tenantId;
    /**
     * 创建者
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 修改者
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;
    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}
