package com.carlos.monitor.api.pojo.ao;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 监控实例健康状态 API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
public class MonitorInstanceHealthAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 实例ID（雪花） */
    private Long instanceId;

    /** 实例名称 */
    private String instanceName;

    /** 实例类型（字典 monitor_instance_type） */
    private String instanceType;

    /** 环境（字典 monitor_env）：dev/test/prod */
    private String env;

    /** 健康状态：UNKNOWN/UP/DOWN */
    private String healthStatus;

    /** 最近成功采集时间 */
    private LocalDateTime lastCollectTime;
}
