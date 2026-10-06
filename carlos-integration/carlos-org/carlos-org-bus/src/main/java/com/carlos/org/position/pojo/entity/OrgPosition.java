package com.carlos.org.position.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 岗位表 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("org_position")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgPosition extends Model<OrgPosition> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 岗位编码，租户内唯一，如：JAVA_DEV_001
     */
    @TableField(value = "position_code")
    private String positionCode;
    /**
     * 岗位名称，如：Java开发工程师
     */
    @TableField(value = "position_name")
    private String positionName;
    /**
     * 岗位简称，如：Java开发
     */
    @TableField(value = "position_short")
    private String positionShort;
    /**
     * 所属职系ID
     */
    @TableField(value = "category_id")
    private Long categoryId;
    /**
     * 默认职级ID（新人入职默认职级）
     */
    @TableField(value = "default_level_id")
    private Long defaultLevelId;
    /**
     * 职级范围，如：T1-T5，支持的范围
     */
    @TableField(value = "level_range")
    private String levelRange;
    /**
     * 岗位类型：1标准岗（有编制），2虚拟岗（项目制），3兼职岗
     */
    @TableField(value = "position_type")
    private Integer positionType;
    /**
     * 岗位性质：1全职，2兼职，3实习，4外包
     */
    @TableField(value = "position_nature")
    private Integer positionNature;
    /**
     * 所属部门ID（标准岗必填，虚拟岗可为空）
     */
    @TableField(value = "department_id")
    private Long departmentId;
    /**
     * 上级岗位ID，用于汇报线，0表示无上级
     */
    @TableField(value = "parent_id")
    private Long parentId;
    /**
     * 编制人数（标准岗），0表示不限
     */
    @TableField(value = "headcount")
    private Integer headcount;
    /**
     * 当前在岗人数（冗余，实时计算）
     */
    @TableField(value = "current_count")
    private Integer currentCount;
    /**
     * 空缺人数（冗余，实时计算）
     */
    @TableField(value = "vacancy_count")
    private Integer vacancyCount;
    /**
     * 岗位职责（富文本）
     */
    @TableField(value = "duty")
    private String duty;
    /**
     * 任职要求（富文本）
     */
    @TableField(value = "requirement")
    private String requirement;
    /**
     * 任职资格（JSON格式，支持扩展）
     */
    @TableField(value = "qualification")
    private String qualification;
    /**
     * 绩效考核指标（JSON格式）
     */
    @TableField(value = "kpi_indicator")
    private String kpiIndicator;
    /**
     * 成本中心编码，对接财务系统
     */
    @TableField(value = "cost_center")
    private String costCenter;
    /**
     * 工作地点，如：北京、上海、远程
     */
    @TableField(value = "location")
    private String location;
    /**
     * 薪等，如：P5、P6
     */
    @TableField(value = "salary_grade")
    private String salaryGrade;
    /**
     * 状态：0禁用，1启用，2冻结（暂停招聘）
     */
    @TableField(value = "position_status")
    private Integer positionStatus;
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
