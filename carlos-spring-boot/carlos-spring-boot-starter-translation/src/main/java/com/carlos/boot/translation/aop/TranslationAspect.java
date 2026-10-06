package com.carlos.boot.translation.aop;

import com.carlos.boot.translation.annotation.Translated;
import com.carlos.boot.translation.core.TranslationContext;
import com.carlos.boot.translation.service.TranslationService;
import com.carlos.core.response.Result;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * <p>
 * 翻译处理切面
 * </p>
 *
 * @author carlos
 * @date 2025/01/20
 */
@Aspect
@Slf4j
@AllArgsConstructor
public class TranslationAspect {

    private final TranslationService translationService;

    /**
     * 拦截 @Translated 注解的方法
     *
     * @param point      切点
     * @param translated 注解
     * @return 结果
     * @throws Throwable 异常
     */
    @Around("@annotation(translated)")
    public Object around(ProceedingJoinPoint point, Translated translated) throws Throwable {
        // 设置翻译上下文
        setupContext(translated);
        try {
            Object result = point.proceed();

            if (result == null) {
                return null;
            }

            long start = System.currentTimeMillis();
            try {
                processResult(result);
            } finally {
                log.debug("Translation completed in {}ms (cacheEnabled={}, cacheMinutes={})", System.currentTimeMillis() - start, translated.cacheEnabled(), translated.cacheMinutes());
            }
            return result;
        } finally {
            // 清除上下文
            TranslationContext.clear();
        }
    }

    /**
     * 拦截返回值类型为 Result 的 Controller 方法（排除已带 @Translated 注解的方法）
     *
     * @param point 切点
     * @return 结果
     * @throws Throwable 异常
     */
    @Around("execution(public com.carlos.core.response.Result com.carlos..controller..*.*(..)) && !@annotation(com.carlos.boot.translation.annotation.Translated)")
    public Object aroundController(ProceedingJoinPoint point) throws Throwable {
        // 检查类是否有 @Translated 注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = point.getTarget().getClass();

        Translated classAnnotation = targetClass.getAnnotation(Translated.class);

        if (classAnnotation == null) {
            return point.proceed();
        }
        Translated translated = classAnnotation;

        // 设置翻译上下文
        setupContext(translated);
        try {
            Object result = point.proceed();

            if (result == null) {
                return null;
            }

            // 解包 ApiResponse 或 Result
            Object data = unwrapResult(result);

            if (data != null) {
                long start = System.currentTimeMillis();
                try {
                    processResult(data);
                } finally {
                    log.debug("Translation completed in {}ms (cacheEnabled={}, cacheMinutes={})", System.currentTimeMillis() - start, translated.cacheEnabled(), translated.cacheMinutes());
                }
            }

            return result;
        } finally {
            // 清除上下文
            TranslationContext.clear();
        }
    }

    /**
     * 设置翻译上下文
     *
     * @param translated 注解
     */
    private void setupContext(Translated translated) {
        TranslationContext context = new TranslationContext();
        context.setCacheEnabled(translated.cacheEnabled());
        context.setCacheMinutes(translated.cacheMinutes());
        TranslationContext.set(context);

        log.debug("Translation context set: cacheEnabled={}, cacheMinutes={}",
            translated.cacheEnabled(), translated.cacheMinutes());
    }

    /**
     * 解包响应结果
     *
     * @param result 原始结果
     * @return 数据
     */
    private Object unwrapResult(Object result) {
        // 解包 Result
        if (result instanceof Result) {
            return ((Result<?>) result).getData();
        }
        return result;
    }

    /**
     * 处理结果
     *
     * @param result 结果
     */
    private void processResult(Object result) {
        if (result instanceof Collection) {
            translationService.translateCollection((Collection<?>) result);
        } else {
            translationService.translate(result);
        }
    }
}
