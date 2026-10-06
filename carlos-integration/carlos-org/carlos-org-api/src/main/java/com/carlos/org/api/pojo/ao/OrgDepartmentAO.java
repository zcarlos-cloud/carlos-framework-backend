package com.carlos.org.api.pojo.ao;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 部门 API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
public class OrgDepartmentAO implements Serializable {
    /** 主键 */
    private Long id;
    /** 父id */
    private Long parentId;
    /** 部门名称 */
    private String deptName;
    /** 部门编号 */
    private String deptCode;
    /** 部门路径 */
    private String path;
    /** 负责人id */
    private Long leaderId;
    /** 状态，0：禁用，1：启用 */
    private Integer deptStatus;
    /** 排序 */
    private Integer orderNum;
    /** 层级 */
    private Integer level;
    /** 备注 */
    private String description;
    /** 版本 */
    private Integer version;
    /** 创建者 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 修改者 */
    private Long updateBy;
    /** 修改时间 */
    private LocalDateTime updateTime;
    /** 租户id */
    private Long tenantId;
}
