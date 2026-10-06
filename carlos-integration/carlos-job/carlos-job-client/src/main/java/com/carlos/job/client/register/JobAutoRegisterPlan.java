package com.carlos.job.client.register;

import cn.hutool.core.util.StrUtil;
import com.carlos.job.pojo.param.ApiJobRegisterParam;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 注解式任务注册计划：启动期解析 {@code @ScheduleJob} + {@code @AutoRegisterJob} 的产物
 * </p>
 *
 * <p>整型字段用哨兵值表示"注解未指定"（{@code -1}/{@code 0}），转
 * {@link ApiJobRegisterParam} 时归零为 {@code null}，交由调度中心补默认值。</p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Data
@Accessors(chain = true)
public class JobAutoRegisterPlan {

    /**
     * 处理器名称（{@code @ScheduleJob} 取值，留空取方法名）
     */
    private String handlerName;

    /**
     * 目标 Bean 名称
     */
    private String beanName;

    /**
     * 目标方法全名（类全名#方法名），用于日志
     */
    private String methodLocation;

    /**
     * 任务名称，留空时取处理器名称
     */
    private String jobName;

    /**
     * 任务分组，留空由调度中心按 DEFAULT 处理
     */
    private String jobGroup;

    /**
     * 目标服务名（注解留空时取本机 spring.application.name）
     */
    private String serviceName;

    /**
     * 调度类型：1 Cron 2 固定间隔 3 一次性
     */
    private Integer scheduleType;

    /**
     * 生效的调度配置（scheduleConf 优先，其次 cron）
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
     * 过期策略，-1 表示未指定
     */
    private Integer misfireStrategy;

    /**
     * 并发模式，0 表示未指定
     */
    private Integer concurrentMode;

    /**
     * 超时秒数，-1 表示未指定
     */
    private Integer timeoutSeconds;

    /**
     * 失败重试次数，-1 表示未指定
     */
    private Integer failRetryCount;

    /**
     * 初始状态：NORMAL / PAUSED，留空按 NORMAL
     */
    private String status;

    /**
     * 负责人
     */
    private String author;

    /**
     * 报警接收人
     */
    private String alarmReceivers;

    /**
     * 转为调度中心注册参数
     *
     * @return 注册参数
     */
    public ApiJobRegisterParam toRegisterParam() {
        return new ApiJobRegisterParam()
            .setJobName(StrUtil.blankToDefault(jobName, handlerName))
            .setJobGroup(StrUtil.blankToDefault(jobGroup, null))
            .setServiceName(serviceName)
            .setHandlerName(handlerName)
            .setScheduleType(scheduleType)
            .setScheduleConf(scheduleConf)
            .setParams(StrUtil.blankToDefault(params, null))
            .setDescription(StrUtil.blankToDefault(description, null))
            .setMisfireStrategy(sentinel(misfireStrategy, -1))
            .setConcurrentMode(sentinel(concurrentMode, 0))
            .setTimeoutSeconds(sentinel(timeoutSeconds, -1))
            .setFailRetryCount(sentinel(failRetryCount, -1))
            .setStatus(StrUtil.blankToDefault(status, null))
            .setAuthor(StrUtil.blankToDefault(author, null))
            .setAlarmReceivers(StrUtil.blankToDefault(alarmReceivers, null));
    }

    /**
     * 哨兵值视为"未指定"，归零为 null 由服务端补默认值
     */
    private static Integer sentinel(Integer value, int sentinel) {
        return value == null || value == sentinel ? null : value;
    }

}
