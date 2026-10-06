package com.carlos.org.position.api.pojo.ao;


import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户岗位职级关联表（核心任职信息） API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
public class OrgUserPositionAO implements Serializable {
    /** 主键 */
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 岗位ID */
    private Long positionId;
    /** 职级ID（具体的职级） */
    private Long levelId;
    /** 任职部门ID（岗位可能跨部门，此处记录实际任职部门） */
    private Long departmentId;
    /** 是否主岗位：1是，0否（兼职/兼岗） */
    private Boolean main;
    /** 任职状态：1在职，2试用期，3待入职，4待离职，5已卸任 */
    private Integer positionStatus;
    /** 任职方式：1任命，2竞聘，3委派，4借调 */
    private Integer appointType;
    /** 任职日期 */
    private LocalDate appointDate;
    /** 试用期结束日期 */
    private LocalDate probationEnd;
    /** 卸任日期 */
    private LocalDate dimissionDate;
    /** 成本分摊比例（兼职岗使用，如0.5表示50%） */
    private BigDecimal costRatio;
    /** 汇报对象（直接上级）user_id */
    private Long reportTo;
    /** 虚线汇报对象（矩阵管理）user_id */
    private Long dottedReport;
    /** 实际工作地点（可覆盖岗位默认地点） */
    private String workLocation;
    /** 合同类型：1劳动合同，2劳务合同，3实习协议，4外包合同 */
    private Integer contractType;
    /** 最近绩效等级：A/B/C/D */
    private String performanceRating;
    /** 实际薪级（可能高于岗位默认） */
    private String salaryLevel;
    /** 是否有股权激励：0无，1有 */
    private Boolean stockGrant;
    /** 乐观锁版本号 */
    private Integer version;
    /** 租户ID */
    private Long tenantId;
    /** 创建者 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 修改者 */
    private Long updateBy;
    /** 修改时间 */
    private LocalDateTime updateTime;
}
