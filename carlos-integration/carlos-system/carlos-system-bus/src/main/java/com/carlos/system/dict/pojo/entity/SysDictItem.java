package com.carlos.system.dict.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统字典详情 数据源对象
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Data
@Accessors(chain = true)
@TableName("sys_dict_item")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysDictItem implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 字典id
     */
    @TableField("dict_id")
    private Long dictId;
    /**
     * 字典项值
     */
    @TableField("item_name")
    private String itemName;
    /**
     * 字典项key
     */
    @TableField("item_code")
    private String itemCode;
    /**
     * 描述
     */
    @TableField("description")
    private String description;
    /**
     * 排序
     */
    @TableField("order_num")
    private Integer orderNum;
    /**
     * 状态（1启用 0不启用）
     */
    @TableField("is_enable")
    private Boolean enable;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}
