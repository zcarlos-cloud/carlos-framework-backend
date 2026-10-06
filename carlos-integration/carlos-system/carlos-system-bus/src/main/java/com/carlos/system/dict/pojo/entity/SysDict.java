package com.carlos.system.dict.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.carlos.system.dict.pojo.enums.DictTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统字典 数据源对象
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Data
@Accessors(chain = true)
@TableName("sys_dict")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysDict implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 字典名称
     */
    @TableField("dict_name")
    private String dictName;
    /**
     * 字典编码
     */
    @TableField("dict_code")
    private String dictCode;
    /**
     * 描述
     */
    @TableField("description")
    private String description;
    /**
     * 字典类型 数字类型 字符类型
     */
    @TableField("type")
    private DictTypeEnum type;
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
