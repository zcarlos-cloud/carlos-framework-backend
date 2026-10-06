package com.carlos.job.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 定时任务元数据 数据传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
public class JobInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务分组
     */
    private String jobGroup;

    /**
     * 目标服务名（Nacos 注册名）
     */
    private String serviceName;

    /**
     * 任务处理器名称
     */
    private String handlerName;

    /**
     * 调度类型: 1-CRON 2-固定间隔 3-一次性
     */
    private Integer scheduleType;

    /**
     * 调度配置: cron 表达式/固定间隔毫秒/一次性触发时间
     */
    private String scheduleConf;

    /**
     * 任务参数（JSON 字符串）
     */
    private String params;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 调度过期策略: 0-智能策略 1-忽略过期 2-立即补跑一次 3-不补跑
     */
    private Integer misfireStrategy;

    /**
     * 并发模式: 1-串行 2-并行
     */
    private Integer concurrentMode;

    /**
     * 任务超时（秒），0-不限制
     */
    private Integer timeoutSeconds;

    /**
     * 失败重试次数，0-不重试
     */
    private Integer failRetryCount;

    /**
     * 负责人
     */
    private String author;

    /**
     * 报警接收人，多个逗号分隔
     */
    private String alarmReceivers;

    /**
     * 任务状态（Quartz 触发器状态）
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
