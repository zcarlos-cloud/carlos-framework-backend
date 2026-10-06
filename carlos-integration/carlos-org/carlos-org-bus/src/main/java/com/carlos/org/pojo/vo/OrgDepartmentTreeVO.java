package com.carlos.org.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 部门树形 VO
 * </p>
 * <p>部门树</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
public class OrgDepartmentTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "主键")
    private Long id;

    /**
     * 父id
     */
    @Schema(description = "父id")
    private Long parentId;

    /**
     * 部门名称
     */
    @Schema(description = "部门名称")
    private String deptName;

    /**
     * 部门编号
     */
    @Schema(description = "部门编号")
    private String deptCode;

    /**
     * 部门路径
     */
    @Schema(description = "部门路径")
    private String path;

    /**
     * 负责人id
     */
    @Schema(description = "负责人id")
    private Long leaderId;

    /**
     * 状态，0：禁用，1：启用
     */
    @Schema(description = "状态，0：禁用，1：启用")
    private Integer deptStatus;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer orderNum;

    /**
     * 层级
     */
    @Schema(description = "层级")
    private Integer level;

    /**
     * 子部门列表
     */
    @Schema(description = "子部门列表")
    private List<OrgDepartmentTreeVO> children;

}
