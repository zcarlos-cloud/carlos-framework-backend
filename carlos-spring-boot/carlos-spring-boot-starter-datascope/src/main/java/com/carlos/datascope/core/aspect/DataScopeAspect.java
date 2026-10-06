package com.carlos.datascope.core.aspect;

import com.carlos.datascope.annotation.DataScope;
import com.carlos.datascope.annotation.DataScopes;
import com.carlos.datascope.audit.DataScopeAuditLogger;
import com.carlos.datascope.core.context.DataScopeContext;
import com.carlos.datascope.core.context.DataScopeContextHolder;
import com.carlos.datascope.core.engine.DataScopeRuleEngine;
import com.carlos.datascope.core.model.DataScopeResult;
import com.carlos.datascope.exception.DataScopeDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 数据权限AOP切面
 * <p>
 * 拦截标记了@DataScope的方法，执行数据权限控制逻辑
 *
 * @author Carlos
 * @version 2.0
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class DataScopeAspect {

    private final DataScopeRuleEngine ruleEngine;
    private final DataScopeAuditLogger auditLogger;

    /**
     * 切入点：标记了@DataScope或@DataScopes的方法
     */
    @Pointcut("@annotation(com.carlos.datascope.annotation.DataScope) || " +
        "@annotation(com.carlos.datascope.annotation.DataScopes)")
    public void dataScopePointcut() {
    }

    /**
     * 环绕通知
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     * @throws Throwable 异常
     */
    @Around("dataScopePointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取注解
        DataScopes dataScopes = method.getAnnotation(DataScopes.class);
        DataScope[] scopes;
        if (dataScopes != null) {
            scopes = dataScopes.value();
        } else {
            scopes = method.getAnnotationsByType(DataScope.class);
        }

        if (scopes.length == 0) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        DataScopeContext context = null;

        try {
            // 1. 执行规则引擎
            DataScopeResult result = ruleEngine.process(method, scopes, joinPoint.getArgs());

            // 2. 检查权限
            if (!result.isAllowed()) {
                long duration = System.currentTimeMillis() - startTime;
                log.warn("Data scope denied: method={}, reason={}", method.getName(), result.getDenyReason());

                // 记录审计日志
                if (auditLogger != null) {
                    auditLogger.logDenied(method, result, duration);
                }

                throw new DataScopeDeniedException(result.getDenyReason());
            }

            // 3. 创建上下文
            context = DataScopeContext.builder()
                .result(result)
                .method(method)
                .args(joinPoint.getArgs())
                .build();
            context.log("Data scope check passed, rules: " + result.getMatchedRuleIds());

            // 4. 设置到ThreadLocal
            DataScopeContextHolder.set(context);

            // 5. 执行目标方法
            Object resultObj = joinPoint.proceed();

            // 6. 处理脱敏
            if (result.needsMasking()) {
                resultObj = applyMasking(resultObj, result);
                context.setMaskingApplied(true);
            }

            // 7. 记录审计日志
            long duration = System.currentTimeMillis() - startTime;
            if (auditLogger != null) {
                auditLogger.logSuccess(method, result, duration);
            }

            context.complete();
            return resultObj;

        } finally {
            // 清理上下文
            DataScopeContextHolder.clear();
        }
    }

    /**
     * 应用数据脱敏
     *
     * @param result 原始结果
     * @param scopeResult 数据权限结果
     * @return 脱敏后的结果
     */
    private Object applyMasking(Object result, DataScopeResult scopeResult) {
        if (result == null) {
            return null;
        }

        // 简化实现：实际项目中应该使用更复杂的脱敏处理器
        // 这里仅作为示例
        log.debug("Applying masking to result: columns={}", scopeResult.getMaskingColumns());

        // TODO: 实现具体的脱敏逻辑
        // 可以使用反射遍历对象属性，对指定字段进行脱敏处理

        return result;
    }
}
