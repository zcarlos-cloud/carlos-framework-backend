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
 * 岗位类别表（职系） 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("org_position_category")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgPositionCategory extends Model<OrgPositionCategory> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 类别编码，如：M、T、P、S
     */
    @TableField(value = "category_code")
    private String categoryCode;
    /**
     * 类别名称，如：管理系、技术系、专业系、销售系
     */
    @TableField(value = "category_name")
    private String categoryName;
    /**
     * 类别类型：1管理通道，2专业通道，3双通道
     */
    @TableField(value = "category_type")
    private Integer categoryType;
    /**
     * 类别描述
     */
    @TableField(value = "description")
    private String description;
    /**
     * 排序
     */
    @TableField(value = "order_num")
    private Integer orderNum;
    /**
     * 状态：0禁用，1启用
     */
    @TableField(value = "position_category_status")
    private Integer positionCategoryStatus;
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
