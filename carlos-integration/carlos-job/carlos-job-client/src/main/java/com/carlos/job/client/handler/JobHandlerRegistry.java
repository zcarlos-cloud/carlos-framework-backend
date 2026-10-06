package com.carlos.job.client.handler;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.exception.ComponentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 任务处理器注册表，启动时收集所有 JobHandler Bean 与 {@code @ScheduleJob} 注解方法
 * </p>
 *
 * <p>两类来源的冲突语义不同：</p>
 * <ul>
 *   <li><b>注解</b>：名称是开发者手写的显式常量，重名即事故 → 直接抛异常终止启动；</li>
 *   <li><b>JobHandler Bean</b>：名称默认取 Bean 名（可能撞车且升级 jar 时无法干预）→ 保持"先注册者优先 + WARN"。</li>
 * </ul>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
public class JobHandlerRegistry implements SmartInitializingSingleton {

    /**
     * Spring 注入的 Bean 名称 -> 处理器（key 为真实 Bean 名称）
     */
    private final Map<String, JobHandler> handlerBeans;

    /**
     * 注解登记的处理器（key 已由注解语义确定，由 BPP 产出）
     */
    private final Map<String, JobHandler> annotatedHandlers;

    /**
     * 处理器名称 -> 处理器（支持自定义 name() 覆盖）
     */
    private final Map<String, JobHandler> handlerMap = new ConcurrentHashMap<>();

    public JobHandlerRegistry(Map<String, JobHandler> handlerBeans) {
        this(handlerBeans, Map.of());
    }

    public JobHandlerRegistry(Map<String, JobHandler> handlerBeans, Map<String, JobHandler> annotatedHandlers) {
        this.handlerBeans = handlerBeans;
        this.annotatedHandlers = annotatedHandlers;
    }

    @Override
    public void afterSingletonsInstantiated() {
        // 注解先登记：其名称是显式声明，接口侧撞上即视为配置事故
        annotatedHandlers.forEach(this::register);
        handlerBeans.forEach((beanName, handler) -> {
            String handlerName = StrUtil.blankToDefault(handler.name(), beanName);
            if (annotatedHandlers.containsKey(handlerName)) {
                throw new ComponentException("任务处理器名称冲突: name=" + handlerName
                    + " 已被 @ScheduleJob 注解占用，JobHandler Bean " + handler.getClass().getName() + " 无法注册");
            }
            JobHandler existing = handlerMap.putIfAbsent(handlerName, handler);
            if (existing != null) {
                log.warn("任务处理器名称冲突: name={}, 已存在 {}, 忽略 {}", handlerName,
                    existing.getClass().getName(), handler.getClass().getName());
            } else {
                log.info("注册任务处理器: name={}, class={}", handlerName, handler.getClass().getName());
            }
        });
    }

    /**
     * 登记一个处理器（严格模式：重名直接抛异常）
     *
     * @param handlerName 处理器名称
     * @param handler     处理器
     */
    public void register(String handlerName, JobHandler handler) {
        JobHandler existing = handlerMap.putIfAbsent(handlerName, handler);
        if (existing != null) {
            throw new ComponentException("任务处理器名称冲突: name=" + handlerName + ", 已存在 "
                + existing.getClass().getName() + ", 冲突项 " + handler.getClass().getName());
        }
        log.debug("登记任务处理器（注解）: name={}, class={}", handlerName, handler.getClass().getName());
    }

    /**
     * 按名称获取处理器
     *
     * @param handlerName 处理器名称
     * @return 处理器，不存在返回 null
     */
    public JobHandler getHandler(String handlerName) {
        if (StrUtil.isBlank(handlerName)) {
            return null;
        }
        return handlerMap.get(handlerName);
    }

    /**
     * 全部已注册处理器名称
     *
     * @return 名称集合
     */
    public Set<String> handlerNames() {
        return handlerMap.keySet();
    }

}
