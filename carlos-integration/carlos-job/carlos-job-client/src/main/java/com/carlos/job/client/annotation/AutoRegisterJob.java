package com.carlos.job.client.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * 任务自动注册注解（方法级，需与 {@link ScheduleJob} 同时标注）
 * </p>
 *
 * <p>应用启动完成后，带本注解的方法会按其声明向调度中心注册一条任务记录：</p>
 * <ul>
 *   <li>任务<b>不存在</b>时按注解创建（写 job_info + 建 Quartz 触发器）；</li>
 *   <li>任务<b>已存在</b>时跳过、<b>不覆盖</b> —— {@code job_info} 是唯一真相源，
 *       运维在后台调整过的 cron/参数不会被下次发布改回去，不一致只打 WARN。</li>
 * </ul>
 *
 * <p>整型属性用哨兵默认值（{@code -1}/{@code 0}）表示"未指定"：由调度中心补默认值，
 * 从而只对显式配置过的字段做漂移提示。</p>
 *
 * <p>注意：<b>删掉注解不会删除任务</b>。任务会继续按库中配置调度，到点因处理器不存在而失败，
 * 需要停用请到管理后台暂停/删除任务记录。</p>
 *
 * @author Carlos
 * @date 2026年9月23日
 * @see ScheduleJob
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoRegisterJob {

    /**
     * cron 表达式（Quartz 6 段）；与 {@link #scheduleConf()} 二选一，后者优先
     */
    String cron() default "";

    /**
     * 调度配置：CRON 表达式 / 固定间隔毫秒（≥10000） / 一次性 ISO 时间（2026-09-24T02:00:00）
     */
    String scheduleConf() default "";

    /**
     * 调度类型：1 Cron 2 固定间隔 3 一次性
     */
    int scheduleType() default 1;

    /**
     * 任务名称（分组内唯一）。留空时取处理器名称
     */
    String jobName() default "";

    /**
     * 任务分组，留空按 DEFAULT
     */
    String jobGroup() default "";

    /**
     * 目标服务名（Nacos 注册名）。留空时取本机 spring.application.name
     */
    String serviceName() default "";

    /**
     * 任务参数（JSON 字符串），执行时原样传给方法
     */
    String params() default "";

    /**
     * 任务描述
     */
    String description() default "";

    /**
     * 过期策略：0 智能 1 忽略过期 2 立即补跑一次 3 不补跑；-1 表示未指定
     */
    int misfireStrategy() default -1;

    /**
     * 并发模式：1 串行 2 并行；0 表示未指定
     */
    int concurrentMode() default 0;

    /**
     * 超时秒数，0 表示不限制；-1 表示未指定
     */
    int timeoutSeconds() default -1;

    /**
     * 失败重试次数；-1 表示未指定
     */
    int failRetryCount() default -1;

    /**
     * 初始状态：NORMAL / PAUSED，留空按 NORMAL
     */
    String status() default "";

    /**
     * 负责人
     */
    String author() default "";

    /**
     * 报警接收人，多个逗号分隔
     */
    String alarmReceivers() default "";

}
