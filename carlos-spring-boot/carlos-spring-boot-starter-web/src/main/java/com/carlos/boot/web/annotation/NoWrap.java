package com.carlos.boot.web.annotation;

import java.lang.annotation.*;

/**
 * <p>
 * 标记控制器类或方法，跳过统一响应包装。
 * </p>
 * <p>
 * 当 Controller 类或方法上标注此注解时，其返回值将不会被
 * {@code ResponseBodyAdvice} 统一包装为 {@code Result<T>}。
 * 适用于需要直接返回原始数据的场景（如文件下载、回调接口等）。
 * </p>
 *
 * <pre>
 * // 类级别：该 Controller 所有方法均跳过包装
 * &#64;NoWrap
 * &#64;RestController
 * public class CallbackController { ... }
 *
 * // 方法级别：仅该方法跳过包装
 * &#64;NoWrap
 * &#64;GetMapping("/download")
 * public ResponseEntity&lt;Resource&gt; download() { ... }
 * </pre>
 *
 * @author carlos
 * @since 3.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoWrap {
}
