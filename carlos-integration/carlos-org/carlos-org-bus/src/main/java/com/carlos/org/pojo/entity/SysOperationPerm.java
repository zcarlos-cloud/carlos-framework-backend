package com.carlos.org.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * <p>
 * sys_operation 精简实体（仅用于权限查询）
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Data
@TableName("sys_operation")
public class SysOperationPerm {

    @TableField(value = "id")
    private Long id;

    @TableField(value = "perm_code")
    private String permCode;

    @TableField(value = "op_status")
    private Integer permStatus;

}
