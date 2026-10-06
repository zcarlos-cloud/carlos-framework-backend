package com.carlos.datascope;

import cn.hutool.core.util.ArrayUtil;
import com.carlos.core.aop.AspectAbstract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * <p>
 * 数据权限注解切面
 * </p>
 *
 * @author Carlos
 * @date 2022/11/21 13:25
 */
@Aspect
@RequiredArgsConstructor
@Slf4j
public class DataScopeAspect implements AspectAbstract {

    private final DataScopeHandler handler;

    /**
     * 监听数据数据权限注解
     */
    @Override
    @Pointcut("@annotation(DataScope) || @annotation(DataScopes)")
    public void pointcut() {
    }


    @Before(value = "pointcut()")
    @Override
    public void doBefore(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DataScopes annotation = method.getAnnotation(DataScopes.class);

        DataScope[] scopes;
        if (annotation != null) {
            scopes = annotation.value();
        } else {
            scopes = method.getAnnotationsByType(DataScope.class);
        }
        if (ArrayUtil.isEmpty(scopes)) {
            log.warn("Method doesn't config any annotation of DataScope");
            return;
        }
        handler.handle(scopes);
        handler.handleParam(joinPoint);
    }

    @After(value = "pointcut()")
    @Override
    public void doAfter(JoinPoint joinPoint) {
        handler.exit();
    }


    @Override
    public void doAfterReturning(JoinPoint joinPoint, Object result) {

    }

    @Override
    public void doAfterThrowing(JoinPoint joinPoint, Throwable exception) {

    }

    @Override
    public void doFinish(JoinPoint joinPoint, Object result, Throwable exception) {

    }

}
