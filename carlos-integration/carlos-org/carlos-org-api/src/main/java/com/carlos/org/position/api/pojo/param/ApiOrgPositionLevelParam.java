package com.carlos.org.position.api.pojo.param;


import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 职级表 API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
public class ApiOrgPositionLevelParam implements Serializable {
    /** 主键 */
    private Long id;
    /** 所属职系ID */
    private Long categoryId;
    /** 职级编码，如：T1、T2-1、M3 */
    private String levelCode;
    /** 职级名称，如：初级工程师、高级工程师、部门经理 */
    private String levelName;
    /** 职级序列号，用于排序和比较，如：1、2、3 */
    private Integer levelSeq;
    /** 职级分组：初级、中级、高级、专家、资深专家 */
    private String levelGroup;
    /** 薪资范围下限 */
    private BigDecimal minSalary;
    /** 薪资范围上限 */
    private BigDecimal maxSalary;
    /** 股权激励等级：0无，1部分，2全额 */
    private Integer stockLevel;
    /** 职级描述 */
    private String description;
    /** 晋升要求（JSON格式） */
    private String requirements;
    /** 状态：0禁用，1启用 */
    private Integer positionLevelStatus;
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
