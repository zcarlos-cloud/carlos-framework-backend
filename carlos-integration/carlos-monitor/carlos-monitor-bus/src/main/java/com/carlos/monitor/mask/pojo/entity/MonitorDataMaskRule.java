package com.carlos.monitor.mask.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 数据管理脱敏规则 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@TableName("monitor_data_mask_rule")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitorDataMaskRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 实例ID，NULL 表示全局规则
     */
    @TableField("instance_id")
    private Long instanceId;

    /**
     * 作用目标：DB=库表列，REDIS=按 key pattern（此时 table_name 存 pattern）
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 模式名（DB）
     */
    @TableField("schema_name")
    private String schemaName;

    /**
     * 表名 / Redis key pattern
     */
    @TableField("table_name")
    private String tableName;

    /**
     * 列名 / hash field 名
     */
    @TableField("column_name")
    private String columnName;

    /**
     * 脱敏类型：FULL/PHONE/EMAIL/NAME/CUSTOM
     */
    @TableField("mask_type")
    private String maskType;

    /**
     * CUSTOM 时的正则与替换模板（JSON：{"pattern":"...","replacement":"..."}）
     */
    @TableField("mask_pattern")
    private String maskPattern;

    /**
     * 启用开关
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 修改人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
