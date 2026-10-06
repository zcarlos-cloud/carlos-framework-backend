package com.carlos.org.position.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 职级表 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("org_position_level")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgPositionLevel extends Model<OrgPositionLevel> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 所属职系ID
     */
    @TableField(value = "category_id")
    private Long categoryId;
    /**
     * 职级编码，如：T1、T2-1、M3
     */
    @TableField(value = "level_code")
    private String levelCode;
    /**
     * 职级名称，如：初级工程师、高级工程师、部门经理
     */
    @TableField(value = "level_name")
    private String levelName;
    /**
     * 职级序列号，用于排序和比较，如：1、2、3
     */
    @TableField(value = "level_seq")
    private Integer levelSeq;
    /**
     * 职级分组：初级、中级、高级、专家、资深专家
     */
    @TableField(value = "level_group")
    private String levelGroup;
    /**
     * 薪资范围下限
     */
    @TableField(value = "min_salary")
    private BigDecimal minSalary;
    /**
     * 薪资范围上限
     */
    @TableField(value = "max_salary")
    private BigDecimal maxSalary;
    /**
     * 股权激励等级：0无，1部分，2全额
     */
    @TableField(value = "stock_level")
    private Integer stockLevel;
    /**
     * 职级描述
     */
    @TableField(value = "description")
    private String description;
    /**
     * 晋升要求（JSON格式）
     */
    @TableField(value = "requirements")
    private String requirements;
    /**
     * 状态：0禁用，1启用
     */
    @TableField(value = "position_level_status")
    private Integer positionLevelStatus;
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
