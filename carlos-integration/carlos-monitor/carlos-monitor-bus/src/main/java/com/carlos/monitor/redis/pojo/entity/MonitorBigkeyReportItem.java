package com.carlos.monitor.redis.pojo.entity;

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
 * Redis 大key/热key 报告明细 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@TableName("monitor_bigkey_report_item")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitorBigkeyReportItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 报告ID
     */
    @TableField("report_id")
    private Long reportId;

    /**
     * 节点地址（cluster 模式标识 key 所在分片）
     */
    @TableField("node_address")
    private String nodeAddress;

    /**
     * key 名
     */
    @TableField("redis_key")
    private String redisKey;

    /**
     * 类型：string/hash/list/set/zset/stream
     */
    @TableField("key_type")
    private String keyType;

    /**
     * 内存占用（字节，MEMORY USAGE）
     */
    @TableField("memory_bytes")
    private Long memoryBytes;

    /**
     * 元素数量
     */
    @TableField("element_count")
    private Long elementCount;

    /**
     * TTL（秒），-1 永不过期
     */
    @TableField("ttl_seconds")
    private Long ttlSeconds;

    /**
     * 访问频次（热key报告，OBJECT FREQ）
     */
    @TableField("access_freq")
    private Long accessFreq;

    /**
     * 内存是否为估算值（Redis < 4.0 无 MEMORY USAGE 时按元素长度估算）
     */
    @TableField("estimated")
    private Boolean estimated;

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
