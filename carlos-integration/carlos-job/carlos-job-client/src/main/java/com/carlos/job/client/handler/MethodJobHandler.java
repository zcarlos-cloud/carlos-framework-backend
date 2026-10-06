package com.carlos.job.client.handler;

import com.carlos.core.exception.ComponentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * <p>
 * 方法级任务处理器：把 {@code @ScheduleJob} 标注的方法适配成 {@link JobHandler}
 * </p>
 *
 * <p>只持有 {@code beanName}，执行时才向容器取 Bean —— {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * 与 AOP 自动代理创建器的先后顺序不确定，此处缓存下来的引用可能是未被代理的原始对象，
 * 会导致方法上的 {@code @Transactional}/{@code @PreAuthorize} 静默失效。</p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Slf4j
public class MethodJobHandler implements JobHandler {

    private final BeanFactory beanFactory;

    /**
     * 目标 Bean 名称
     */
    private final String beanName;

    /**
     * 处理器名称（注解声明值，留空时取方法名）
     */
    private final String handlerName;

    /**
     * 被标注的方法（声明类上的方法，非代理类）
     */
    private final Method method;

    /**
     * 方法是否接收 params 参数
     */
    private final boolean withParams;

    public MethodJobHandler(BeanFactory beanFactory, String beanName, String handlerName,
                            Method method, boolean withParams) {
        this.beanFactory = beanFactory;
        this.beanName = beanName;
        this.handlerName = handlerName;
        this.method = method;
        this.withParams = withParams;
    }

    @Override
    public String name() {
        return handlerName;
    }

    @Override
    public JobResult execute(String params) throws Exception {
        Object bean = beanFactory.getBean(beanName);
        // 目标可能被代理，取目标类上可调用的同名方法，避免 JDK 动态代理场景下方法不可达
        Method invocable = AopUtils.selectInvocableMethod(method, bean.getClass());
        // 目标类可能是包级私有（或声明在非 public 内部类上），显式放开访问权限，
        // 与 Spring 处理 @Scheduled 的做法一致
        ReflectionUtils.makeAccessible(invocable);
        Object result;
        try {
            result = withParams ? invocable.invoke(bean, params) : invocable.invoke(bean);
        } catch (InvocationTargetException e) {
            // 业务异常原样抛出，由上层统一记录为执行失败（不要包装成 InvocationTargetException 丢失原始信息）
            Throwable target = e.getTargetException();
            if (target instanceof Exception exception) {
                throw exception;
            }
            if (target instanceof Error error) {
                throw error;
            }
            throw new ComponentException("任务处理器抛出无法归类的 Throwable: " + describe(), target);
        }
        // void 方法正常返回即视为成功；返回 JobResult 时原样透传
        if (result instanceof JobResult jobResult) {
            return jobResult;
        }
        // 声明了 JobResult 却返回 null：记为失败，不能静默成功（否则执行日志与失败告警链都不会触发）
        if (JobResult.class.equals(invocable.getReturnType())) {
            return JobResult.fail("任务处理器返回 null: " + describe());
        }
        return JobResult.ok();
    }

    /**
     * 目标方法全名，用于日志与异常信息
     *
     * @return {@code 类全名#方法名}
     */
    public String describe() {
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }

}
