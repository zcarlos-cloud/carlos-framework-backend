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
import java.time.LocalDateTime;

/**
 * <p>
 * SQL 控制台执行历史（脱敏后留档） 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Data
@Accessors(chain = true)
@TableName("monitor_sql_history")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitorSqlHistory implements Serializable {

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
     * 执行人（用户ID）
     */
    @TableField("execute_by")
    private Long executeBy;

    /**
     * 执行 SQL（参数已脱敏，截断 5000）
     */
    @TableField("sql_text")
    private String sqlText;

    /**
     * SQL 类型：SELECT/EXPLAIN/UPDATE/DELETE
     */
    @TableField("sql_type")
    private String sqlType;

    /**
     * 执行结果：SUCCESS/FAILED/BLOCKED
     */
    @TableField("exec_status")
    private String execStatus;

    /**
     * 被拦截原因（exec_status=BLOCKED）
     */
    @TableField("block_reason")
    private String blockReason;

    /**
     * 影响行数
     */
    @TableField("affected_rows")
    private Integer affectedRows;

    /**
     * 返回行数
     */
    @TableField("result_rows")
    private Integer resultRows;

    /**
     * 执行耗时（毫秒）
     */
    @TableField("duration_ms")
    private Long durationMs;

    /**
     * 来源 IP
     */
    @TableField("client_ip")
    private String clientIp;

    /**
     * 执行时间
     */
    @TableField("execute_time")
    private LocalDateTime executeTime;

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
