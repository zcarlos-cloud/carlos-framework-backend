package com.carlos.auth.app.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用分类
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Data
@Accessors(chain = true)
@TableName("app_category")
public class AppCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /** 分类编码 */
    @TableField(value = "category_code")
    private String categoryCode;

    /** 分类名称 */
    @TableField(value = "category_name")
    private String categoryName;

    /** 分类图标 */
    @TableField(value = "category_icon")
    private String categoryIcon;

    /** 排序 */
    @TableField(value = "sort_order")
    private Integer sortOrder;

    /** 逻辑删除 */
    @TableLogic
    @TableField(value = "is_deleted")
    private Boolean deleted;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
