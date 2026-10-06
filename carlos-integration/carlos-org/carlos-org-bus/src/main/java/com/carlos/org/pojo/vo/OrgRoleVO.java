package com.carlos.org.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 角色 VO
 * </p>
 * <p>角色列表, 角色详情</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgRoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "主键")
    private Long id;

    /**
     * 角色名称
     */
    @Schema(description = "角色名称")
    private String roleName;

    /**
     * 角色唯一编码
     */
    @Schema(description = "角色编码")
    private String roleCode;

    /**
     * 角色类型， 1：系统角色, 2: 自定义角色
     */
    @Schema(description = "角色类型， 1：系统角色, 2: 自定义角色")
    private Integer roleType;

    /**
     * 角色类型名称
     */
    @Schema(description = "角色类型名称")
    private String roleTypeName;

    /**
     * 数据范围， 1：全部, 2: 本部及子部门, 3:仅本部门, 4:仅本人, 5:自定义规则
     */
    @Schema(description = "数据范围")
    private Integer dataScope;

    /**
     * 数据范围名称
     */
    @Schema(description = "数据范围名称")
    private String dataScopeName;

    /**
     * 角色状态， 0：禁用, 1: 启用
     */
    @Schema(description = "角色状态， 0：禁用, 1: 启用")
    private Integer roleStatus;

    /**
     * 状态名称
     */
    @Schema(description = "状态名称")
    private String roleStatusName;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String description;

    /**
     * 用户数量
     */
    @Schema(description = "用户数量")
    private Integer userCount;

    /**
     * 创建者
     */
    @Schema(description = "创建者")
    private Long createBy;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

}
