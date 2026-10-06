package com.carlos.job.client.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * 任务处理器注解（方法级，对标 xxl-job 的 {@code @XxlJob}）
 * </p>
 *
 * <p>标注在任意 Spring Bean 的 public 方法上，该方法即成为调度中心可派发的任务处理器，
 * 无需实现 {@link com.carlos.job.client.handler.JobHandler} 接口。</p>
 *
 * <p>允许的方法签名（其余签名启动即报错）：</p>
 * <pre>{@code
 * void handle()                      // 正常返回即成功
 * void handle(String params)         // params 为任务配置的参数（JSON 字符串，可为 null）
 * JobResult handle()                 // 返回值原样作为执行结果
 * JobResult handle(String params)
 * }</pre>
 *
 * <p>示例：</p>
 * <pre>{@code
 * @Component
 * public class OrderSyncJob {
 *
 *     @ScheduleJob("orderSyncHandler")
 *     @AutoRegisterJob(cron = "0 0 2 * * ?", description = "订单同步")
 *     public void sync(String params) {
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * @author Carlos
 * @date 2026年9月23日
 * @see AutoRegisterJob
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScheduleJob {

    /**
     * 处理器名称（全局唯一，写入 job_info.handler_name）。留空时取方法名。
     *
     * <p>长度上限 100：任务名默认派生自处理器名，而 job_info.job_name 限 100 字符。</p>
     */
    String value() default "";

}
