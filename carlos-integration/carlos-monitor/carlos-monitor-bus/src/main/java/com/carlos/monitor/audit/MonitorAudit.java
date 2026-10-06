package com.carlos.monitor.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * 监控操作审计注解（写操作埋点，异步上报 carlos-audit）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MonitorAudit {

    /**
     * 操作类型（审计 logType，如 REDIS_DATA_DELETE）
     */
    String action();

    /**
     * 目标对象 SpEL（基于方法参数，如 "#instanceId + ':' + #key"）
     */
    String target() default "";

    /**
     * 是否抓取前值快照（key 级写操作；要求方法签名含 instanceId 与 key 参数）
     */
    boolean snapshot() default false;

    /**
     * 风险等级（0-100，默认 60=高危数据操作）
     */
    int riskLevel() default 60;
}
