package com.carlos.job.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * Quartz 集群节点状态 数据源对象（只读视图，映射 QRTZ_SCHEDULER_STATE）
 * </p>
 * <p>
 * 该表由 Quartz JDBC Store 自行维护，本实体仅用于只读查询集群节点，禁止写入。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Data
@Accessors(chain = true)
@TableName("QRTZ_SCHEDULER_STATE")
public class QrtzSchedulerState implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 调度器名称
     */
    @TableField(value = "SCHED_NAME")
    private String schedName;

    /**
     * 实例ID
     */
    @TableField(value = "INSTANCE_NAME")
    private String instanceName;

    /**
     * 最近心跳时间(毫秒)
     */
    @TableField(value = "LAST_CHECKIN_TIME")
    private Long lastCheckinTime;

    /**
     * 心跳间隔(毫秒)
     */
    @TableField(value = "CHECKIN_INTERVAL")
    private Long checkinInterval;

}
