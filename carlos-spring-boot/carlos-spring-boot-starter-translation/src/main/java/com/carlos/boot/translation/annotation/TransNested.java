package com.carlos.boot.translation.annotation;

import java.lang.annotation.*;

/**
 * <p>
 * 批量翻译嵌套对象
 * </p>
 *
 * <p>递归处理嵌套对象的翻译注解</p>
 *
 * @author carlos
 * @date 2025/01/20
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TransNested {

    /**
     * 是否递归翻译嵌套对象
     */
    boolean recursive() default true;

    /**
     * 最大递归深度
     */
    int maxDepth() default 3;
}
