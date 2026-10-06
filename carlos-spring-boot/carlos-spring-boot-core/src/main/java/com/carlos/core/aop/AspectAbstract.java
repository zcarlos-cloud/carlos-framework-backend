package com.carlos.core.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.Map;

/**
 * <p>
 * 切面通用操作操作接口
 * </p>
 *
 * @author carlos
 * @date 2020/4/17 15:00
 */
public interface AspectAbstract {

    /**
     * 切点配置，使用时可在方法上使用@Pointcut注解
     *
     * @author carlos
     * @date 2020/4/19 1:11
     */
    default void pointcut() {
    }

    /**
     * 环绕通知 方法执行前打印请求参数信息 方法执行后打印响应结果信息
     *
     * @param joinPoint 切点
     * @date 2020-04-17 15:33:41
     */
    default Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        return null;
    }

    /**
     * 方法执行前操作
     *
     * @param joinPoint 切点
     * @date 2020-04-17 15:33:41
     */
    default void doBefore(JoinPoint joinPoint) throws Throwable {
    }

    /**
     * 方法执行后操作
     *
     * @param joinPoint 切点
     * @date 2020-04-17 15:33:41
     */
    default void doAfter(JoinPoint joinPoint) {
    }

    /**
     * 目标方法正常执行后的操作
     *
     * @param joinPoint 切点
     * @param result    方法执行完成的结果信息
     * @date 2020-04-17 15:33:41
     */
    default void doAfterReturning(JoinPoint joinPoint, Object result) {
    }

    /**
     * 抛出异常执行后的操作
     *
     * @param joinPoint 切点
     * @param exception 异常对象 方法运行的异常信息
     * @date 2020-04-17 15:33:41
     */
    default void doAfterThrowing(JoinPoint joinPoint, Throwable exception) {
    }

    /**
     * 执行异常或者正常返回后的结果
     *
     * @param joinPoint 切点信息
     * @param result    正常结果信息
     * @param exception 异常信息
     * @author carlos
     * @date 2020/6/7 22:28
     */
    default void doFinish(JoinPoint joinPoint, Object result, Throwable exception) {
    }

    /**
     * 获取切点的参数信息
     *
     * @author carlos
     * @date 2020/6/4 12:03
     */
    default Map<String, Object> getMethodArgs(JoinPoint joinPoint) {
        return null;
    }
}
