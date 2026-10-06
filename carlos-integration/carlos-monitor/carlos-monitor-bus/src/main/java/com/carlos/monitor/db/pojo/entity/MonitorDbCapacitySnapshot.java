package com.carlos.monitor.db.pojo.entity;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 数据库表容量每日快照 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Data
@Accessors(chain = true)
@TableName("monitor_db_capacity_snapshot")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitorDbCapacitySnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 数据库实例ID
     */
    @TableField("instance_id")
    private Long instanceId;

    /**
     * 模式名
     */
    @TableField("schema_name")
    private String schemaName;

    /**
     * 表名
     */
    @TableField("table_name")
    private String tableName;

    /**
     * 行数（估计值）
     */
    @TableField("table_rows")
    private Long tableRows;

    /**
     * 表大小（字节）
     */
    @TableField("table_size_bytes")
    private Long tableSizeBytes;

    /**
     * 索引大小（字节）
     */
    @TableField("index_size_bytes")
    private Long indexSizeBytes;

    /**
     * 快照日期
     */
    @TableField("snapshot_date")
    private LocalDate snapshotDate;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
