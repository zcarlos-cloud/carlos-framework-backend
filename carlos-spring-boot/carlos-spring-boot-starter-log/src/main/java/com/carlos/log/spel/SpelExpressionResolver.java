package com.carlos.log.spel;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpEL 表达式解析器
 * <p>
 * 用于解析日志注解中的 SpEL 表达式
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
public class SpelExpressionResolver {

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    // 缓存解析结果
    private final Map<String, org.springframework.expression.Expression> expressionCache = new ConcurrentHashMap<>();

    /**
     * 创建 SpEL 上下文
     *
     * @param joinPoint 连接点
     * @param result    方法返回值（可能为 null）
     * @return StandardEvaluationContext
     */
    public StandardEvaluationContext createContext(JoinPoint joinPoint, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 添加方法参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = discoverer.getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();

        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // 添加常用变量
        context.setVariable("result", result);
        context.setVariable("request", getCurrentRequest());
        context.setVariable("method", signature.getMethod());
        context.setVariable("target", joinPoint.getTarget());
        context.setVariable("class", joinPoint.getTarget().getClass());

        return context;
    }

    /**
     * 解析 SpEL 表达式
     *
     * @param expression 表达式
     * @param context    上下文
     * @return 解析结果（字符串）
     */
    public String resolve(String expression, StandardEvaluationContext context) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }

        try {
            org.springframework.expression.Expression expr = expressionCache.computeIfAbsent(
                expression,
                parser::parseExpression
            );
            Object value = expr.getValue(context);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("SpEL 表达式解析失败: {}", expression, e);
            return expression; // 返回原表达式，方便排查问题
        }
    }

    /**
     * 解析 SpEL 表达式为指定类型
     *
     * @param expression  表达式
     * @param context     上下文
     * @param desiredType 目标类型
     * @return 解析结果
     */
    public <T> T resolve(String expression, StandardEvaluationContext context, Class<T> desiredType) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }

        try {
            org.springframework.expression.Expression expr = expressionCache.computeIfAbsent(
                expression,
                parser::parseExpression
            );
            return expr.getValue(context, desiredType);
        } catch (Exception e) {
            log.warn("SpEL 表达式解析失败: {}", expression, e);
            return null;
        }
    }

    /**
     * 解析布尔表达式
     *
     * @param expression 表达式
     * @param context    上下文
     * @param defaultValue 默认值
     * @return 布尔结果
     */
    public boolean resolveBoolean(String expression, StandardEvaluationContext context, boolean defaultValue) {
        if (!StringUtils.hasText(expression)) {
            return defaultValue;
        }

        try {
            org.springframework.expression.Expression expr = expressionCache.computeIfAbsent(
                expression,
                parser::parseExpression
            );
            Boolean value = expr.getValue(context, Boolean.class);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            log.warn("SpEL 布尔表达式解析失败: {}", expression, e);
            return defaultValue;
        }
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.debug("获取当前请求失败", e);
        }
        return null;
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        expressionCache.clear();
    }
}
