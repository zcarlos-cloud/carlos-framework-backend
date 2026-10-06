package com.carlos.apm.annotation;

import java.lang.annotation.*;

/**
 * 追踪标签集合注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TraceTags {
    TraceTag[] value();
}
