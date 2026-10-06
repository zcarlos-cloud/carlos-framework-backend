package com.carlos.apm.aop;

import brave.Span;
import brave.Tracer;
import com.carlos.apm.annotation.TraceTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * TraceTag 注解切面
 * <p>
 * 处理 {@link TraceTag} 注解，将自定义标签添加到当前追踪 Span 中
 *
 * @author Carlos
 * @date 2024-12-09
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class TraceTagAspect {

    private final Tracer tracer;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(traceTag)")
    public Object around(ProceedingJoinPoint point, TraceTag traceTag) throws Throwable {
        return handleTraceTag(point, new TraceTag[]{traceTag});
    }

    @Around("@annotation(com.carlos.apm.annotation.TraceTags)")
    public Object aroundTags(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        TraceTag[] traceTags = method.getAnnotationsByType(TraceTag.class);
        return handleTraceTag(point, traceTags);
    }

    private Object handleTraceTag(ProceedingJoinPoint point, TraceTag[] traceTags) throws Throwable {
        Object result = point.proceed();

        try {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan == null) {
                return result;
            }

            // 准备 SpEL 上下文
            StandardEvaluationContext context = prepareContext(point, result);

            // 处理每个 TraceTag
            for (TraceTag tag : traceTags) {
                try {
                    // 检查条件
                    if (hasCondition(tag.condition()) && !evaluateCondition(tag.condition(), context)) {
                        continue;
                    }

                    // 解析值
                    String value = evaluateExpression(tag.value(), context);
                    if (value != null) {
                        currentSpan.tag(tag.key(), value);
                    }
                } catch (Exception e) {
                    log.debug("处理 TraceTag 失败 [key={}, value={}]: {}", tag.key(), tag.value(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("TraceTag 切面处理失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 准备 SpEL 评估上下文
     */
    private StandardEvaluationContext prepareContext(ProceedingJoinPoint point, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 添加参数
        Object[] args = point.getArgs();
        MethodSignature signature = (MethodSignature) point.getSignature();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // 添加返回值
        context.setVariable("result", result);

        return context;
    }

    /**
     * 是否有条件表达式
     */
    private boolean hasCondition(String condition) {
        return condition != null && !condition.isEmpty();
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String condition, StandardEvaluationContext context) {
        try {
            return Boolean.TRUE.equals(parser.parseExpression(condition).getValue(context, Boolean.class));
        } catch (Exception e) {
            log.debug("评估条件表达式失败 [{}]: {}", condition, e.getMessage());
            return false;
        }
    }

    /**
     * 评估 SpEL 表达式
     */
    private String evaluateExpression(String expression, StandardEvaluationContext context) {
        try {
            Object value = parser.parseExpression(expression).getValue(context);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.debug("评估 SpEL 表达式失败 [{}]: {}", expression, e.getMessage());
            return null;
        }
    }
}
